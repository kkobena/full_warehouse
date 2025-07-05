package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.Lot;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface LotRepository extends JpaRepository<Lot, Long> {
    @Query(
        "SELECT o FROM Lot o JOIN o.orderLine ord JOIN ord.fournisseurProduit fp WHERE fp.produit.id =:produitId AND o.expiryDate >:dateLimit  AND o.quantity > 0  ORDER BY o.expiryDate ASC"
    )
    List<Lot> findByProduitId(Long produitId, LocalDate dateLimit);

    @Query(
        "SELECT o FROM Lot o JOIN o.orderLine ord JOIN ord.fournisseurProduit fp WHERE fp.produit.id =:produitId   AND o.quantity > 0  ORDER BY o.expiryDate ASC"
    )
    List<Lot> findByProduitId(Long produitId);
}
