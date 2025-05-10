package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.PrixReference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface PrixReferenceRepository extends  JpaRepository<PrixReference, Long> {
    @Query("select prixReference from PrixReference prixReference where prixReference.produit.id =:produitId and prixReference.tiersPayant.id =:tiersPayantId and prixReference.enabled = true")
    Optional<PrixReference> findOneActifByProduitIdAndTiersPayantId(Long produitId, Long tiersPayantId);
    List<PrixReference> findAllByProduitIdAndTiersPayantId(Long produitId, Long tiersPayantId);
    @Query("select prixReference from PrixReference prixReference where prixReference.produit.id =:produitId and prixReference.tiersPayant.id IN(:tiersPayantIds)  and prixReference.enabled = true")
    List<PrixReference> findByProduitIdAndTiersPayantIds(Long produitId, Set<Long> tiersPayantIds);
}
