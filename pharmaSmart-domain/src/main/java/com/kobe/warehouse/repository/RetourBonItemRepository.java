package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.RetourBonItem;
import com.kobe.warehouse.domain.enumeration.RetourStatut;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@SuppressWarnings("unused")
@Repository
public interface RetourBonItemRepository extends JpaRepository<RetourBonItem, Integer> {
    @Query("SELECT r FROM RetourBonItem r WHERE r.retourBon.id = :retourBonId")
    List<RetourBonItem> findAllByRetourBonId(@Param("retourBonId") Integer retourBonId);

    @Query("SELECT r FROM RetourBonItem r WHERE r.orderLine.id = :orderLineId")
    List<RetourBonItem> findAllByOrderLineId(@Param("orderLineId") Integer orderLineId);

    @Modifying
    @Query("DELETE FROM RetourBonItem r WHERE r.retourBon.id = :retourBonId")
    void deleteAllByRetourBonId(@Param("retourBonId") Integer retourBonId);

    @Query("""
        SELECT COALESCE(SUM(r.qtyMvt), 0)
        FROM RetourBonItem r
        WHERE r.lot.id = :lotId
          AND r.retourBon.statut <> :excludedStatut
          AND (:excludeRetourBonId IS NULL OR r.retourBon.id <> :excludeRetourBonId)
        """)
    int sumQtyMvtByLotId(
        @Param("lotId") Integer lotId,
        @Param("excludeRetourBonId") Integer excludeRetourBonId,
        @Param("excludedStatut") RetourStatut excludedStatut
    );

    @Query("""
        SELECT COALESCE(SUM(r.qtyMvt), 0)
        FROM RetourBonItem r
        WHERE r.orderLine.id = :orderLineId
        AND r.orderLine.orderDate=:orderDate

          AND r.retourBon.statut <> :excludedStatut
          AND (:excludeRetourBonId IS NULL OR r.retourBon.id <> :excludeRetourBonId)
        """)
    int sumQtyMvtByOrderLineId(
        @Param("orderLineId") Integer orderLineId,
        @Param("orderDate") LocalDate orderDate,
        @Param("excludeRetourBonId") Integer excludeRetourBonId,
        @Param("excludedStatut") RetourStatut excludedStatut
    );

    /**
     * Somme de toutes les quantités retournées pour une ligne de commande,
     * tous statuts confondus (utilisé pour la validation anti-surretour).
     * Le retour en cours ({@code excludeRetourBonId}) est exclu pour gérer
     * le cas de la mise à jour.
     */
    @Query("""
        SELECT COALESCE(SUM(r.qtyMvt), 0)
        FROM RetourBonItem r
        WHERE r.orderLine.id = :orderLineId
          AND r.orderLine.orderDate = :orderDate
          AND (:excludeRetourBonId IS NULL OR r.retourBon.id <> :excludeRetourBonId)
        """)
    int sumAllReturnedQtyByOrderLineId(
        @Param("orderLineId") Integer orderLineId,
        @Param("orderDate") LocalDate orderDate,
        @Param("excludeRetourBonId") Integer excludeRetourBonId
    );
}
