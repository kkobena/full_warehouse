package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.Rupture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RuptureRepository extends JpaRepository<Rupture, Integer> {

    /**
     * Met à jour toutes les ruptures d'un produit en marquant product_still_out_of_stock = false
     * (le produit est de nouveau en stock).
     */
    @Modifying
    @Query("UPDATE Rupture r SET r.productStillOutOfStock = false WHERE r.produit.id = :produitId AND r.productStillOutOfStock = true")
    int markProductAsBackInStock(@Param("produitId") Integer produitId);
}
