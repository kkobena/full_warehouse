package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.enumeration.TypeProduit;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data repository for the Produit entity.
 */
@SuppressWarnings("unused")
@Repository
public interface ProduitRepository extends JpaRepository<Produit, Long>,JpaSpecificationExecutor<Produit> {
	default Specification<Produit> specialisationCritereRecherche(String queryString) {
		return (root, query, cb) -> cb.like(cb.upper(root.get("libelle")), queryString.toUpperCase());
	}
	default Specification<Produit> specialisationTypeProduit(TypeProduit typeProduit) {
		return (root, query, cb) -> cb.equal(root.get("typeProduit"), typeProduit);
	}
	Produit findFirstByParentId(Long parentId);
	List<Produit> findAllByParentIdIsNull();

}
