package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.Rayon;
import com.kobe.warehouse.domain.Storage;
import com.kobe.warehouse.service.dto.RayonDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomizedRayonService {
    Page<RayonDTO> listRayonsByStorageId(Integer magasinId, Integer storageId, String query, Pageable pageable);

    RayonDTO save(RayonDTO dto);

    RayonDTO update(RayonDTO dto);

    default void buildRayonFromRayonDTO(RayonDTO dto, Rayon rayon) {
        rayon.setCode(dto.getCode());
        rayon.setLibelle(dto.getLibelle());
    }

    default Rayon buildRayonFromRayonDTO(RayonDTO dto) {
        Rayon rayon = new Rayon();
        rayon.setCode(dto.getCode());
        rayon.setLibelle(dto.getLibelle());
        rayon.setStorage(fromId(dto.getStorageId()));
        return rayon;
    }

    default Rayon buildRayonFromRayonDTO(RayonDTO dto, Storage storage) {
        Rayon rayon = new Rayon();
        rayon.setCode(dto.getCode());
        rayon.setLibelle(dto.getLibelle());
        rayon.setStorage(storage);
        return rayon;
    }

    default Storage fromId(Integer id) {
        Storage storage = new Storage();
        storage.setId(id);
        return storage;
    }
}
