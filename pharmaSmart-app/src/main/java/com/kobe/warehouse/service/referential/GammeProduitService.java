package com.kobe.warehouse.service.referential;

import com.kobe.warehouse.service.dto.GammeProduitDTO;
import com.kobe.warehouse.service.dto.ResponseDTO;
import java.io.InputStream;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service Interface for managing {@link com.kobe.warehouse.domain.GammeProduit}.
 */
public interface GammeProduitService {
    /**
     * Save a gammeProduit.
     *
     * @param gammeProduitDTO the entity to save.
     * @return the persisted entity.
     */
    GammeProduitDTO save(GammeProduitDTO gammeProduitDTO);

    /**
     * Get all the gammeProduits.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Page<GammeProduitDTO> findAll(String search, Pageable pageable);

    /**
     * Get the "id" gammeProduit.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<GammeProduitDTO> findOne(Integer id);

    /**
     * Delete the "id" gammeProduit.
     *
     * @param id the id of the entity.
     */
    void delete(Integer id);

    ResponseDTO importation(InputStream inputStream);
}
