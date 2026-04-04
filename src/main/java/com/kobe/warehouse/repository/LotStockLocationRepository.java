package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.Lot;
import com.kobe.warehouse.domain.LotStockLocation;
import com.kobe.warehouse.domain.Storage;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LotStockLocationRepository extends JpaRepository<LotStockLocation, Long> {

    Optional<LotStockLocation> findByLotAndStorage(Lot lot, Storage storage);

    @Query("SELECT lsl FROM LotStockLocation lsl WHERE lsl.lot.id = :lotId AND lsl.storage.id = :storageId")
    Optional<LotStockLocation> findByLotIdAndStorageId(@Param("lotId") int lotId, @Param("storageId") int storageId);

    /**
     * Toutes les localisations d'un lot avec du stock (qty > 0),
     * triées : stockage PRINCIPAL d'abord (ordinal 0), puis les autres par qty décroissante.
     */
    @Query("""
        SELECT lsl FROM LotStockLocation lsl
        JOIN lsl.storage s
        WHERE lsl.lot.id = :lotId
          AND lsl.qty > 0
        ORDER BY s.storageType ASC, lsl.qty DESC
        """)
    List<LotStockLocation> findAvailableByLotId(@Param("lotId") Integer lotId);

    /**
     * Lots disponibles dans un storage pour un produit donné, triés FEFO :
     * péremption la plus proche en premier, null en dernier.
     */
    @Query("""
        SELECT lsl FROM LotStockLocation lsl
        JOIN lsl.lot l
        WHERE lsl.storage.id = :storageId
          AND l.produit.id   = :produitId
          AND lsl.qty        > 0
        ORDER BY
          CASE WHEN l.expiryDate IS NULL THEN 1 ELSE 0 END,
          l.expiryDate ASC
        """)
    List<LotStockLocation> findFefoByStorageAndProduit(
        @Param("storageId") Integer storageId,
        @Param("produitId") Integer produitId
    );

    /**
     * Lot le plus récemment reçu (createdDate DESC) pour un produit dans un storage donné.
     * Priorité : lot déjà connu dans ce storage (qty > 0), sinon tout lot du produit.
     */
    @Query("""
        SELECT lsl FROM LotStockLocation lsl
        JOIN lsl.lot l
        WHERE lsl.storage.id = :storageId
          AND l.produit.id   = :produitId
        ORDER BY l.createdDate DESC
        LIMIT 1
        """)
    Optional<LotStockLocation> findLastReceivedByStorageAndProduit(
        @Param("storageId") Integer storageId,
        @Param("produitId") Integer produitId
    );

    /** Somme des quantités d'un lot dans tous ses emplacements. */
    @Query("SELECT COALESCE(SUM(lsl.qty), 0) FROM LotStockLocation lsl WHERE lsl.lot.id = :lotId")
    int sumQtyByLot(@Param("lotId") Integer lotId);

    /** Supprime les entrées épuisées (qty = 0) pour un lot donné. */
    @Modifying
    @Query("DELETE FROM LotStockLocation lsl WHERE lsl.lot.id = :lotId AND lsl.qty = 0")
    void deleteZeroQtyByLot(@Param("lotId") Integer lotId);
}
