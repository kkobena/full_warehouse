package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.StockProduit;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
