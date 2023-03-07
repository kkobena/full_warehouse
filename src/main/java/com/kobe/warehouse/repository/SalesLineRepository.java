package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.SalesLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data repository for the SalesLine entity.
 */
@SuppressWarnings("unused")
@Repository
public interface SalesLineRepository extends JpaRepository<SalesLine, Long> {
    List<SalesLine> findBySalesIdOrderByProduitLibelle(Long salesId);

    Optional<SalesLine> findBySalesIdAndProduitId(Long salesId, Long produitId);

    Optional<List<SalesLine>> findAllBySalesId(Long salesId);

    List<SalesLine> findAllByQuantityAvoirGreaterThan(Integer zero);
}
