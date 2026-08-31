package com.kobe.warehouse.service.impl;

import com.kobe.warehouse.domain.GammeProduit;
import com.kobe.warehouse.repository.GammeProduitRepository;
import com.kobe.warehouse.repository.ProduitRepository;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.repository.util.Condition;
import com.kobe.warehouse.repository.util.SpecificationBuilder;
import com.kobe.warehouse.service.dto.GammeProduitDTO;
import com.kobe.warehouse.service.dto.ResponseDTO;
import com.kobe.warehouse.service.referential.GammeProduitService;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Service Implementation for managing {@link com.kobe.warehouse.domain.GammeProduit}.
 */
@Service
@Transactional
public class GammeProduitServiceImpl implements GammeProduitService {

    private final Logger log = LoggerFactory.getLogger(GammeProduitServiceImpl.class);

    private final GammeProduitRepository gammeProduitRepository;
    private final ProduitRepository produitRepository;

    public GammeProduitServiceImpl(GammeProduitRepository gammeProduitRepository, ProduitRepository produitRepository) {
        this.produitRepository = produitRepository;
        this.gammeProduitRepository = gammeProduitRepository;
    }

    /**
     * Save a gammeProduit.
     *
     * @param gammeProduitDTO the entity to save.
     * @return the persisted entity.
     */
    @Override
    public GammeProduitDTO save(GammeProduitDTO gammeProduitDTO) {
        log.debug("Request to save GammeProduit : {}", gammeProduitDTO);
        GammeProduit gammeProduit = new GammeProduit()
            .code(gammeProduitDTO.getCode())
            .libelle(gammeProduitDTO.getLibelle())
            .id(gammeProduitDTO.getId());
        gammeProduit = gammeProduitRepository.save(gammeProduit);
        return new GammeProduitDTO(gammeProduit);
    }

    /**
     * Get all the gammeProduits.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<GammeProduitDTO> findAll(String search, Pageable pageable) {
        log.debug("Request to get all GammeProduits");
        Pageable page = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "libelle"));

        if (!StringUtils.isEmpty(search)) {
            SpecificationBuilder<GammeProduit> builder = new SpecificationBuilder<>();
            Specification<GammeProduit> spec = builder
                .with(new String[] { "libelle" }, search + "%", Condition.OperationType.LIKE, Condition.LogicalOperatorType.OR)
                .with(new String[] { "code" }, search + "%", Condition.OperationType.LIKE, Condition.LogicalOperatorType.END)
                .build();
            return gammeProduitRepository.findAll(spec, page).map(GammeProduitDTO::new);
        }
        return gammeProduitRepository.findAll(page).map(GammeProduitDTO::new);
    }

    /**
     * Get one gammeProduit by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<GammeProduitDTO> findOne(Integer id) {
        log.debug("Request to get GammeProduit : {}", id);
        return gammeProduitRepository.findById(id).map(GammeProduitDTO::new);
    }

    /**
     * Delete the gammeProduit by id.
     *
     * @param id the id of the entity.
     */
    @Override
    public void delete(Integer id) {
        // Un référentiel encore porté par des fiches ne peut pas disparaître : la
        // requête échouerait sur la contrainte de clé étrangère, en HTTP 500 et sans
        // rien dire d'utile. On compte d'abord, et l'on explique.
        long rattaches = this.produitRepository.countByGammeId(id);
        if (rattaches > 0) {
            throw new GenericError(
                "La gamme est encore utilisée par %d produit(s) : suppression impossible.".formatted(rattaches),
                "referentielUtilise"
            );
        }
        log.debug("Request to delete GammeProduit : {}", id);
        gammeProduitRepository.deleteById(id);
    }

    @Override
    public ResponseDTO importation(InputStream inputStream) {
        AtomicInteger count = new AtomicInteger();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.withDelimiter(';').withFirstRecordAsHeader().parse(br);
            records.forEach(record -> {
                GammeProduit gammeProduit = new GammeProduit();
                gammeProduit.setLibelle(record.get(0));
                gammeProduitRepository.save(gammeProduit);
                count.incrementAndGet();
            });
        } catch (IOException e) {
            log.debug("importation : {}", e);
        }

        return new ResponseDTO().size(count.get());
    }
}
