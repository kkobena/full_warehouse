package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.Rayon;
import com.kobe.warehouse.domain.StoreInventoryLine;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data repository for the StoreInventoryLine entity.
 */
@SuppressWarnings("unused")
@Repository
public interface StoreInventoryLineRepository extends JpaRepository<StoreInventoryLine, Long> {
    List<StoreInventoryLine> findAllByStoreInventoryId(Long storeInventoryId);

    long countStoreInventoryLineByUpdatedIsFalseAndStoreInventoryId(Long id);

    void deleteAllByStoreInventoryId(Long storeInventoryId);

    @Modifying
    @Procedure(name = "StoreInventoryLine.proc_close_inventory")
    int procCloseInventory(@Param("store_inventory_id") Integer inventoryId);

    @Query(value = "SELECT o FROM StoreInventoryLine o JOIN o.produit p JOIN p.fournisseurProduits fp WHERE fp.codeCip IN :codeCips")
    List<StoreInventoryLine> findAllByCodeCip(Set<String> codeCips);

    // ── Nouvelles méthodes (nouveaux services — sans impact sur l'existant) ────

    long countByStoreInventoryId(Long storeInventoryId);

    long countByStoreInventoryIdAndUpdatedIsTrue(Long storeInventoryId);

    long countByStoreInventoryIdAndGapNot(Long storeInventoryId, Integer gap);

    /**
     * Recherche filtrée par inventaire + codes CIP pour l'import CSV.
     * Évite de modifier des lignes appartenant à d'autres inventaires ouverts.
     */
    @Query("""
        SELECT DISTINCT sil FROM StoreInventoryLine sil
        JOIN FETCH sil.produit p
        JOIN p.fournisseurProduits fp
        WHERE sil.storeInventory.id = :storeInventoryId
          AND fp.codeCip IN :codeCips
        """)
    List<StoreInventoryLine> findAllByStoreInventoryIdAndCodeCipIn(
        @Param("storeInventoryId") Long storeInventoryId,
        @Param("codeCips") Collection<String> codeCips
    );

    @Query(
        "SELECT DISTINCT rp.rayon FROM  StoreInventoryLine  s JOIN s.produit p JOIN p.rayonProduits rp WHERE s.storeInventory.id = ?1 ORDER BY rp.rayon.libelle"
    )
    List<Rayon> findAllRayons(Long storeInventoryId);

    @Query(
        "SELECT DISTINCT s FROM  StoreInventoryLine  s JOIN s.produit p JOIN p.rayonProduits rp WHERE s.storeInventory.id = :storeInventoryId AND rp.rayon.id=:rayonId ORDER BY rp.rayon.libelle"
    )
    List<StoreInventoryLine> findAllByStoreInventoryIdAndRayonId(
        @Param("storeInventoryId") Long storeInventoryId,
        @Param("rayonId") Long rayonId
    );

    /** Lignes avec écart ≠ 0 et saisie effectuée (updated = true), triées par écart absolu décroissant. */
    @Query("""
        SELECT sil FROM StoreInventoryLine sil
        JOIN FETCH sil.produit
        WHERE sil.storeInventory.id = :inventoryId
          AND sil.updated = true
          AND sil.gap <> 0
        ORDER BY ABS(sil.gap) DESC
        """)
    List<StoreInventoryLine> findLinesWithGap(@Param("inventoryId") Long inventoryId);
}
