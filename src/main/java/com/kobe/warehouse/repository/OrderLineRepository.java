package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.OrderLine;

import java.util.List;

import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data  repository for the OrderLine entity.
 */
@SuppressWarnings("unused")
@Repository
public interface OrderLineRepository extends JpaRepository<OrderLine, Long> {
	List<OrderLine> findByCommandeIdOrderByProduitLibelleAsc(Long commandeId);
}
