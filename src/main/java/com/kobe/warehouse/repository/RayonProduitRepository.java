package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.RayonProduit;
import com.kobe.warehouse.domain.enumeration.StorageType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@SuppressWarnings("unused")
@Repository
public interface RayonProduitRepository extends JpaRepository<RayonProduit, Long> {
    Set<RayonProduit> findAllByProduitId(Long produitId);


}
