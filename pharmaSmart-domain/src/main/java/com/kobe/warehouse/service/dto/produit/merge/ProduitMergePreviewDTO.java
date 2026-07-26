package com.kobe.warehouse.service.dto.produit.merge;

import java.util.List;
import java.util.Map;

/**
 * Rapport de simulation d'une fusion de produits — ne modifie rien en base.
 * {@code rejectedSourceIds} liste les produits source qui ne peuvent pas être fusionnés
 * (ex. produit parent de déclinaisons) ; {@code lotConflicts} doit être résolu par
 * l'utilisateur (cf. {@link LotResolutionDTO}) avant l'appel de confirmation.
 */
public record ProduitMergePreviewDTO(
    Integer targetId,
    List<Integer> sourceIds,
    List<Integer> rejectedSourceIds,
    Map<String, String> rejectionReasons,
    Map<String, Integer> entityCounts,
    List<LotConflictDTO> lotConflicts,
    List<StockConflictDTO> stockConflicts
) {}
