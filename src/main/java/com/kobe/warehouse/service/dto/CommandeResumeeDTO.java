package com.kobe.warehouse.service.dto;

/**
 * Résumé léger d'une commande pour le tableau de bord.
 */
public record CommandeResumeeDTO(
    Integer id,
    String orderDate,
    String orderReference,
    String fournisseurLibelle,
    int grossAmount,
    String orderStatus,
    Integer reliquatDeCommandeId
) {}
