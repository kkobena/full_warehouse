package com.kobe.warehouse.repository;


import com.kobe.warehouse.domain.StockProduit;
import org.springframework.data.jpa.repository.JpaRepository;
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

    Optional<List<StockProduit>> findStockProduitByStorageMagasinIdAndProduitId(Long magasinId, Long produitId);
}
