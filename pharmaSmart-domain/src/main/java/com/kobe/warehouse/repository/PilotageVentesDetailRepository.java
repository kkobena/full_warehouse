package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.SaleId;
import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.service.dto.pilotage.VenteMargeDTO;
import com.kobe.warehouse.service.dto.pilotage.VenteRemiseeDTO;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/**
 * Ventes une à une, pour les listes du pilotage (les agrégats ne gardent pas la vente). Marge sur la quantité demandée, HT,
 * comme l'agrégat des lignes.
 */
public interface PilotageVentesDetailRepository extends Repository<Sales, SaleId> {
    @Query(
        """
        SELECT new com.kobe.warehouse.service.dto.pilotage.VenteMargeDTO(
            s.id, s.saleDate, s.numberTransaction, concat(u.firstName, ' ', u.lastName),
            sum(l.salesAmount * 100.0 / (100 + coalesce(l.taxValue, 0))),
            sum(coalesce(l.costAmount, 0) * l.quantityRequested * 100.0 / (100 + coalesce(l.taxValue, 0))),
            sum(l.salesAmount * 100.0 / (100 + coalesce(l.taxValue, 0))) - sum(coalesce(l.costAmount, 0) * l.quantityRequested * 100.0 / (100 + coalesce(l.taxValue, 0))))
        FROM SalesLine l
        JOIN l.sales s
        LEFT JOIN s.seller u
        WHERE s.saleDate BETWEEN :du AND :au
          AND s.statut = :statut
          AND s.canceled = false
          AND s.categorieChiffreAffaire IN :categories
        GROUP BY s.id, s.saleDate, s.numberTransaction, u.firstName, u.lastName
        HAVING sum(l.salesAmount * 100.0 / (100 + coalesce(l.taxValue, 0))) < sum(coalesce(l.costAmount, 0) * l.quantityRequested * 100.0 / (100 + coalesce(l.taxValue, 0)))
        ORDER BY sum(l.salesAmount * 100.0 / (100 + coalesce(l.taxValue, 0))) - sum(coalesce(l.costAmount, 0) * l.quantityRequested * 100.0 / (100 + coalesce(l.taxValue, 0)))
        """
    )
    List<VenteMargeDTO> listerVentesAMargeNegative(LocalDate du, LocalDate au, SalesStatut statut, Set<CategorieChiffreAffaire> categories, Pageable page);

    /** @param privilege le privilège dont la clé de sécurité autorise une remise ({@code PR_AJOUTER_REMISE_VENTE}) */
    @Query(
        """
        SELECT new com.kobe.warehouse.service.dto.pilotage.VenteRemiseeDTO(
            s.id, s.saleDate, s.numberTransaction, concat(u.firstName, ' ', u.lastName), cast(s.salesAmount AS long), cast(s.discountAmount AS long),
            s.discountAmount * 100.0 / s.salesAmount,
            (SELECT max(concat(o.firstName, ' ', o.lastName))
             FROM UtilisationCleSecurite c JOIN c.navItem n JOIN c.cleSecuriteOwner o
             WHERE c.entityId = s.id AND n.code = :privilege))
        FROM Sales s
        LEFT JOIN s.seller u
        WHERE s.saleDate BETWEEN :du AND :au
          AND s.statut = :statut
          AND s.canceled = false
          AND s.categorieChiffreAffaire IN :categories
          AND s.discountAmount > 0
          AND s.salesAmount > 0
        ORDER BY s.discountAmount DESC
        """
    )
    List<VenteRemiseeDTO> listerPlusFortesRemises(
        LocalDate du,
        LocalDate au,
        SalesStatut statut,
        Set<CategorieChiffreAffaire> categories,
        String privilege,
        Pageable page
    );
}
