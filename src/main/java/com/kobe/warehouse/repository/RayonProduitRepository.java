package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.RayonProduit;
import com.kobe.warehouse.domain.enumeration.StorageType;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@SuppressWarnings("unused")
@Repository
public interface RayonProduitRepository extends JpaRepository<RayonProduit, Integer> {
    Set<RayonProduit> findAllByProduitId(Integer produitId);

    @Query("SELECT COUNT(o) FROM RayonProduit o WHERE  o.produit.id=?1 AND o.rayon.id = ?2")
    long countRayonProduitByProduitIdAndRayonId(Integer produitId, Integer rayonId);

    @Query("SELECT COUNT(o) FROM RayonProduit o WHERE  o.produit.id=?1 AND o.rayon.storage.id = ?2")
    long countRayonProduitByProduitIdAndStockageId(Integer produitId, Integer stockageId);
}
