package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.FournisseurProduit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface FournisseurProduitRepository extends JpaRepository<FournisseurProduit, Long> {
    @Query("SELECT o FROM FournisseurProduit o WHERE o.principal=TRUE AND o.produit.id=?1")
    FournisseurProduit findFirstByPrincipalIsTrueAndProduitId(Long produitId);
}
