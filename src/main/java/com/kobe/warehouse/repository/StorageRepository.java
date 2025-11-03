package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.Storage;
import com.kobe.warehouse.domain.enumeration.StorageType;
import java.util.List;
import java.util.Set;

import com.kobe.warehouse.service.dto.projection.IdProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface StorageRepository extends JpaRepository<Storage, Long> {
    List<Storage> findAllByMagasinId(Long magasinId);

    Storage findFirstByMagasinIdAndStorageType(Long magasinId, StorageType storageType);

    Storage findFirstByStorageType(StorageType storageType);

    List<Storage> findAllByMagasin(Magasin magasin);
    @Query("SELECT s.id AS id FROM Storage s WHERE s.magasin.id = :magasinId")
    Set<IdProjection> findIds(Long magasinId);

    @Query("SELECT s.id AS id FROM Storage s WHERE s.magasin.id = :magasinId AND s.storageType =:storageType")
    IdProjection findByMagasinIdAndStorageType(Long magasinId, StorageType storageType);
}
