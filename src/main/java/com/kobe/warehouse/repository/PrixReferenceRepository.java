package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.PrixReference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PrixReferenceRepository extends  JpaRepository<PrixReference, Long> {
    @Query("select prixRererence from PrixReference prixRererence where prixRererence.produit.id =:produitId and prixRererence.tiersPayant.id =:tiersPayantId and prixRererence.enabled = true")
    Optional<PrixReference> findOneActifByProduitIdAndTiersPayantId(Long produitId, Long tiersPayantId);
    List<PrixReference> findAllByProduitIdAndTiersPayantId(Long produitId, Long tiersPayantId);
}
