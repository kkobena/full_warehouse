package com.kobe.warehouse.service;

import com.kobe.warehouse.domain.Fournisseur;
import com.kobe.warehouse.service.dto.FournisseurDTO;
import com.kobe.warehouse.service.dto.ResponseDTO;
import java.io.InputStream;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service Interface for managing {@link com.kobe.warehouse.domain.Fournisseur}.
 */
public interface FournisseurService {
    /**
     * Save a fournisseur.
     *
     * @param fournisseurDTO the entity to save.
     * @return the persisted entity.
     */
    FournisseurDTO save(FournisseurDTO fournisseurDTO);

    /**
     * Get all the fournisseurs.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Page<FournisseurDTO> findAll(String search, Pageable pageable);

    /**
     * Get the "id" fournisseur.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<FournisseurDTO> findOne(Integer id);

    /**
     * Delete the "id" fournisseur.
     *
     * @param id the id of the entity.
     */
    void delete(Integer id);

    ResponseDTO importation(InputStream inputStream);

    Fournisseur findOneById(Integer id);
}
