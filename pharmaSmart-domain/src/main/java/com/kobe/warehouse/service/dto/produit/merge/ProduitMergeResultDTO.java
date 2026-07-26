package com.kobe.warehouse.service.dto.produit.merge;

import java.util.List;
import java.util.Map;

/**
 * Résumé de ce qui a été effectivement fusionné/réaffecté, retourné après confirmation.
 * {@code stockConflicts} liste les emplacements où un ajustement de stock manuel reste
 * nécessaire : la quantité du produit source n'a volontairement pas été reportée sur la cible.
 */
public record ProduitMergeResultDTO(
    Integer targetId,
    List<Integer> mergedSourceIds,
    Map<String, Integer> entityCounts,
    List<StockConflictDTO> stockConflicts
) {}
