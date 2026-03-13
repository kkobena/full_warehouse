package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.InventoryLot;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface InventoryLotRepository extends JpaRepository<InventoryLot, Long> {

    List<InventoryLot> findAllByStoreInventoryLineId(Long storeInventoryLineId);

    @Query("SELECT il FROM InventoryLot il WHERE il.storeInventoryLine.storeInventory.id = :inventoryId")
    List<InventoryLot> findAllByStoreInventoryId(@Param("inventoryId") Long inventoryId);

    void deleteAllByStoreInventoryLineId(Long storeInventoryLineId);
}
