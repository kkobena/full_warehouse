package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.SaleLineId;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.enumeration.ExclusionMotif;
import com.kobe.warehouse.service.declaration_ca.dto.IntituleTiersPayantDTO;
import com.kobe.warehouse.service.declaration_ca.dto.JournalKpiDTO;
import com.kobe.warehouse.service.declaration_ca.dto.JournalLigneDTO;
import com.kobe.warehouse.service.declaration_ca.dto.JournalVenteDTO;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Les lectures des journaux d'exclusion du chiffre d'affaires à déclarer.
 *
 * <p>En JPQL et non en SQL natif : ces requêtes suivent des associations que le modèle décrit déjà
 * — la vente, le produit, son fournisseur principal, le rayon, le tiers-payant. Les écrire en SQL
 * obligeait à recopier des noms de colonnes que rien ne vérifie, dont deux étaient faux du premier
 * coup ({@code tierspayant_id}, {@code number_transaction}). Ici, un renommage d'attribut casse la
 * compilation plutôt que l'écran.
 *
 * <p>Les projections passent par des expressions constructeur : les journaux affichent une dizaine
 * de colonnes issues de quatre tables, charger les entités entières pour n'en lire que cela ferait
 * payer chaque ligne bien au-delà de ce qu'elle rapporte.
 */
@Repository
public interface JournalExclusionRepository extends JpaRepository<SalesLine, SaleLineId> {
    /**
     * Les lignes portant un motif d'exclusion, hors tiers-payant.
     *
     * <p>{@code storageId} désigne le magasin dont on lit le rayon : un produit peut en avoir un par
     * magasin, et c'est celui du magasin principal qui a décidé de l'exclusion. Retenir « un rayon
     * exclu quelconque » afficherait un libellé sans rapport avec la décision prise.
     */
    @Query(
        """
        select new com.kobe.warehouse.service.declaration_ca.dto.JournalLigneDTO(
            s.id, sl.saleDate, s.numberTransaction,
            coalesce(fp.codeCip, p.codeEanLaboratoire), p.libelle,
            r.libelle,
            sl.quantityRequested, sl.quantityUg,
            sl.quantityRequested * sl.regularUnitPrice,
            sl.quantityRequested * sl.regularUnitPrice - coalesce(sl.amountToBeTakenIntoAccount, 0),
            sl.salesAmount - sl.quantityRequested * sl.costAmount)
        from SalesLine sl
        join sl.sales s
        join sl.produit p
        left join p.fournisseurProduitPrincipal fp
        left join RayonProduit rp on rp.produit = p
        left join rp.rayon r on r.storage.id = :storageId
        where sl.exclusionMotif = :motif
          and sl.saleDate between :dateDebut and :dateFin
          and sl.toIgnore = false
          and s.magasin.id = :magasinId
          and s.statut = com.kobe.warehouse.domain.enumeration.SalesStatut.CLOSED
          and s.canceled = false
          and (:terme is null
               or upper(p.libelle) like :terme
               or upper(fp.codeCip) like :terme
               or upper(p.codeEanLaboratoire) like :terme)
        order by sl.saleDate desc, s.id desc, p.libelle
        """
    )
    List<JournalLigneDTO> lignes(
        @Param("motif") ExclusionMotif motif,
        @Param("dateDebut") LocalDate dateDebut,
        @Param("dateFin") LocalDate dateFin,
        @Param("terme") String terme,
        @Param("magasinId") Integer magasinId,
        @Param("storageId") Integer storageId,
        Pageable pageable
    );

    /** Les lignes d'une vente donnée, pour le panneau de détail du journal tiers-payant. */
    @Query(
        """
        select new com.kobe.warehouse.service.declaration_ca.dto.JournalLigneDTO(
            s.id, sl.saleDate, s.numberTransaction,
            coalesce(fp.codeCip, p.codeEanLaboratoire), p.libelle,
            null,
            sl.quantityRequested, sl.quantityUg,
            sl.quantityRequested * sl.regularUnitPrice,
            sl.quantityRequested * sl.regularUnitPrice - coalesce(sl.amountToBeTakenIntoAccount, 0),
            sl.salesAmount - sl.quantityRequested * sl.costAmount)
        from SalesLine sl
        join sl.sales s
        join sl.produit p
        left join p.fournisseurProduitPrincipal fp
        where s.id = :saleId
          and s.saleDate = :saleDate
          and s.magasin.id = :magasinId
          and sl.exclusionMotif = :motif
          and sl.toIgnore = false
        order by p.libelle
        """
    )
    List<JournalLigneDTO> lignesDeLaVente(
        @Param("saleId") Long saleId,
        @Param("saleDate") LocalDate saleDate,
        @Param("motif") ExclusionMotif motif,
        @Param("magasinId") Integer magasinId
    );

    /**
     * Les ventes écartées au titre d'un tiers-payant exclu, une ligne par vente.
     *
     * <p>Les intitulés des tiers-payants n'y figurent pas : une vente peut en porter plusieurs, et
     * les joindre ici la dupliquerait — donc doublerait ses montants. Ils sont rapportés par
     * {@link #nomsTiersPayant}.
     */
    @Query(
        """
        select new com.kobe.warehouse.service.declaration_ca.dto.JournalVenteDTO(
            s.id, s.saleDate, s.numberTransaction,
            null,
            concat(c.firstName, ' ', c.lastName),
            sum(sl.quantityRequested * sl.regularUnitPrice),
            sum(sl.quantityRequested * sl.regularUnitPrice - coalesce(sl.amountToBeTakenIntoAccount, 0)),
            sum(sl.salesAmount - sl.quantityRequested * sl.costAmount),
            count(sl))
        from SalesLine sl
        join sl.sales s
        join sl.produit p
        left join p.fournisseurProduitPrincipal fp
        left join s.customer c
        where sl.exclusionMotif = :motif
          and sl.saleDate between :dateDebut and :dateFin
          and sl.toIgnore = false
          and s.magasin.id = :magasinId
          and s.statut = com.kobe.warehouse.domain.enumeration.SalesStatut.CLOSED
          and s.canceled = false
          and (:terme is null
               or upper(p.libelle) like :terme
               or upper(fp.codeCip) like :terme
               or upper(p.codeEanLaboratoire) like :terme)
          and (:tiersPayantId is null
               or exists (select 1 from ThirdPartySaleLine tpsl
                           where tpsl.sale = s
                             and tpsl.clientTiersPayant.tiersPayant.id = :tiersPayantId))
        group by s.id, s.saleDate, s.numberTransaction, c.firstName, c.lastName
        order by s.saleDate desc, s.id desc
        """
    )
    List<JournalVenteDTO> ventesTiersPayant(
        @Param("motif") ExclusionMotif motif,
        @Param("dateDebut") LocalDate dateDebut,
        @Param("dateFin") LocalDate dateFin,
        @Param("terme") String terme,
        @Param("magasinId") Integer magasinId,
        @Param("tiersPayantId") Integer tiersPayantId,
        Pageable pageable
    );

    /** Les intitulés de tiers-payant, vente par vente : un couple par rattachement. */
    @Query(
        """
        select new com.kobe.warehouse.service.declaration_ca.dto.IntituleTiersPayantDTO(
            tpsl.sale.id, tp.fullName)
        from ThirdPartySaleLine tpsl
        join tpsl.clientTiersPayant ctp
        join ctp.tiersPayant tp
        where tpsl.sale.id in :saleIds
        order by tp.fullName
        """
    )
    List<IntituleTiersPayantDTO> nomsTiersPayant(@Param("saleIds") Collection<Long> saleIds);

    /**
     * Les cumuls de l'ensemble filtré, hors plafond d'affichage.
     *
     * <p>Sans {@code group by} la requête rend toujours une ligne, y compris sur un ensemble vide :
     * l'écran reçoit des zéros plutôt qu'une absence de réponse à interpréter.
     *
     * <p>{@code count(distinct s.id)} et non le couple {@code (id, saleDate)} : l'identifiant vient
     * d'une séquence, il ne se répète pas d'une partition à l'autre.
     */
    @Query(
        """
        select new com.kobe.warehouse.service.declaration_ca.dto.JournalKpiDTO(
            count(distinct s.id),
            count(sl),
            coalesce(sum(sl.quantityRequested), 0L),
            coalesce(sum(sl.quantityUg), 0L),
            coalesce(sum(cast(sl.quantityRequested * sl.regularUnitPrice as Long)), 0L),
            coalesce(sum(cast(sl.quantityRequested * sl.regularUnitPrice
                              - coalesce(sl.amountToBeTakenIntoAccount, 0) as Long)), 0L),
            coalesce(sum(cast(sl.salesAmount - sl.quantityRequested * sl.costAmount as Long)), 0L))
        from SalesLine sl
        join sl.sales s
        join sl.produit p
        left join p.fournisseurProduitPrincipal fp
        where sl.exclusionMotif = :motif
          and sl.saleDate between :dateDebut and :dateFin
          and sl.toIgnore = false
          and s.magasin.id = :magasinId
          and s.statut = com.kobe.warehouse.domain.enumeration.SalesStatut.CLOSED
          and s.canceled = false
          and (:terme is null
               or upper(p.libelle) like :terme
               or upper(fp.codeCip) like :terme
               or upper(p.codeEanLaboratoire) like :terme)
          and (:tiersPayantId is null
               or exists (select 1 from ThirdPartySaleLine tpsl
                           where tpsl.sale = s
                             and tpsl.clientTiersPayant.tiersPayant.id = :tiersPayantId))
        """
    )
    JournalKpiDTO indicateurs(
        @Param("motif") ExclusionMotif motif,
        @Param("dateDebut") LocalDate dateDebut,
        @Param("dateFin") LocalDate dateFin,
        @Param("terme") String terme,
        @Param("magasinId") Integer magasinId,
        @Param("tiersPayantId") Integer tiersPayantId
    );
}
