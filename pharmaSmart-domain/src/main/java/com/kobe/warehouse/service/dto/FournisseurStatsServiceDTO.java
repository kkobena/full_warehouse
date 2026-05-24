package com.kobe.warehouse.service.dto;

/**
 * Statistiques de service d'un fournisseur sur une période glissante.
 *
 * @param tauxService  Pourcentage de lignes entièrement servies
 *                     (sum(qty_reçue) / sum(qty_commandée) × 100), arrondi à 1 décimale.
 * @param delaiMoyen   Délai moyen en jours entre la date de commande et la réception effective
 *                     (updatedAt de la commande CLOSED).
 * @param periodeJours Nombre de jours de la période analysée (ex. 30, 60, 90).
 */
public record FournisseurStatsServiceDTO(
    double tauxService,
    double delaiMoyen,
    int periodeJours
) {}
