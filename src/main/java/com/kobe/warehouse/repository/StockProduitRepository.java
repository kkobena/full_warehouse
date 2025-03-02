package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.StockProduit;
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

    Optional<List<StockProduit>> findStockProduitByStorageMagasinIdAndProduitId(Long magasinId, Long produitId);

    @Query("SELECT o FROM StockProduit o WHERE  o.produit.id=?1 AND o.storage.id = ?2")
    StockProduit findOneByProduitIdAndStockageId(Long produitId, Long stockageId);
}
