package com.kobe.warehouse.repository;


import com.kobe.warehouse.domain.Rayon;
import com.kobe.warehouse.domain.Storage;
import com.kobe.warehouse.service.dto.RayonDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;

public interface CustomizedRayonService {
	Page<RayonDTO> listRayonsByStorageId(Long magasinId, String query, Pageable pageable);

	RayonDTO save(RayonDTO dto);

	RayonDTO update(RayonDTO dto);

	default RayonDTO buildRayonDTOFromRayon(Rayon rayon) {
		RayonDTO dto = new RayonDTO();
		dto.setCode(rayon.getCode());
		dto.setId(rayon.getId());
        Storage storage = rayon.getStorage();
		dto.setStorageId(storage.getId());
		dto.setStorageLibelle(storage.getName());
		dto.setLibelle(rayon.getLibelle());
		dto.setExclude(rayon.isExclude());
		return dto;

	}

	default Rayon buildRayonFromRayonDTO(RayonDTO dto, Rayon rayon) {
		rayon.setCode(dto.getCode());
		rayon.setLibelle(dto.getLibelle());
		rayon.setUpdatedAt(Instant.now());
		return rayon;

	}

	default Rayon buildRayonFromRayonDTO(RayonDTO dto) {
		Rayon rayon = new Rayon();
		rayon.setCode(dto.getCode());
		rayon.setLibelle(dto.getLibelle());
		rayon.setUpdatedAt(Instant.now());
		rayon.setCreatedAt(Instant.now());
		rayon.setStorage(fromId(dto.getStorageId()));
		return rayon;

	}

	default Storage fromId(Long id) {
        Storage storage = new Storage();
        storage.setId(id);
		return storage;
	}

	default Rayon cloner(Rayon rayon) {
		Rayon dto = new Rayon();
		dto.setCode(rayon.getCode());
		dto.setLibelle(rayon.getLibelle());
		dto.setExclude(rayon.isExclude());
		dto.setUpdatedAt(Instant.now());
		dto.setCreatedAt(Instant.now());
		return dto;

	}

}
