package com.kobe.warehouse.service;


import com.kobe.warehouse.service.dto.RayonDTO;
import com.kobe.warehouse.service.dto.ResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;

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
	Page<RayonDTO> findAll(Long magasinId, String query, Pageable pageable);

	/**
	 * Get the "id" rayon.
	 *
	 * @param id the id of the entity.
	 * @return the entity.
	 */
	Optional<RayonDTO> findOne(Long id);

	/**
	 * Delete the "id" rayon.
	 *
	 * @param id the id of the entity.
	 */
	void delete(Long id);

	ResponseDTO importation(InputStream inputStream, Long magasinId);

	ResponseDTO cloner(List<RayonDTO> rayonIds, Long magasinId);
}
