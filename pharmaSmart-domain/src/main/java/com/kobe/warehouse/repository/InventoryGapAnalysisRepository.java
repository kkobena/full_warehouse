package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.InventoryGapAnalysis;
import com.kobe.warehouse.domain.enumeration.CauseEcart;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface InventoryGapAnalysisRepository extends JpaRepository<InventoryGapAnalysis, Long> {

    @Query("""
        SELECT ga FROM InventoryGapAnalysis ga
        WHERE ga.storeInventoryLine.storeInventory.id = :inventoryId
        """)
    List<InventoryGapAnalysis> findAllByInventoryId(@Param("inventoryId") Long inventoryId);

    @Modifying
    @Query("""
        DELETE FROM InventoryGapAnalysis ga
        WHERE ga.storeInventoryLine.storeInventory.id = :inventoryId
        """)
    void deleteAllByInventoryId(@Param("inventoryId") Long inventoryId);

    @Query("""
        SELECT ga.cause, COUNT(ga), SUM(ABS(ga.quantity))
        FROM InventoryGapAnalysis ga
        WHERE ga.storeInventoryLine.storeInventory.id = :inventoryId
        GROUP BY ga.cause
        ORDER BY SUM(ABS(ga.quantity)) DESC
        """)
    List<Object[]> aggregateByInventoryId(@Param("inventoryId") Long inventoryId);

    boolean existsByStoreInventoryLineStoreInventoryId(Long inventoryId);
}
