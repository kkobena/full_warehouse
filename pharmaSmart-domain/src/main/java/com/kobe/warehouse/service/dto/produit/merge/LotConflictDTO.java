package com.kobe.warehouse.service.dto.produit.merge;

import java.time.LocalDate;

/**
 * Conflit détecté entre un lot du produit source et un lot du produit cible partageant
 * le même {@code numLot} (contrainte unique {@code (num_lot, produit_id)}). L'utilisateur
 * tranche via {@link LotResolutionDTO} avant de confirmer la fusion.
 */
public record LotConflictDTO(
    String numLot,
    Integer sourceLotId,
    Integer sourceQuantity,
    LocalDate sourceExpiryDate,
    Integer targetLotId,
    Integer targetQuantity,
    LocalDate targetExpiryDate
) {}
