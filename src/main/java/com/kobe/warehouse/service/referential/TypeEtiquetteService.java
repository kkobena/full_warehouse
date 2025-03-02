package com.kobe.warehouse.service.referential;

import com.kobe.warehouse.service.dto.TypeEtiquetteDTO;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TypeEtiquetteService {
    /**
     * Save a typeEtiquette.
     *
     * @param typeEtiquetteDTO the entity to save.
     * @return the persisted entity.
     */
    TypeEtiquetteDTO save(TypeEtiquetteDTO typeEtiquetteDTO);

    /**
     * Get all the typeEtiquettes.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Page<TypeEtiquetteDTO> findAll(Pageable pageable);

    /**
     * Get the "id" typeEtiquette.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<TypeEtiquetteDTO> findOne(Long id);

    /**
     * Delete the "id" typeEtiquette.
     *
     * @param id the id of the entity.
     */
    void delete(Long id);
}
