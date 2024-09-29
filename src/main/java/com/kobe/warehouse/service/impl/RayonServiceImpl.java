package com.kobe.warehouse.service.impl;

import com.kobe.warehouse.domain.Rayon;
import com.kobe.warehouse.domain.Storage;
import com.kobe.warehouse.repository.CustomizedRayonService;
import com.kobe.warehouse.repository.RayonRepository;
import com.kobe.warehouse.service.RayonService;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.RayonDTO;
import com.kobe.warehouse.service.dto.ResponseDTO;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link Rayon}.
 */
@Service
@Transactional
public class RayonServiceImpl implements RayonService {

    private final Logger log = LoggerFactory.getLogger(RayonServiceImpl.class);
    private final RayonRepository rayonRepository;
    private final StorageService storageService;
    private final CustomizedRayonService customizedRayonService;

    public RayonServiceImpl(RayonRepository rayonRepository, StorageService storageService, CustomizedRayonService customizedRayonService) {
        this.rayonRepository = rayonRepository;
        this.storageService = storageService;
        this.customizedRayonService = customizedRayonService;
    }

    /**
     * Save a rayon.
     *
     * @param rayonDTO the entity to save.
     * @return the persisted entity.
     */
    @Override
    public RayonDTO save(RayonDTO rayonDTO) {
        log.debug("Request to save Rayon : {}", rayonDTO);

        return customizedRayonService.save(rayonDTO);
    }

    @Override
    public RayonDTO update(RayonDTO rayonDTO) {
        log.debug("Request to save Rayon : {}", rayonDTO);

        return customizedRayonService.update(rayonDTO);
    }

    /**
     * Get all the rayons.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<RayonDTO> findAll(Long storageId, String query, Pageable pageable) {
        log.debug("Request to get all Rayons");
        if (storageId == null || storageId == 0) {
            storageId = storageService.getDefaultConnectedUserMainStorage().getId();
        }

        return customizedRayonService.listRayonsByStorageId(storageId, query, pageable);
    }

    /**
     * Get one rayon by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<RayonDTO> findOne(Long id) {
        log.debug("Request to get Rayon : {}", id);
        return rayonRepository.findById(id).map(RayonDTO::new);
    }

    /**
     * Delete the rayon by id.
     *
     * @param id the id of the entity.
     */
    @Override
    public void delete(Long id) {
        log.debug("Request to delete Rayon : {}", id);
        rayonRepository.deleteById(id);
    }

    @Override
    public ResponseDTO importation(InputStream inputStream, Long storageId) {
        final Long storageId2 = (storageId == null || storageId == 0)
            ? storageService.getDefaultConnectedUserMainStorage().getId()
            : storageId;
        AtomicInteger count = new AtomicInteger(0);
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.builder().setDelimiter(';').build().parse(br);
            records.forEach(record -> {
                var index = count.get();
                if (index == 0) {
                    count.incrementAndGet();
                    return;
                }
                Rayon rayon = new Rayon();
                rayon.setLibelle(record.get(0).trim());
                rayon.setCode(record.get(1).trim());
                try {
                    boolean exclude = Integer.parseInt(record.get(2)) == 0;
                    rayon.setExclude(exclude);
                } catch (Exception e) {
                    log.error("importation", e);
                }
                rayon.setStorage(customizedRayonService.fromId(storageId2));
                rayonRepository.save(rayon);
                count.incrementAndGet();
            });
        } catch (IOException e) {
            log.error("importation : {0}", e);
        }

        return new ResponseDTO().size(count.get());
    }

    @Override
    public ResponseDTO cloner(List<RayonDTO> rayonIds, Long storageId) {
        int count = 0;

        Storage storage = storageService.getOne(storageId);
        for (RayonDTO rayonDTO : rayonIds) {
            Rayon rayon = customizedRayonService.buildRayonFromRayonDTO(rayonDTO);
            rayon.setStorage(storage);
            rayonRepository.save(rayon);
            count++;
        }
        return new ResponseDTO().size(count);
    }
}
