package com.kobe.warehouse.service;


import com.kobe.warehouse.service.dto.LaboratoireDTO;
import com.kobe.warehouse.service.dto.ResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.InputStream;
import java.util.Optional;

/**
 * Service Interface for managing {@link com.kobe.warehouse.domain.Laboratoire}.
 */
public interface LaboratoireService {

    /**
     * Save a laboratoire.
     *
     * @param laboratoireDTO the entity to save.
     * @return the persisted entity.
     */
    LaboratoireDTO save(LaboratoireDTO laboratoireDTO);

    /**
     * Get all the laboratoires.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Page<LaboratoireDTO> findAll(String libelle, Pageable pageable);


    /**
     * Get the "id" laboratoire.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<LaboratoireDTO> findOne(Long id);

    /**
     * Delete the "id" laboratoire.
     *
     * @param id the id of the entity.
     */
    void delete(Long id);

    ResponseDTO importation(InputStream inputStream);
}
