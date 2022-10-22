package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.StoreInventory;

import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data  repository for the StoreInventory entity.
 */
@SuppressWarnings("unused")
@Repository
public interface StoreInventoryRepository extends JpaRepository<StoreInventory, Long> {
}
