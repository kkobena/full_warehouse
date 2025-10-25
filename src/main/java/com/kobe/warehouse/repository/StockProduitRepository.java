package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.StockProduit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data  repository for the StockProduit entity.
 */
@SuppressWarnings("unused")
@Repository
public interface StockProduitRepository extends JpaRepository<StockProduit, Long> {
    Optional<StockProduit> findStockProduitByStorageIdAndProduitId(Long storageId, Long produitId);

    List<StockProduit> findStockProduitByStorageMagasinIdAndProduitId(Long magasinId,
                                                                      Long produitId);

    @Query("SELECT o FROM StockProduit o WHERE  o.produit.id=?1 AND o.storage.id = ?2")
    StockProduit findOneByProduitIdAndStockageId(Long produitId, Long stockageId);

    @Query(value =
        "SELECT SUM(sp.qtyStock)+SUM(sp.qtyUG)  as totalQuantity FROM StockProduit sp WHERE sp.produit.id =:produitId AND sp.storage.magasin.id =:magasinId "
    )
    Integer findTotalQuantityByMagasinIdIdAndProduitId(@Param("magasinId") Long magasinId, @Param("produitId") Long produitId);

}
