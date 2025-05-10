package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.DailyStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface DailyStockRepository extends JpaRepository<DailyStock, Long> {
    @Query("select stock from DailyStock stock where stock.produit.id =:produitId order by stock.key desc LIMIT 1")
    Optional<DailyStock> findTop1ByProduitIdOrderByKeyDesc(Long produitId);

    Optional<DailyStock> findOneByKeyAndProduitId(LocalDate key, Long produitId);

    @Modifying
    @Query(value = "INSERT INTO daily_stock( date_key, stock, produit_id) SELECT DATE(NOW()) AS date_key, SUM(o.qty_ug+o.qty_stock) AS stock,o.produit_id AS produit_id FROM stock_produit o JOIN produit pr ON pr.id=o.produit_id   WHERE pr.status=0 AND o.produit_id NOT IN (SELECT p.produit_id FROM daily_stock p WHERE p.date_key =DATE(NOW())) GROUP BY o.produit_id", nativeQuery = true)
    void updateDailyStock();
}
