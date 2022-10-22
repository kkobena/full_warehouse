package com.kobe.warehouse.service.impl;

import com.kobe.warehouse.domain.Laboratoire;
import com.kobe.warehouse.repository.LaboratoireRepository;
import com.kobe.warehouse.repository.util.Condition;
import com.kobe.warehouse.repository.util.SpecificationBuilder;
import com.kobe.warehouse.service.LaboratoireService;
import com.kobe.warehouse.service.dto.LaboratoireDTO;
import com.kobe.warehouse.service.dto.ResponseDTO;
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service Implementation for managing {@link Laboratoire}.
 */
@Service
@Transactional
public class LaboratoireServiceImpl implements LaboratoireService {

    private final Logger log = LoggerFactory.getLogger(LaboratoireServiceImpl.class);

    private final LaboratoireRepository laboratoireRepository;


    public LaboratoireServiceImpl(LaboratoireRepository laboratoireRepository) {
        this.laboratoireRepository = laboratoireRepository;

    }

    /**
     * Save a laboratoire.
     *
     * @param laboratoireDTO the entity to save.
     * @return the persisted entity.
     */
    @Override
    public LaboratoireDTO save(LaboratoireDTO laboratoireDTO) {
        log.debug("Request to save Laboratoire : {}", laboratoireDTO);
        Laboratoire laboratoire = new Laboratoire().id(laboratoireDTO.getId())
            .libelle(laboratoireDTO.getLibelle());
        laboratoire = laboratoireRepository.save(laboratoire);
        return new LaboratoireDTO(laboratoire);
    }

    /**
     * Get all the laboratoires.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<LaboratoireDTO> findAll(String libelle, Pageable pageable) {
        log.debug("Request to get all Laboratoires");
        Pageable page = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
            Sort.by(Sort.Direction.ASC, "libelle"));
        if (!StringUtils.isEmpty(libelle)) {
            SpecificationBuilder<Laboratoire> builder = new SpecificationBuilder<>();
            Specification<Laboratoire> spec = builder.with(new String[]{"libelle"}, libelle + "%",
                Condition.OperationType.LIKE, Condition.LogicalOperatorType.END).build();
            return laboratoireRepository.findAll(spec, page).map(LaboratoireDTO::new);
        }
        return laboratoireRepository.findAll(page).map(LaboratoireDTO::new);
    }

    /**
     * Get one laboratoire by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<LaboratoireDTO> findOne(Long id) {
        log.debug("Request to get Laboratoire : {}", id);
        return laboratoireRepository.findById(id).map(LaboratoireDTO::new);
    }

    /**
     * Delete the laboratoire by id.
     *
     * @param id the id of the entity.
     */
    @Override
    public void delete(Long id) {
        log.debug("Request to delete Laboratoire : {}", id);

        laboratoireRepository.deleteById(id);
    }

    @Override
    public ResponseDTO importation(InputStream inputStream) {
        AtomicInteger count = new AtomicInteger();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.withDelimiter(';').withFirstRecordAsHeader().parse(br);
            records.forEach(record -> {
                Laboratoire laboratoire = new Laboratoire();
                laboratoire.setLibelle(record.get(0));
                laboratoireRepository.save(laboratoire);
                count.incrementAndGet();
            });
        } catch (IOException e) {
            log.debug("importation : {}", e);
        }
        return new ResponseDTO().size(count.get());
    }
}
