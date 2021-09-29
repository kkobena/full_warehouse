package com.kobe.warehouse.service.impl;


import com.kobe.warehouse.domain.GroupeFournisseur;
import com.kobe.warehouse.repository.GroupeFournisseurRepository;
import com.kobe.warehouse.repository.util.Condition;
import com.kobe.warehouse.repository.util.SpecificationBuilder;
import com.kobe.warehouse.service.GroupeFournisseurService;
import com.kobe.warehouse.service.dto.GroupeFournisseurDTO;
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
 * Service Implementation for managing {@link GroupeFournisseur}.
 */
@Service
@Transactional
public class GroupeFournisseurServiceImpl implements GroupeFournisseurService {

    private final Logger log = LoggerFactory.getLogger(GroupeFournisseurServiceImpl.class);
    private final GroupeFournisseurRepository groupeFournisseurRepository;

    public GroupeFournisseurServiceImpl(GroupeFournisseurRepository groupeFournisseurRepository
    ) {
        this.groupeFournisseurRepository = groupeFournisseurRepository;

    }

    /**
     * Save a groupeFournisseur.
     *
     * @param groupeFournisseurDTO the entity to save.
     * @return the persisted entity.
     */
    @Override
    public GroupeFournisseurDTO save(GroupeFournisseurDTO groupeFournisseurDTO) {
        log.debug("Request to save GroupeFournisseur : {}", groupeFournisseurDTO);
        GroupeFournisseur groupeFournisseur = new GroupeFournisseur()
            .id(groupeFournisseurDTO.getId())
            .libelle(groupeFournisseurDTO.getLibelle())
            .addresspostale(groupeFournisseurDTO.getAddresspostale())
            .email(groupeFournisseurDTO.getEmail())
            .numFaxe(groupeFournisseurDTO.getNumFaxe())
            .odre(groupeFournisseurDTO.getOdre())
            .tel(groupeFournisseurDTO.getTel());
        groupeFournisseur = groupeFournisseurRepository.save(groupeFournisseur);
        return new GroupeFournisseurDTO(groupeFournisseur);
    }

    /**
     * Get all the groupeFournisseurs.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<GroupeFournisseurDTO> findAll(String search, Pageable pageable) {
        log.debug("Request to get all GroupeFournisseurs");
        Pageable page = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
            Sort.by(Sort.Direction.ASC, "libelle"));
        if (!StringUtils.isEmpty(search)) {
            SpecificationBuilder<GroupeFournisseur> builder = new SpecificationBuilder<>();
            Specification<GroupeFournisseur> spec = builder
                .with(new String[]{"libelle"}, search + "%", Condition.OperationType.LIKE, Condition.LogicalOperatorType.END)
                .build();
            return groupeFournisseurRepository.findAll(spec, page).map(GroupeFournisseurDTO::new);
        }

        return groupeFournisseurRepository.findAll(page)
            .map(GroupeFournisseurDTO::new);
    }


    /**
     * Get one groupeFournisseur by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<GroupeFournisseurDTO> findOne(Long id) {
        log.debug("Request to get GroupeFournisseur : {}", id);
        return groupeFournisseurRepository.findById(id)
            .map(GroupeFournisseurDTO::new);
    }

    /**
     * Delete the groupeFournisseur by id.
     *
     * @param id the id of the entity.
     */
    @Override
    public void delete(Long id) {
        log.debug("Request to delete GroupeFournisseur : {}", id);

        groupeFournisseurRepository.deleteById(id);
    }

    @Override
    public ResponseDTO importation(InputStream inputStream) {
        AtomicInteger count = new AtomicInteger();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.withDelimiter(';')
                .withFirstRecordAsHeader()
                .parse(br);

            records.forEach(record -> {
                GroupeFournisseur groupeFournisseur = new GroupeFournisseur();
                groupeFournisseur.setLibelle(record.get(0));
                groupeFournisseur.setOdre(count.incrementAndGet());


                groupeFournisseurRepository.save(groupeFournisseur);
            });
        } catch (IOException e) {
            log.debug("importation : {}", e);
        }

        return new ResponseDTO().size(count.get());
    }

}
