package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.Substitut;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubstitutRepository extends JpaRepository<Substitut, Integer> {

    boolean existsByProduitAndSubstitut(Produit produit, Produit substitut);

    java.util.List<Substitut> findAllByProduitId(Integer produitId);
}
