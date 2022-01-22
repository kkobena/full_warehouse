package com.kobe.warehouse.repository;


import com.kobe.warehouse.domain.Storage;
import com.kobe.warehouse.domain.enumeration.StorageType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StorageRepository extends JpaRepository<Storage, Long> {
    List<Storage> findAllByMagasinId(Long magasinId);

    Storage findFirstByMagasinIdAndStorageType(Long magasinId, StorageType storageType);

    Storage findFirstByStorageType(StorageType storageType);
}
