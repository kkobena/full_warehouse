package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.LigneReassort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@SuppressWarnings("unused")
@Repository
public interface LigneReassortRepository extends JpaRepository<LigneReassort, Integer> {

    Optional<LigneReassort> findByStockProduitIdAndReassortId(Integer stockProduitId, Integer reassortId);
}
