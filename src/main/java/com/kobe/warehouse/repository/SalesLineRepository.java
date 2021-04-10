package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.SalesLine;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data repository for the SalesLine entity.
 */
@SuppressWarnings("unused")
@Repository
public interface SalesLineRepository extends JpaRepository<SalesLine, Long> {
    List<SalesLine> findBySalesIdOrderByProduitLibelle(Long salesId);

    Optional<SalesLine> findBySalesIdAndProduitId(Long salesId, Long produitId);
}
