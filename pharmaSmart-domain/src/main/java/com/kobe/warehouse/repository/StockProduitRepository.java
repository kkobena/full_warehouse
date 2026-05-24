package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.StockProduit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data  repository for the StockProduit entity.
 */
@SuppressWarnings("unused")
@Repository
public interface StockProduitRepository extends JpaRepository<StockProduit, Integer> {
    Optional<StockProduit> findStockProduitByStorageIdAndProduitId(Integer storageId, Integer produitId);

    List<StockProduit> findStockProduitByStorageMagasinIdAndProduitId(Integer magasinId, Integer produitId);

    @Query("SELECT o FROM StockProduit o WHERE  o.produit.id=?1 AND o.storage.id = ?2")
    StockProduit findOneByProduitIdAndStockageId(Integer produitId, Integer stockageId);

    @Query(
        value = "SELECT SUM(sp.qtyStock)+SUM(sp.qtyUG)  as totalQuantity FROM StockProduit sp WHERE sp.produit.id =:produitId AND sp.storage.magasin.id =:magasinId "
    )
    Integer findTotalQuantityByMagasinIdIdAndProduitId(@Param("magasinId") Integer magasinId, @Param("produitId") Integer produitId);


    @Query(
        value = "SELECT SUM(sp.qtyStock)+SUM(sp.qtyUG)  as totalQuantity FROM StockProduit sp WHERE sp.produit.id =:produitId AND sp.storage.id =:storageId "
    )
    Integer findPointVenteStock(@Param("produitId") Integer produitId, @Param("storageId") Integer storageId);

    @Query(
        value = "SELECT COALESCE(SUM(sp.qtyStock + sp.qtyUG), 0) FROM StockProduit sp WHERE sp.produit.id = :produitId AND sp.storage.id = :reserveStorageId"
    )
    int findReserveStock(@Param("produitId") Integer produitId, @Param("reserveStorageId") Integer reserveStorageId);


    /**
     * Chargement en masse par storage précis (rayon ou réserve).
     * Usage : inventaires RAYON, STORAGE.
     */
    @Query("SELECT sp FROM StockProduit sp JOIN FETCH sp.produit WHERE sp.storage.id = :storageId AND sp.produit.id IN :produitIds")
    List<StockProduit> findAllByStorageIdAndProduitIdIn(
        @Param("storageId") Integer storageId,
        @Param("produitIds") Collection<Integer> produitIds
    );

    /**
     * Chargement en masse agrégé par magasin (rayon + réserve confondus).
     * Retourne une projection [produitId, stockTotal] pour chaque produit demandé.
     * Usage : inventaires MAGASIN, FAMILLY et types thématiques (PERIME, VENDU, etc.)
     */
    @Query("""
        SELECT sp.produit.id, SUM(sp.qtyStock + sp.qtyUG)
        FROM StockProduit sp
        WHERE sp.storage.magasin.id = :magasinId
          AND sp.produit.id IN :produitIds
        GROUP BY sp.produit.id
        """)
    List<Object[]> findAggregatedStockByMagasinIdAndProduitIdIn(
        @Param("magasinId") Integer magasinId,
        @Param("produitIds") Collection<Integer> produitIds
    );

    /**
     * Axe 2 SEMOIS — Chargement en masse par liste de produits et magasin.
     * Retourne toutes les entrées (PRINCIPAL + SAFETY_STOCK) pour les produits donnés.
     * Utilisé pour l'auto-calcul des paramètres rayon/réserve dans le batch SEMOIS.
     */
    @Query("""
        SELECT sp FROM StockProduit sp
        JOIN FETCH sp.storage s
        WHERE sp.produit.id IN :produitIds
          AND s.magasin.id = :magasinId
        """)
    List<StockProduit> findAllByProduitIdInAndMagasinId(
        @Param("produitIds") Collection<Integer> produitIds,
        @Param("magasinId") Integer magasinId
    );

    /**
     * Search stock produits by storage and product criteria
     * Fetches all stocks with their associated produit and storage information
     */
    @Query("""
        SELECT DISTINCT sp FROM StockProduit sp
        LEFT JOIN FETCH sp.produit p
        LEFT JOIN FETCH sp.storage s
        LEFT JOIN FETCH p.stockProduits
        LEFT JOIN FETCH p.fournisseurProduitPrincipal fp
        WHERE s.id = :storageId
        AND s.magasin.id = :magasinId
        AND (
            LOWER(p.libelle) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
            OR LOWER(fp.codeCip) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
        )
        AND sp.qtyStock > 0
        ORDER BY p.libelle
        """)
    List<StockProduit> searchStockProduitsForRepartition(
        @Param("storageId") Integer storageId,
        @Param("magasinId") Integer magasinId,
        @Param("searchTerm") String searchTerm
    );

}
