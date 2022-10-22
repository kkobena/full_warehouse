package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.OrderLine;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Spring Data repository for the OrderLine entity. */
@SuppressWarnings("unused")
@Repository
public interface OrderLineRepository extends JpaRepository<OrderLine, Long> {
  List<OrderLine> findByCommandeIdOrderByProduitLibelleAsc(Long commandeId);

  Optional<OrderLine> findFirstByFournisseurProduitIdAndCommandeId(
      Long fournisseurProduitId, Long commandeId);

  Page<OrderLine> findByCommandeId(Long commandeId, Pageable pageable);

  Long countByCommandeId(Long commandeId);
}
