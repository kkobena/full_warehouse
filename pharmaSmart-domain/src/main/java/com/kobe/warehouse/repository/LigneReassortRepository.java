package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.LigneReassort;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@SuppressWarnings("unused")
@Repository
public interface LigneReassortRepository extends JpaRepository<LigneReassort, Integer> {

    Optional<LigneReassort> findByStockProduitIdAndReassortId(Integer stockProduitId, Integer reassortId);
}
