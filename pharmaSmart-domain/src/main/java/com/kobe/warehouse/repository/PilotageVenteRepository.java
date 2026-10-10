package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.SaleId;
import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.service.dto.pilotage.ChiffreAffairesReferenceDTO;
import java.time.LocalDate;
import java.util.Set;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/**
 * Lectures des ventes pour le pilotage. La définition du CA de l'officine vit ici, une seule fois : ventes clôturées,
 * non annulées, de catégorie {@code CA} — les ventes importées du dépôt en font partie, les ventes au dépôt
 * ({@code CA_DEPOT}) non. Les contre-ventes d'annulation portent le statut {@code CANCELED} et sortent d'elles-mêmes.
 */
public interface PilotageVenteRepository extends Repository<Sales, SaleId> {
    default ChiffreAffairesReferenceDTO calculerChiffreAffairesOfficine(LocalDate du, LocalDate au) {
        return calculerChiffreAffaires(du, au, SalesStatut.CLOSED, CategorieChiffreAffaire.officine());
    }

    @Query(
        """
        SELECT new com.kobe.warehouse.service.dto.pilotage.ChiffreAffairesReferenceDTO(
            count(s), coalesce(sum(s.salesAmount), 0L), coalesce(sum(s.htAmount), 0L), coalesce(sum(s.discountAmount), 0L))
        FROM Sales s
        WHERE s.saleDate BETWEEN :du AND :au
          AND s.statut = :statut
          AND s.canceled = false
          AND s.categorieChiffreAffaire IN :categories
        """
    )
    ChiffreAffairesReferenceDTO calculerChiffreAffaires(
        LocalDate du,
        LocalDate au,
        SalesStatut statut,
        Set<CategorieChiffreAffaire> categories
    );
}
