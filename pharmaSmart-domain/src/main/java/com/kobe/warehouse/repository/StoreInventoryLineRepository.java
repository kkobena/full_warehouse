package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.Rayon;
import com.kobe.warehouse.domain.StoreInventoryLine;
import com.kobe.warehouse.service.dto.records.GapLineRecord;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    List<StoreInventoryLine> findAllByProduitId(Integer produitId);

    boolean existsByProduitIdAndStoreInventoryIdAndStorageId(Integer produitId, Long storeInventoryId, Integer storageId);

    long countStoreInventoryLineByUpdatedIsFalseAndStoreInventoryId(Long id);

    /**
     * Identifiants des inventaires ayant au moins une ligne comptée, parmi ceux passés en
     * paramètre. Une seule requête pour toute une page, plutôt qu'un comptage par ligne.
     */
    @Query("SELECT DISTINCT l.storeInventory.id FROM StoreInventoryLine l WHERE l.storeInventory.id IN :ids AND l.updated = TRUE")
    Set<Long> findStoreInventoryIdsWithCountedLines(@Param("ids") Collection<Long> ids);

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

    /**
     * Page des lignes en écart, projetée directement dans {@link GapLineRecord}.
     *
     * <p>Projection et non entités : l'écran de qualification n'a besoin que de huit colonnes,
     * charger des {@code StoreInventoryLine} complets (plus leur {@code Produit}) hydratait
     * inutilement le contexte de persistance sur des inventaires à plusieurs milliers d'écarts.
     * La qualification existante est ramenée par la même requête, au lieu d'un second
     * chargement complet des {@code InventoryGapAnalysis} indexé en mémoire.
     *
     * <p>Le tri est figé ici, écart absolu décroissant : c'est l'ordre utile à l'écran, et
     * l'identifiant en départage les ex æquo — sans ce second critère, deux lignes de même
     * écart pourraient changer de page d'un appel à l'autre et l'une des deux disparaître.
     */
    @Query(
        value = """
            SELECT new com.kobe.warehouse.service.dto.records.GapLineRecord(
                sil.id,
                p.libelle,
                sil.quantityInit,
                sil.quantityOnHand,
                sil.gap,
                ABS(sil.gap) * COALESCE(sil.lastUnitPrice, 0),
                ga.cause,
                ga.commentaire
            )
            FROM StoreInventoryLine sil
            JOIN sil.produit p
            LEFT JOIN InventoryGapAnalysis ga ON ga.storeInventoryLine = sil
            WHERE sil.storeInventory.id = :inventoryId
              AND sil.updated = true
              AND sil.gap <> 0
            ORDER BY ABS(sil.gap) DESC, sil.id
            """,
        countQuery = """
            SELECT COUNT(sil)
            FROM StoreInventoryLine sil
            WHERE sil.storeInventory.id = :inventoryId
              AND sil.updated = true
              AND sil.gap <> 0
            """
    )
    Page<GapLineRecord> findLinesWithGap(@Param("inventoryId") Long inventoryId, Pageable pageable);

    /**
     * Lignes ciblées par une qualification, restreintes à leur inventaire.
     *
     * <p>Le filtre sur l'inventaire n'est pas décoratif : il empêche qu'une charge utile
     * mentionnant l'identifiant d'une ligne d'un autre inventaire vienne y écrire.
     */
    @Query("""
        SELECT sil FROM StoreInventoryLine sil
        WHERE sil.storeInventory.id = :inventoryId
          AND sil.id IN :ids
        """)
    List<StoreInventoryLine> findAllByIdInAndInventoryId(
        @Param("ids") Collection<Long> ids,
        @Param("inventoryId") Long inventoryId
    );
}
