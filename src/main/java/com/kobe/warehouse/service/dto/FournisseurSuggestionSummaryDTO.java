package com.kobe.warehouse.service.dto;

import java.time.LocalDateTime;

/**
 * Résumé agrégé d'une suggestion par fournisseur.
 * Retourné par GET /api/suggestions/par-fournisseur.
 * Les noms de champs correspondent exactement au modèle TypeScript FournisseurSuggestionSummary.
 */
public record FournisseurSuggestionSummaryDTO(
    Integer suggestionId,
    Integer fournisseurId,
    String libelle,
    String statut,
    int nbProduits,
    int nbUrgents,
    long montantEstime,
    long montantEstimeVente,
    String source,
    LocalDateTime updatedAt
) {}
