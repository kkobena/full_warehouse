package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.StoreInventoryLine;
import java.util.List;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.stereotype.Repository;

/** Spring Data repository for the StoreInventoryLine entity. */
@SuppressWarnings("unused")
@Repository
public interface StoreInventoryLineRepository extends JpaRepository<StoreInventoryLine, Long> {
    List<StoreInventoryLine> findAllByStoreInventoryId(Long storeInventoryId);

    long countStoreInventoryLineByUpdatedIsFalseAndStoreInventoryId(Long id);

    @Procedure("proc_close_inventory")
    int procCloseInventory(Integer inventoryId);
}
