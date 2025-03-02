package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.FournisseurProduit;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface FournisseurProduitRepository extends JpaRepository<FournisseurProduit, Long> {
    @Query("SELECT o FROM FournisseurProduit o WHERE o.principal=TRUE AND o.produit.id=?1")
    FournisseurProduit findFirstByPrincipalIsTrueAndProduitId(Long produitId);

    @Query("SELECT COUNT(o) FROM FournisseurProduit o WHERE o.principal=TRUE AND o.produit.id=?1 AND o.id<> ?2")
    long principalAlreadyExiste(Long produitId, Long produitFournisseurId);

    @Query("SELECT COUNT(o) FROM FournisseurProduit o WHERE  o.produit.id=?1 AND o.fournisseur.id = ?2")
    long countFournisseurProduitByProduitIdAndFournisseurId(Long produitId, Long produitFournisseurId);

    List<FournisseurProduit> findAllByProduitId(Long produitId);

    @Query("SELECT COUNT(o) FROM FournisseurProduit o WHERE  o.codeCip=?1 AND o.fournisseur.id = ?2")
    long countFournisseurProduitByCodeCipAndFournisseurId(String codeCip, Long produitFournisseurId);

    @Query("SELECT COUNT(o) FROM FournisseurProduit o WHERE  o.codeCip=?1 AND o.fournisseur.id <> ?2")
    long countByCodeCipAndFournisseurId(String codeCip, Long produitFournisseurId);

    @Query("SELECT COUNT(o) FROM FournisseurProduit o WHERE  o.codeCip=?1 AND o.produit.id <> ?2")
    long countByCodeCipAndProduitId(String codeCip, Long produitId);

    Optional<FournisseurProduit> findFirstByProduitIdAndFournisseurId(Long produitId, Long fournisseurId);

    List<FournisseurProduit> findAllByFournisseurIdAndProduitParentIsNull(Long produitId, Pageable pageable);
}
