package com.kobe.warehouse.service;

import com.kobe.warehouse.service.dto.TvaDTO;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TvaService {
    /**
     * Save a tva.
     *
     * @param tvaDTO the entity to save.
     * @return the persisted entity.
     */
    TvaDTO save(TvaDTO tvaDTO);

    /**
     * Get all the tvas.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Page<TvaDTO> findAll(Pageable pageable);

    /**
     * Get the "id" tva.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<TvaDTO> findOne(Long id);

    /**
     * Delete the "id" tva.
     *
     * @param id the id of the entity.
     */
    void delete(Long id);
}
