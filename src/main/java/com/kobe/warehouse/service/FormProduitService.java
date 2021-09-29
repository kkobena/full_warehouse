package com.kobe.warehouse.service;


import com.kobe.warehouse.service.dto.FormProduitDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;


public interface FormProduitService {

    /**
     * Save a formProduit.
     *
     * @param formProduitDTO the entity to save.
     * @return the persisted entity.
     */
    FormProduitDTO save(FormProduitDTO formProduitDTO);

    /**
     * Get all the formProduits.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Page<FormProduitDTO> findAll(Pageable pageable);


    /**
     * Get the "id" formProduit.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<FormProduitDTO> findOne(Long id);

    /**
     * Delete the "id" formProduit.
     *
     * @param id the id of the entity.
     */
    void delete(Long id);
}
