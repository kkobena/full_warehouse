package com.kobe.warehouse.service.stock;

import com.kobe.warehouse.service.dto.ProduitIndicateursDTO;
import com.kobe.warehouse.service.dto.VenteMoisDTO;
import java.util.List;
import java.util.Optional;

/**
 * Service d'indicateurs analytiques pour la fiche article produit.
 * Agrège les données de {@code v_stock_rotation} et {@code v_abc_pareto_analysis}.
 */
public interface ProduitIndicateursService {

    /**
     * Retourne les indicateurs analytiques d'un produit (rotation, CMM, jours de stock, pareto).
     * Conçu pour le chargement lazy du panneau détail de la fiche article.
     *
     * @param produitId identifiant du produit
     * @return indicateurs ou {@code Optional.empty()} si le produit est inconnu
     */
    Optional<ProduitIndicateursDTO> getIndicateurs(Integer produitId);

    /**
     * Retourne l'historique mensuel des ventes d'un produit sur les N derniers mois.
     * Utilisé pour le sparkline Chart.js de l'onglet Historique.
     *
     * @param produitId identifiant du produit
     * @param nbMois    nombre de mois à retourner (défaut recommandé : 12)
     * @return liste triée par mois croissant
     */
    List<VenteMoisDTO> getVentesMensuelles(Integer produitId, int nbMois);
}
