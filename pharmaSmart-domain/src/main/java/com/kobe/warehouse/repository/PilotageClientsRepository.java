package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.SaleId;
import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.service.dto.pilotage.ClienteleDTO;
import com.kobe.warehouse.service.dto.pilotage.ComptageDTO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/**
 * Onglet « Clients & équipe » du pilotage : la clientèle identifiée (les agrégats ne gardent pas le client) et ce que l'agrégat
 * des en-têtes écarte (annulations), par vendeur. Ventes clôturées du CA de l'officine, comme partout dans le pilotage.
 */
public interface PilotageClientsRepository extends Repository<Sales, SaleId> {
    @Query(
        """
        SELECT new com.kobe.warehouse.service.dto.pilotage.ClienteleDTO(
            count(DISTINCT c.id), coalesce(sum(s.salesAmount), 0L), coalesce(sum(CASE WHEN c.id IS NOT NULL THEN s.salesAmount ELSE 0 END), 0L))
        FROM Sales s
        LEFT JOIN s.customer c
        WHERE s.saleDate BETWEEN :du AND :au
          AND s.statut = :statut
          AND s.canceled = false
          AND s.categorieChiffreAffaire IN :categories
        """
    )
    ClienteleDTO lireClientele(LocalDate du, LocalDate au, SalesStatut statut, Set<CategorieChiffreAffaire> categories);

    /** Clients dont la toute première vente tombe dans la période. */
    @Query(
        """
        SELECT count(c) FROM Customer c
        WHERE (SELECT min(s.saleDate) FROM Sales s WHERE s.customer = c AND s.statut = :statut
          AND s.canceled = false
          AND s.categorieChiffreAffaire IN :categories) BETWEEN :du AND :au
        """
    )
    long compterNouveaux(LocalDate du, LocalDate au, SalesStatut statut, Set<CategorieChiffreAffaire> categories);

    /** Actifs sur la référence, sans vente sur la période. */
    @Query(
        """
        SELECT count(DISTINCT c.id) FROM Sales s JOIN s.customer c
        WHERE s.saleDate BETWEEN :referenceDu AND :referenceAu
          AND s.statut = :statut
          AND s.canceled = false
          AND s.categorieChiffreAffaire IN :categories
          AND NOT EXISTS (
            SELECT 1 FROM Sales s2 WHERE s2.customer = c AND s2.saleDate BETWEEN :du AND :au
              AND s2.statut = :statut AND s2.canceled = false AND s2.categorieChiffreAffaire IN :categories)
        """
    )
    long compterPerdus(LocalDate referenceDu, LocalDate referenceAu, LocalDate du, LocalDate au, SalesStatut statut, Set<CategorieChiffreAffaire> categories);

    /** Actifs sur la période, absents de la référence, mais clients avant elle. */
    @Query(
        """
        SELECT count(DISTINCT c.id) FROM Sales s JOIN s.customer c
        WHERE s.saleDate BETWEEN :du AND :au
          AND s.statut = :statut
          AND s.canceled = false
          AND s.categorieChiffreAffaire IN :categories
          AND NOT EXISTS (
            SELECT 1 FROM Sales s2 WHERE s2.customer = c AND s2.saleDate BETWEEN :referenceDu AND :referenceAu
              AND s2.statut = :statut AND s2.canceled = false AND s2.categorieChiffreAffaire IN :categories)
          AND EXISTS (
            SELECT 1 FROM Sales s3 WHERE s3.customer = c AND s3.saleDate < :referenceDu
              AND s3.statut = :statut AND s3.canceled = false AND s3.categorieChiffreAffaire IN :categories)
        """
    )
    long compterRevenus(LocalDate referenceDu, LocalDate referenceAu, LocalDate du, LocalDate au, SalesStatut statut, Set<CategorieChiffreAffaire> categories);

    @Query(
        """
        SELECT new com.kobe.warehouse.service.dto.pilotage.ComptageDTO(
            cast(c.id AS String), concat(c.firstName, ' ', c.lastName), count(s), 0L, coalesce(sum(s.salesAmount), 0L))
        FROM Sales s JOIN s.customer c
        WHERE s.saleDate BETWEEN :du AND :au
          AND s.statut = :statut
          AND s.canceled = false
          AND s.categorieChiffreAffaire IN :categories
        GROUP BY c.id, c.firstName, c.lastName
        """
    )
    List<ComptageDTO> sommerCaParClient(LocalDate du, LocalDate au, SalesStatut statut, Set<CategorieChiffreAffaire> categories);

    /** Ventes annulées par vendeur : l'agrégat des en-têtes les garde, marquées. */
    @Query(
        """
        SELECT new com.kobe.warehouse.service.dto.pilotage.ComptageDTO(
            coalesce(cast(v.vendeurId AS String), ''), '', coalesce(sum(v.nbVentes), 0L), 0L, coalesce(sum(v.montantTtc), 0L))
        FROM PilotageVenteJour v
        WHERE v.jour BETWEEN :du AND :au
          AND v.annulee = true
          AND v.categorieChiffreAffaire IN :categories
        GROUP BY v.vendeurId
        """
    )
    List<ComptageDTO> compterAnnulationsParVendeur(LocalDate du, LocalDate au, Set<CategorieChiffreAffaire> categories);

    /** Avoirs clients (quantités demandées et non servies) par vendeur de la vente d'origine. */
    @Query(
        """
        SELECT new com.kobe.warehouse.service.dto.pilotage.ComptageDTO(
            coalesce(cast(u.id AS String), ''), '', count(a), coalesce(sum(a.quantite), 0L), coalesce(sum(a.montant), 0L))
        FROM AvoirClient a
        JOIN a.salesLine l
        JOIN l.sales s
        LEFT JOIN s.seller u
        WHERE a.createdAt >= :debut AND a.createdAt < :fin
        GROUP BY u.id
        """
    )
    List<ComptageDTO> compterAvoirsParVendeur(LocalDateTime debut, LocalDateTime fin);
}
