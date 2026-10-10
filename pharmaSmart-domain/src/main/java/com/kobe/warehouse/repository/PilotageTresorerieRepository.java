package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.FactureTiersPayant;
import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.InvoiceStatut;
import com.kobe.warehouse.domain.enumeration.ModeClotureAvoir;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.service.dto.pilotage.ComptageDTO;
import com.kobe.warehouse.service.dto.pilotage.DelaiObserveDTO;
import com.kobe.warehouse.service.dto.pilotage.DifferesAgeDTO;
import com.kobe.warehouse.service.dto.pilotage.EncaissementModeJourDTO;
import com.kobe.warehouse.service.dto.pilotage.FactureEncoursDTO;
import com.kobe.warehouse.service.dto.pilotage.FactureOrganismeDTO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/**
 * Onglet « Trésorerie & tiers payant » du pilotage. Organisme d'une facture : son tiers payant, à défaut son groupe (clé T… ou
 * G…), comme le vieillissement des créances. Les factures rattachées à une facture de groupe ne sont pas comptées deux fois.
 */
public interface PilotageTresorerieRepository extends Repository<FactureTiersPayant, Long> {
    @Query(
        """
        SELECT new com.kobe.warehouse.service.dto.pilotage.EncaissementModeJourDTO(
            e.jour, coalesce(e.modePaiement, ''), coalesce(m.libelle, e.modePaiement, 'Non renseigné'), coalesce(sum(e.montant), 0L))
        FROM PilotageEncaissementJour e
        LEFT JOIN PaymentMode m ON m.code = e.modePaiement
        WHERE e.jour BETWEEN :du AND :au
          AND e.typeTransaction IN :types
        GROUP BY e.jour, e.modePaiement, m.libelle
        ORDER BY e.jour
        """
    )
    List<EncaissementModeJourDTO> sommerEncaissementsParJourEtMode(LocalDate du, LocalDate au, Collection<String> types);

    @Query(
        """
        SELECT new com.kobe.warehouse.service.dto.pilotage.ComptageDTO(
            coalesce(cast(u.id AS String), ''), coalesce(concat(u.firstName, ' ', u.lastName), 'Sans caissier'),
            coalesce(sum(e.nbTransactions), 0L), 0L, coalesce(sum(e.montant), 0L))
        FROM PilotageEncaissementJour e
        LEFT JOIN AppUser u ON u.id = e.caissierId
        WHERE e.jour BETWEEN :du AND :au
          AND e.typeTransaction IN :types
        GROUP BY u.id, u.firstName, u.lastName
        """
    )
    List<ComptageDTO> sommerEncaissementsParCaissier(LocalDate du, LocalDate au, Collection<String> types);

    @Query(
        """
        SELECT new com.kobe.warehouse.service.dto.pilotage.ComptageDTO(cast(u.id AS String), concat(u.firstName, ' ', u.lastName), count(c), 0L, 0L)
        FROM CashRegister c
        JOIN c.user u
        WHERE c.beginTime >= :debut AND c.beginTime < :fin
        GROUP BY u.id, u.firstName, u.lastName
        """
    )
    List<ComptageDTO> compterSessionsParCaissier(LocalDateTime debut, LocalDateTime fin);

    @Query(
        """
        SELECT new com.kobe.warehouse.service.dto.pilotage.FactureOrganismeDTO(
            coalesce(concat('T', cast(tp.id AS String)), concat('G', cast(g.id AS String)), ''), coalesce(tp.name, g.name, 'Inconnu'), coalesce(cast(sum(f.montantNet) AS long), 0L), coalesce(sum(f.montantRegle), 0L))
        FROM FactureTiersPayant f
        LEFT JOIN f.tiersPayant tp
        LEFT JOIN f.groupeTiersPayant g
        WHERE f.invoiceDate BETWEEN :du AND :au
          AND f.groupeFactureTiersPayant IS NULL
        GROUP BY tp.id, tp.name, g.id, g.name
        """
    )
    List<FactureOrganismeDTO> sommerFacturationParOrganisme(LocalDate du, LocalDate au);

    @Query(
        """
        SELECT new com.kobe.warehouse.service.dto.pilotage.FactureEncoursDTO(
            coalesce(concat('T', cast(tp.id AS String)), concat('G', cast(g.id AS String)), ''), coalesce(tp.name, g.name, 'Inconnu'), f.invoiceDate, cast(f.montantNet - f.montantRegle AS long), coalesce(tg.delaiReglement, g.delaiReglement))
        FROM FactureTiersPayant f
        LEFT JOIN f.tiersPayant tp
        LEFT JOIN tp.groupeTiersPayant tg
        LEFT JOIN f.groupeTiersPayant g
        WHERE f.statut IN :statuts
          AND f.groupeFactureTiersPayant IS NULL
          AND f.montantNet > f.montantRegle
        """
    )
    List<FactureEncoursDTO> listerFacturesNonSoldees(Collection<InvoiceStatut> statuts);

    @Query(
        """
        SELECT new com.kobe.warehouse.service.dto.pilotage.DelaiObserveDTO(
            coalesce(concat('T', cast(tp.id AS String)), concat('G', cast(g.id AS String)), ''), avg((p.transactionDate - f.invoiceDate) BY DAY), count(DISTINCT f.id))
        FROM InvoicePayment p
        JOIN p.factureTiersPayant f
        LEFT JOIN f.tiersPayant tp
        LEFT JOIN f.groupeTiersPayant g
        WHERE f.groupeFactureTiersPayant IS NULL
          AND f.statut = :solde
        GROUP BY tp.id, g.id
        """
    )
    List<DelaiObserveDTO> listerDelaisObserves(InvoiceStatut solde);

    @Query(
        """
        SELECT new com.kobe.warehouse.service.dto.pilotage.ComptageDTO(
            cast(c.id AS String), concat(c.firstName, ' ', c.lastName), count(s), 0L, coalesce(sum(s.restToPay), 0L))
        FROM Sales s
        JOIN s.customer c
        WHERE s.differe = true
          AND s.restToPay > 0
          AND s.statut = :statut
          AND s.canceled = false
          AND s.categorieChiffreAffaire IN :categories
        GROUP BY c.id, c.firstName, c.lastName
        ORDER BY coalesce(sum(s.restToPay), 0L) DESC
        """
    )
    List<ComptageDTO> sommerDifferesParClient(SalesStatut statut, Set<CategorieChiffreAffaire> categories, Pageable page);

    /** Reste dû des ventes différées selon l'âge de la vente ; les bornes sont des dates, calculées par l'appelant. */
    @Query(
        """
        SELECT new com.kobe.warehouse.service.dto.pilotage.DifferesAgeDTO(
            coalesce(sum(CASE WHEN s.saleDate >= :depuis30 THEN s.restToPay ELSE 0 END), 0L),
            coalesce(sum(CASE WHEN s.saleDate < :depuis30 AND s.saleDate >= :depuis60 THEN s.restToPay ELSE 0 END), 0L),
            coalesce(sum(CASE WHEN s.saleDate < :depuis60 AND s.saleDate >= :depuis90 THEN s.restToPay ELSE 0 END), 0L),
            coalesce(sum(CASE WHEN s.saleDate < :depuis90 THEN s.restToPay ELSE 0 END), 0L))
        FROM Sales s
        WHERE s.differe = true
          AND s.restToPay > 0
          AND s.statut = :statut
          AND s.canceled = false
          AND s.categorieChiffreAffaire IN :categories
        """
    )
    DifferesAgeDTO sommerDifferesParAge(
        LocalDate depuis30,
        LocalDate depuis60,
        LocalDate depuis90,
        SalesStatut statut,
        Set<CategorieChiffreAffaire> categories
    );

    @Query(
        """
        SELECT new com.kobe.warehouse.service.dto.pilotage.ComptageDTO('', 'Émis', count(a), coalesce(sum(a.quantite), 0L), coalesce(sum(a.montant), 0L))
        FROM AvoirClient a
        WHERE a.createdAt >= :debut AND a.createdAt < :fin
        """
    )
    ComptageDTO sommerAvoirsEmis(LocalDateTime debut, LocalDateTime fin);

    @Query(
        """
        SELECT new com.kobe.warehouse.service.dto.pilotage.ComptageDTO('', 'Remboursés', count(a), coalesce(sum(a.quantite), 0L), coalesce(sum(a.montant), 0L))
        FROM AvoirClient a
        WHERE a.clotureLe >= :debut AND a.clotureLe < :fin
          AND a.modeCloture IN :modes
        """
    )
    ComptageDTO sommerAvoirsRembourses(LocalDateTime debut, LocalDateTime fin, Collection<ModeClotureAvoir> modes);
}
