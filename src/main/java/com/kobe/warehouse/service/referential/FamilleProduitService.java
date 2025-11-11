package com.kobe.warehouse.service.referential;

import com.kobe.warehouse.service.dto.FamilleProduitDTO;
import com.kobe.warehouse.service.dto.ResponseDTO;
import java.io.InputStream;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service Interface for managing {@link com.kobe.warehouse.domain.FamilleProduit}.
 */
public interface FamilleProduitService {
    /**
     * Save a familleProduit.
     *
     * @param familleProduitDTO the entity to save.
     * @return the persisted entity.
     */
    FamilleProduitDTO save(FamilleProduitDTO familleProduitDTO);

    /**
     * Get all the familleProduits.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Page<FamilleProduitDTO> findAll(String search, Pageable pageable);

    /**
     * Get the "id" familleProduit.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<FamilleProduitDTO> findOne(Integer id);

    /**
     * Delete the "id" familleProduit.
     *
     * @param id the id of the entity.
     */
    void delete(Integer id);

    ResponseDTO importation(InputStream inputStream);
}
