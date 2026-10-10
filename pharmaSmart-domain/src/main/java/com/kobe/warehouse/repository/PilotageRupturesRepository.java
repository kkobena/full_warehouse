package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.Rupture;
import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.service.dto.pilotage.ComptageDTO;
import com.kobe.warehouse.service.dto.pilotage.PeremptionMoisDTO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/**
 * Ruptures et péremptions du pilotage. Ruptures fournisseurs : table {@code rupture} ; ruptures au comptoir (ventes manquées) :
 * table {@code avoir_client} et quantités en avoir des lignes de vente ; péremptions : lots encore en stock.
 */
public interface PilotageRupturesRepository extends Repository<Rupture, Integer> {
    @Query("SELECT count(ol) FROM OrderLine ol JOIN ol.commande c WHERE c.orderDate BETWEEN :du AND :au")
    long compterLignesCommandees(LocalDate du, LocalDate au);

    @Query("SELECT count(r) FROM Rupture r WHERE r.dateMtv >= :debut AND r.dateMtv < :fin")
    long compterRuptures(LocalDateTime debut, LocalDateTime fin);

    @Query(
        """
        SELECT new com.kobe.warehouse.service.dto.pilotage.ComptageDTO(
            coalesce(cast(f.id AS String), ''), coalesce(f.libelle, 'Sans fournisseur'), count(r), coalesce(sum(r.qty), 0L), 0L)
        FROM Rupture r
        LEFT JOIN r.fournisseur f
        WHERE r.dateMtv >= :debut AND r.dateMtv < :fin
        GROUP BY f.id, f.libelle
        ORDER BY count(r) DESC
        """
    )
    List<ComptageDTO> sommerRupturesParFournisseur(LocalDateTime debut, LocalDateTime fin);

    @Query(
        """
        SELECT new com.kobe.warehouse.service.dto.pilotage.ComptageDTO(cast(p.id AS String), p.libelle, count(r), coalesce(sum(r.qty), 0L), 0L)
        FROM Rupture r
        JOIN r.produit p
        WHERE r.dateMtv >= :debut AND r.dateMtv < :fin
        GROUP BY p.id, p.libelle
        ORDER BY count(r) DESC
        """
    )
    List<ComptageDTO> sommerRupturesParProduit(LocalDateTime debut, LocalDateTime fin, Pageable page);

    @Query(
        """
        SELECT new com.kobe.warehouse.service.dto.pilotage.ComptageDTO(
            cast(p.id AS String), p.libelle, count(a), coalesce(sum(a.quantite), 0L), coalesce(sum(a.montant), 0L))
        FROM AvoirClient a
        JOIN a.produit p
        WHERE a.createdAt >= :debut AND a.createdAt < :fin
        GROUP BY p.id, p.libelle
        ORDER BY coalesce(sum(a.montant), 0L) DESC
        """
    )
    List<ComptageDTO> sommerVentesManqueesParProduit(LocalDateTime debut, LocalDateTime fin, Pageable page);

    @Query(
        """
        SELECT new com.kobe.warehouse.service.dto.pilotage.ComptageDTO(
            '', 'Total', count(a), coalesce(sum(a.quantite), 0L), coalesce(sum(a.montant), 0L))
        FROM AvoirClient a
        WHERE a.createdAt >= :debut AND a.createdAt < :fin
        """
    )
    ComptageDTO sommerVentesManquees(LocalDateTime debut, LocalDateTime fin);

    /** Quantités en avoir et quantités demandées des ventes de la période : le taux de ventes manquées au comptoir. */
    @Query(
        """
        SELECT new com.kobe.warehouse.service.dto.pilotage.ComptageDTO(
            '', 'Total', coalesce(sum(l.quantiteDemandee), 0L), coalesce(sum(l.quantiteAvoir), 0L), 0L)
        FROM PilotageVenteLigneJour l
        WHERE l.jour BETWEEN :du AND :au
          AND l.categorieChiffreAffaire IN :categories
        """
    )
    ComptageDTO sommerQuantitesEnAvoir(LocalDate du, LocalDate au, Set<CategorieChiffreAffaire> categories);

    @Query(
        """
        SELECT new com.kobe.warehouse.service.dto.pilotage.PeremptionMoisDTO(
            year(l.expiryDate), month(l.expiryDate), coalesce(sum(l.currentQuantity), 0L),
            coalesce(sum(l.currentQuantity * coalesce(l.prixAchat, 0)), 0L))
        FROM Lot l
        WHERE l.expiryDate BETWEEN :du AND :au
          AND l.currentQuantity > 0
        GROUP BY year(l.expiryDate), month(l.expiryDate)
        ORDER BY year(l.expiryDate), month(l.expiryDate)
        """
    )
    List<PeremptionMoisDTO> sommerPeremptionsParMois(LocalDate du, LocalDate au);
}
