package com.kobe.warehouse.service.dto;

/**
 * Agrégation mensuelle des ventes d'un produit.
 * Utilisé pour le sparkline Chart.js de l'onglet Historique de la fiche article.
 *
 * @param anneeMois      Mois au format YYYY-MM (ex: "2026-03")
 * @param quantiteVendue Quantité vendue durant le mois
 * @param montantCa      Chiffre d'affaires du mois en centimes
 * @param nombreVentes   Nombre de transactions de vente distinctes
 */
public record VenteMoisDTO(
    String anneeMois,
    Integer quantiteVendue,
    Integer montantCa,
    Integer nombreVentes
) {}
