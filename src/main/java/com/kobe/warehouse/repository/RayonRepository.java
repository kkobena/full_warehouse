package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.Rayon;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RayonRepository extends JpaRepository<Rayon, Long> {
    Page<Rayon> findAllByStorageId(Long storageId, Pageable pageable);

    Optional<Rayon> findFirstByLibelleEquals(String libelle);

    Optional<Rayon> findFirstByLibelleAndStorageId(String libelle, Long storageId);

    Optional<Rayon> findFirstByCodeAndStorageId(String code, Long storageId);
}
