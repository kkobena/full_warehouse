package com.kobe.warehouse.service.dto;

/**
 * Résumé d'un produit dont le stock rayon dépasse {@code stockMaxi} après réception.
 * Retourné par l'endpoint {@code GET /api/commandes/{id}/putaway-preview} pour
 * alimenter le modal de confirmation côté frontend.
 */
public record PutawayPreviewItemDTO(
    Integer produitId,
    String produitLibelle,
    String codeCip,
    int qtyRayon,
    int stockMaxiRayon,
    int qtyOverflow,
    int qtyReserveActuelle,
    String classePareto
) {}
