package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.OrderLine;
import com.kobe.warehouse.domain.enumeration.OrderStatut;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Spring Data repository for the OrderLine entity. */
@SuppressWarnings("unused")
@Repository
public interface OrderLineRepository extends JpaRepository<OrderLine, Long> {
    List<OrderLine> findByCommandeIdOrderByFournisseurProduitProduitLibelleAsc(Long commandeId);

    Optional<OrderLine> findFirstByFournisseurProduitIdAndCommandeId(Long fournisseurProduitId, Long commandeId);

    Page<OrderLine> findByCommandeId(Long commandeId, Pageable pageable);

    Long countByCommandeId(Long commandeId);

    int countByCommandeOrderStatusAndFournisseurProduitProduitId(OrderStatut orderStatut, Long produitId);

    boolean existsByFournisseurProduitProduitIdAndCommandeOrderStatus(Long produitId, OrderStatut orderStatus);
}
