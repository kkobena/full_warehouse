package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.OptionPrixProduit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface PrixReferenceRepository extends JpaRepository<OptionPrixProduit, Integer> {
    @Query(
        "select prixReference from OptionPrixProduit prixReference where prixReference.produit.id =:produitId and prixReference.tiersPayant.id =:tiersPayantId and prixReference.enabled = true"
    )
    Optional<OptionPrixProduit> findOneActifByProduitIdAndTiersPayantId(Integer produitId, Integer tiersPayantId);

    List<OptionPrixProduit> findAllByProduitIdAndTiersPayantId(Integer produitId, Integer tiersPayantId);

    @Query(
        "select prixReference from OptionPrixProduit prixReference where prixReference.produit.id =:produitId and prixReference.tiersPayant.id IN(:tiersPayantIds)  and prixReference.enabled = true"
    )
    List<OptionPrixProduit> findByProduitIdAndTiersPayantIds(Integer produitId, Set<Integer> tiersPayantIds);

    List<OptionPrixProduit> findAllByProduitId(Integer produitId);
}
