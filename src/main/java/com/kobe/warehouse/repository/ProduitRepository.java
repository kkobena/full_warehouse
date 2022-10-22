package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.Produit_;
import com.kobe.warehouse.domain.enumeration.TypeProduit;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data repository for the Produit entity.
 */
@SuppressWarnings("unused")
@Repository
public interface ProduitRepository extends JpaRepository<Produit, Long>, JpaSpecificationExecutor<Produit> {
    default Specification<Produit> specialisationCritereRecherche(String queryString) {
        return (root, query, cb) -> cb.like(cb.upper(root.get(Produit_.libelle)), queryString.toUpperCase());
    }

    default Specification<Produit> specialisationTypeProduit(TypeProduit typeProduit) {
        return (root, query, cb) -> cb.equal(root.get(Produit_.typeProduit), typeProduit);
    }

    Produit findFirstByParentId(Long parentId);

    List<Produit> findAllByParentIdIsNull();
    Optional<Produit> findOneByLibelle(String libelle);
}
