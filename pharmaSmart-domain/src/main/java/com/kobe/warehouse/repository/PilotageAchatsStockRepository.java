package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.PilotageAchatJour;
import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.OrderStatut;
import com.kobe.warehouse.service.dto.pilotage.DelaiFournisseurDTO;
import com.kobe.warehouse.service.dto.pilotage.FournisseurAchatsDTO;
import com.kobe.warehouse.service.dto.pilotage.MoisStockDTO;
import com.kobe.warehouse.service.dto.pilotage.MontantJourDTO;
import com.kobe.warehouse.service.dto.pilotage.MontantsFamilleDTO;
import com.kobe.warehouse.service.dto.pilotage.ProduitDormantDTO;
import com.kobe.warehouse.service.dto.pilotage.StockFamilleDTO;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/** Onglet « Achats & stock » du pilotage : achats reçus (datés à la réception), coût des ventes, photographies du stock. */
public interface PilotageAchatsStockRepository extends Repository<PilotageAchatJour, Long> {
    @Query(
        """
        SELECT new com.kobe.warehouse.service.dto.pilotage.FournisseurAchatsDTO(
            coalesce(cast(f.id AS String), ''), coalesce(f.libelle, 'Sans fournisseur'),
            coalesce(sum(coalesce(ol.orderCostAmount, 0) * coalesce(ol.quantityReceived, 0)), 0L), count(DISTINCT c.id),
            coalesce(sum(coalesce(ol.quantityRequested, 0)), 0L), coalesce(sum(coalesce(ol.quantityReceived, 0)), 0L))
        FROM OrderLine ol
        JOIN ol.commande c
        LEFT JOIN c.fournisseur f
        WHERE c.receiptDate BETWEEN :du AND :au
          AND c.orderStatus IN :statuts
        GROUP BY f.id, f.libelle
        """
    )
    List<FournisseurAchatsDTO> sommerAchatsParFournisseur(LocalDate du, LocalDate au, Collection<OrderStatut> statuts);

    @Query(
        """
        SELECT new com.kobe.warehouse.service.dto.pilotage.DelaiFournisseurDTO(
            coalesce(cast(f.id AS String), ''), avg((c.receiptDate - c.orderDate) BY DAY), count(c))
        FROM Commande c
        LEFT JOIN c.fournisseur f
        WHERE c.receiptDate BETWEEN :du AND :au
          AND c.orderStatus IN :statuts
        GROUP BY f.id
        """
    )
    List<DelaiFournisseurDTO> listerDelaisParFournisseur(LocalDate du, LocalDate au, Collection<OrderStatut> statuts);

    @Query(
        """
        SELECT new com.kobe.warehouse.service.dto.pilotage.MontantJourDTO(a.jour, coalesce(sum(a.montantHt), 0L))
        FROM PilotageAchatJour a
        WHERE a.jour BETWEEN :du AND :au
        GROUP BY a.jour
        ORDER BY a.jour
        """
    )
    List<MontantJourDTO> sommerAchatsHtParJour(LocalDate du, LocalDate au);

    @Query(
        """
        SELECT new com.kobe.warehouse.service.dto.pilotage.MontantsFamilleDTO(
            coalesce(cast(f.id AS String), ''), coalesce(f.libelle, 'Sans famille'), coalesce(sum(a.montantHt), 0L), coalesce(sum(a.montantTtc), 0L))
        FROM PilotageAchatJour a
        JOIN Produit p ON p.id = a.produitId
        LEFT JOIN p.famille f
        WHERE a.jour BETWEEN :du AND :au
        GROUP BY f.id, f.libelle
        """
    )
    List<MontantsFamilleDTO> sommerAchatsParFamille(LocalDate du, LocalDate au);

    /** Coût d'achat des quantités demandées, HT et TTC, par famille. */
    @Query(
        """
        SELECT new com.kobe.warehouse.service.dto.pilotage.MontantsFamilleDTO(
            coalesce(cast(f.id AS String), ''), coalesce(f.libelle, 'Sans famille'), coalesce(sum(l.coutHt), 0L), coalesce(sum(l.coutTtc), 0L))
        FROM PilotageVenteLigneJour l
        JOIN Produit p ON p.id = l.produitId
        LEFT JOIN p.famille f
        WHERE l.jour BETWEEN :du AND :au
          AND l.categorieChiffreAffaire IN :categories
        GROUP BY f.id, f.libelle
        """
    )
    List<MontantsFamilleDTO> sommerCoutVentesParFamille(LocalDate du, LocalDate au, Set<CategorieChiffreAffaire> categories);

    @Query(
        """
        SELECT coalesce(sum(l.coutTtc), 0L)
        FROM PilotageVenteLigneJour l
        WHERE l.jour BETWEEN :du AND :au
          AND l.categorieChiffreAffaire IN :categories
        """
    )
    long sommerCoutVentesTtc(LocalDate du, LocalDate au, Set<CategorieChiffreAffaire> categories);

    @Query(
        """
        SELECT new com.kobe.warehouse.service.dto.pilotage.MoisStockDTO(s.mois, coalesce(sum(s.valeurAchat), 0L))
        FROM PilotageStockMensuel s
        WHERE s.mois BETWEEN :du AND :au
        GROUP BY s.mois
        ORDER BY s.mois
        """
    )
    List<MoisStockDTO> sommerStockParMois(LocalDate du, LocalDate au);

    @Query(
        """
        SELECT new com.kobe.warehouse.service.dto.pilotage.StockFamilleDTO(
            coalesce(cast(f.id AS String), ''), coalesce(f.libelle, 'Sans famille'), coalesce(sum(s.quantite), 0L), coalesce(sum(s.valeurAchat), 0L))
        FROM PilotageStockMensuel s
        JOIN Produit p ON p.id = s.produitId
        LEFT JOIN p.famille f
        WHERE s.mois = :mois
        GROUP BY f.id, f.libelle
        """
    )
    List<StockFamilleDTO> sommerStockParFamille(LocalDate mois);

    /** Produits en stock dans la photographie du mois, sans vente depuis {@code depuis}, par famille. */
    @Query(
        """
        SELECT new com.kobe.warehouse.service.dto.pilotage.StockFamilleDTO(
            coalesce(cast(f.id AS String), ''), coalesce(f.libelle, 'Sans famille'), count(DISTINCT s.produitId), coalesce(sum(s.valeurAchat), 0L))
        FROM PilotageStockMensuel s
        JOIN Produit p ON p.id = s.produitId
        LEFT JOIN p.famille f
        WHERE s.mois = :mois
          AND s.quantite > 0
          AND NOT EXISTS (SELECT 1 FROM PilotageVenteLigneJour l WHERE l.produitId = s.produitId AND l.jour >= :depuis)
        GROUP BY f.id, f.libelle
        """
    )
    List<StockFamilleDTO> sommerDormantsParFamille(LocalDate mois, LocalDate depuis);

    @Query(
        """
        SELECT new com.kobe.warehouse.service.dto.pilotage.ProduitDormantDTO(
            cast(p.id AS String), p.libelle, sum(s.quantite), sum(s.valeurAchat),
            (SELECT max(l.jour) FROM PilotageVenteLigneJour l WHERE l.produitId = p.id))
        FROM PilotageStockMensuel s
        JOIN Produit p ON p.id = s.produitId
        WHERE s.mois = :mois
          AND s.quantite > 0
          AND NOT EXISTS (SELECT 1 FROM PilotageVenteLigneJour l2 WHERE l2.produitId = s.produitId AND l2.jour >= :depuis)
        GROUP BY p.id, p.libelle
        ORDER BY sum(s.valeurAchat) DESC
        """
    )
    List<ProduitDormantDTO> listerDormants(LocalDate mois, LocalDate depuis, Pageable page);
}
