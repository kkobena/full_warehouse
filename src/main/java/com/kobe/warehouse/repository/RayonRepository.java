package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.Rayon;
import com.kobe.warehouse.domain.Storage;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RayonRepository extends JpaRepository<Rayon, Integer> {
    Page<Rayon> findAllByStorageId(Long storageId, Pageable pageable);

    Optional<Rayon> findFirstByLibelleEquals(String libelle);

    Optional<Rayon> findFirstByLibelleAndStorageId(String libelle, Integer storageId);

    Optional<Rayon> findFirstByCodeAndStorageId(String code, Integer storageId);

    Rayon findByCodeEquals(String code);

    void deleteAllByStorage(Storage storage);
}
