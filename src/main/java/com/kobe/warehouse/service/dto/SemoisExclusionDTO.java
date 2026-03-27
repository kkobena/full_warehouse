package com.kobe.warehouse.service.dto;

import java.time.LocalDateTime;

/**
 * DTO pour l'exclusion temporaire d'un produit du module SEMOIS.
 */
public record SemoisExclusionDTO(
    Integer produitId,
    String produitLibelle,
    Integer exclusionDureeJours,
    String exclusionMotif,
    LocalDateTime exclusionDate,
    LocalDateTime exclusionDateFin,
    boolean exclActif
) {
    /** Durée par défaut d'une exclusion temporaire (30 jours). */
    public static final int DEFAULT_DUREE_JOURS = 30;
}

