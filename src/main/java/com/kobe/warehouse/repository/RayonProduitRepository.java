package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.RayonProduit;
import com.kobe.warehouse.domain.enumeration.StorageType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@SuppressWarnings("unused")
@Repository
public interface RayonProduitRepository extends JpaRepository<RayonProduit, Long> {
    Set<RayonProduit> findAllByProduitId(Long produitId);

    @Query("SELECT COUNT(o) FROM RayonProduit o WHERE  o.produit.id=?1 AND o.rayon.id = ?2")
    long countRayonProduitByProduitIdAndRayonId(Long produitId, Long rayonId);

    @Query("SELECT COUNT(o) FROM RayonProduit o WHERE  o.produit.id=?1 AND o.rayon.storage.id = ?2")
    long countRayonProduitByProduitIdAndStockageId(Long produitId, Long stockageId);

}
