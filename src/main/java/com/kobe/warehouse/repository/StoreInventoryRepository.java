package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.StoreInventory;
import com.kobe.warehouse.service.dto.StoreInventoryExport;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Spring Data repository for the StoreInventory entity. */
@SuppressWarnings("unused")
@Repository
public interface StoreInventoryRepository extends JpaRepository<StoreInventory, Long> {
  @Query(
      value =
          "SELECT o.inventory_value_cost_begin,o.inventory_value_cost_after,o.inventory_amount_begin,o.inventory_amount_after,o.created_at,o.updated_at,u.first_name,u.last_name FROM store_inventory o,user u WHERE o.user_id=u.id AND o.id=:id",
      nativeQuery = true)
  StoreInventoryExport fetchOneToPrintById(@Param("id") Long id);
}
