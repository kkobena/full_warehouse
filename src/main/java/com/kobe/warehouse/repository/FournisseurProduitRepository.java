package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.FournisseurProduit;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface FournisseurProduitRepository extends JpaRepository<FournisseurProduit, Integer> {
    @Query("SELECT COUNT(o) FROM FournisseurProduit o WHERE  o.produit.id=?1 AND o.fournisseur.id = ?2")
    long countFournisseurProduitByProduitIdAndFournisseurId(Integer produitId, Integer produitFournisseurId);

    List<FournisseurProduit> findAllByProduitId(Integer produitId);

    @Query("SELECT COUNT(o) FROM FournisseurProduit o WHERE  o.codeCip=?1 AND o.fournisseur.id = ?2")
    long countFournisseurProduitByCodeCipAndFournisseurId(String codeCip, Integer produitFournisseurId);

    @Query("SELECT COUNT(o) FROM FournisseurProduit o WHERE  o.codeCip=?1 AND o.fournisseur.id <> ?2")
    long countByCodeCipAndFournisseurId(String codeCip, Integer produitFournisseurId);

    @Query("SELECT COUNT(o) FROM FournisseurProduit o WHERE  o.codeCip=?1 AND o.produit.id <> ?2")
    long countByCodeCipAndProduitId(String codeCip, Integer produitId);

    @Query("SELECT COUNT(o) FROM FournisseurProduit o WHERE  o.produit.id=?1")
    long countByProduit(Integer produitId);

    Optional<FournisseurProduit> findOneByProduitIdAndFournisseurId(Integer produitId, Integer fournisseurId);

    List<FournisseurProduit> findAllByFournisseurIdAndProduitParentIsNull(Integer produitId, Pageable pageable);

    List<FournisseurProduit> findByCodeCipContainingOrCodeEanContaining(String codeCip, String codeEan);
}
