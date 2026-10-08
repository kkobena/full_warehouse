package com.kobe.warehouse.repository;

import com.kobe.warehouse.service.dto.ordonnance.ProduitGroupeDTO;
import java.util.Collection;
import com.kobe.warehouse.service.dto.controle.MoleculeProduitDTO;
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

    /** Groupe générique des produits (spécialité retenue de confiance) ; un produit sans groupe est absent. */
    @Query(
        "select new com.kobe.warehouse.service.dto.ordonnance.ProduitGroupeDTO(r.produit.id, s.groupeGenerique.id) " +
        "from ProduitRefSpecialite r join r.specialite s " +
        "where r.produit.id in :produitIds and r.decision in :decisions and s.groupeGenerique is not null"
    )
    List<ProduitGroupeDTO> findGroupes(
        @Param("produitIds") Collection<Integer> produitIds,
        @Param("decisions") Collection<DecisionRapprochement> decisions
    );

    /**
     * Molécules des produits par la composition de leur spécialité retenue : seules les décisions de
     * confiance ({@code decisions}) comptent, et seules les molécules reliées au référentiel.
     */
    @Query(
        "select new com.kobe.warehouse.service.dto.controle.MoleculeProduitDTO(prs.produit.id, d.id, d.libelle) " +
        "from ProduitRefSpecialite prs, RefSpecialiteComposition c join c.dci d " +
        "where c.specialite = prs.specialite and prs.decision in :decisions and prs.produit.id in :produitIds"
    )
    List<MoleculeProduitDTO> findMoleculesRetenues(
        @Param("produitIds") Collection<Integer> produitIds,
        @Param("decisions") Collection<DecisionRapprochement> decisions
    );
}
