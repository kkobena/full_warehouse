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
    Integer reliquatDeCommandeId,
    /** Null tant que la commande n'est pas partie : l'écran affiche alors « Brouillon ». */
    String dateEnvoi,
    /** Jours au-delà du délai annoncé par le grossiste ; 0 si dans les temps. */
    long joursDeRetard
) {}
