package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.service.dto.StockProduitProjection;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

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

    @Query(
        value = "SELECT SUM(o.qty_ug+o.qty_stock) AS totalStock,o.produit_id AS produitId FROM stock_produit o WHERE o.produit_id NOT IN (SELECT p.produit_id FROM daily_stock p WHERE p.date_key =:now) GROUP BY o.produit_id LIMIT :limit",
        nativeQuery = true
    )
    List<StockProduitProjection> findDailyStock(LocalDate now, int limit);
}
