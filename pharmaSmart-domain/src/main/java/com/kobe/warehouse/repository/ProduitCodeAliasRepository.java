package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.ProduitCodeAlias;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProduitCodeAliasRepository extends JpaRepository<ProduitCodeAlias, Integer> {
    /** Plusieurs produits peuvent porter le même code : l'unicité est posée par produit. */
    @Query("SELECT a FROM ProduitCodeAlias a JOIN FETCH a.produit WHERE a.code = :code ORDER BY a.id")
    List<ProduitCodeAlias> findAllByCode(@Param("code") String code);

    List<ProduitCodeAlias> findAllByProduitIdOrderByCreatedAtDesc(Integer produitId);

    Optional<ProduitCodeAlias> findFirstByProduitIdAndCode(Integer produitId, String code);
}
