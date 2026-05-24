package com.kobe.warehouse.service.dto;

import java.time.LocalDateTime;

/**
 * Résumé léger d'un envoi PharmaML pour le tableau de bord.
 */
public record PharmaMlEnvoiResumeeDTO(
    Integer id,
    Integer commandeId,
    String commandeOrderDate,
    String commandeRef,
    String fournisseurLibelle,
    String statut,
    LocalDateTime createdAt
) {}
