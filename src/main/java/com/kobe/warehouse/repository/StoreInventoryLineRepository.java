package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.StoreInventoryLine;

import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data  repository for the StoreInventoryLine entity.
 */
@SuppressWarnings("unused")
@Repository
public interface StoreInventoryLineRepository extends JpaRepository<StoreInventoryLine, Long> {

    List<StoreInventoryLine> findAllByStoreInventoryId(Long storeInventoryId);
}
