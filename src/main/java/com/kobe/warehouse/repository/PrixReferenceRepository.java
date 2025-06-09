package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.OptionPrixProduit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface PrixReferenceRepository extends JpaRepository<OptionPrixProduit, Long> {
    @Query(
        "select prixReference from OptionPrixProduit prixReference where prixReference.produit.id =:produitId and prixReference.tiersPayant.id =:tiersPayantId and prixReference.enabled = true"
    )
    Optional<OptionPrixProduit> findOneActifByProduitIdAndTiersPayantId(Long produitId, Long tiersPayantId);

    List<OptionPrixProduit> findAllByProduitIdAndTiersPayantId(Long produitId, Long tiersPayantId);

    @Query(
        "select prixReference from OptionPrixProduit prixReference where prixReference.produit.id =:produitId and prixReference.tiersPayant.id IN(:tiersPayantIds)  and prixReference.enabled = true"
    )
    List<OptionPrixProduit> findByProduitIdAndTiersPayantIds(Long produitId, Set<Long> tiersPayantIds);

    List<OptionPrixProduit> findAllByProduitId(Long produitId);
}
