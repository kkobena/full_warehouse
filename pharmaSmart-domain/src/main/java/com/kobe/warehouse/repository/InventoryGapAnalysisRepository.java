package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.InventoryGapAnalysis;
import com.kobe.warehouse.domain.enumeration.CauseEcart;
import java.util.Collection;
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

    /** Qualifications existantes pour un lot de lignes — alimente l'upsert de {@code saveAnalysis}. */
    @Query("""
        SELECT ga FROM InventoryGapAnalysis ga
        JOIN FETCH ga.storeInventoryLine
        WHERE ga.storeInventoryLine.id IN :lineIds
        """)
    List<InventoryGapAnalysis> findAllByStoreInventoryLineIdIn(@Param("lineIds") Collection<Long> lineIds);

    /** Retrait ciblé : l'opérateur a effacé la cause d'une ligne déjà qualifiée. */
    @Modifying
    @Query("""
        DELETE FROM InventoryGapAnalysis ga
        WHERE ga.storeInventoryLine.id IN :lineIds
        """)
    void deleteAllByStoreInventoryLineIdIn(@Param("lineIds") Collection<Long> lineIds);

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
