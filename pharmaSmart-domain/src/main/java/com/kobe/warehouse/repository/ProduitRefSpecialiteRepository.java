package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.ProduitRefSpecialite;
import com.kobe.warehouse.domain.enumeration.DecisionRapprochement;
import com.kobe.warehouse.domain.enumeration.NatureSubstance;
import com.kobe.warehouse.domain.enumeration.StatutRapprochement;
import com.kobe.warehouse.service.referentiel.ProduitReferentielDTO;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProduitRefSpecialiteRepository extends JpaRepository<ProduitRefSpecialite, Integer> {
    Optional<ProduitRefSpecialite> findByProduitId(Integer produitId);

    /** Propositions à relire, les plus sûres d'abord. */
    Page<ProduitRefSpecialite> findByDecisionAndStatutInOrderByScoreDescIdAsc(
        DecisionRapprochement decision,
        List<StatutRapprochement> statuts,
        Pageable pageable
    );

    /**
     * DCI du catalogue portées par une spécialité du référentiel, pour une nature de substance
     * donnée. Seules les DCI déjà reliées au catalogue (ref_dci.dci_id) sont retournées.
     */
    @Query(
        "select distinct c.dci.dci.id from RefSpecialiteComposition c " +
        "where c.specialite.cis = :cis and c.nature = :nature and c.dci.dci is not null"
    )
    List<Integer> findCatalogueDciIds(@Param("cis") String cis, @Param("nature") NatureSubstance nature);

    /**
     * Produits actifs du catalogue dont le rapprochement de confiance (AUTO, VALIDE) tombe dans le
     * même groupe générique : les substituts. Princeps d'abord, puis du moins cher au plus cher.
     */
    @Query(
        "select new com.kobe.warehouse.service.referentiel.ProduitReferentielDTO$Substitut(p.id, p.libelle, s.typeGenerique, p.regularUnitPrice) " +
        "from ProduitRefSpecialite r join r.produit p join r.specialite s " +
        "where s.groupeGenerique.id = :groupeId and p.id <> :produitId " +
        "and r.decision in (com.kobe.warehouse.domain.enumeration.DecisionRapprochement.AUTO, com.kobe.warehouse.domain.enumeration.DecisionRapprochement.VALIDE) " +
        "and p.status = com.kobe.warehouse.domain.enumeration.Status.ENABLE " +
        "order by case when s.typeGenerique = com.kobe.warehouse.domain.enumeration.TypeGenerique.PRINCEPS then 0 else 1 end, p.regularUnitPrice, p.id"
    )
    List<ProduitReferentielDTO.Substitut> findSubstituts(@Param("groupeId") Integer groupeId, @Param("produitId") Integer produitId);
}
