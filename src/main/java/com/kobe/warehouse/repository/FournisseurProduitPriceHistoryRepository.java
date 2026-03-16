package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.FournisseurProduitPriceHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FournisseurProduitPriceHistoryRepository extends JpaRepository<FournisseurProduitPriceHistory, Integer> {

    List<FournisseurProduitPriceHistory> findByFournisseurProduitIdOrderByChangedAtDesc(Integer fournisseurProduitId);
}
