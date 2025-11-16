package com.kobe.warehouse.service;

import com.kobe.warehouse.domain.Storage;
import com.kobe.warehouse.service.dto.RayonDTO;
import com.kobe.warehouse.service.dto.ResponseDTO;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service Interface for managing {@link com.kobe.warehouse.domain.Rayon}.
 */
public interface RayonService {
    /**
     * Save a rayon.
     *
     * @param rayonDTO the entity to save.
     * @return the persisted entity.
     */
    RayonDTO save(RayonDTO rayonDTO);

    RayonDTO update(RayonDTO rayonDTO);

    /**
     * Get all the rayons.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Page<RayonDTO> findAll(Integer magasinId, Integer storageId, String query, Pageable pageable);

    /**
     * Get the "id" rayon.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<RayonDTO> findOne(Integer id);

    /**
     * Delete the "id" rayon.
     *
     * @param id the id of the entity.
     */
    void delete(Integer id);

    ResponseDTO importation(InputStream inputStream, Integer magasinId);

    ResponseDTO cloner(List<RayonDTO> rayons, Integer magasinId);

    void initDefaultRayon(Set<Storage> storages);

    void deleteByStorage(Storage storage);
}
