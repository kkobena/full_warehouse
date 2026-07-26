package com.kobe.warehouse.service.dto.produit.merge;

import java.util.List;

/**
 * Requête de fusion : les produits {@code sourceIds} sont fusionnés dans {@code targetId}
 * puis désactivés. {@code lotResolutions} doit couvrir tous les conflits de lot remontés
 * par {@link ProduitMergePreviewDTO#lotConflicts()} — la fusion est rejetée sinon.
 */
public record ProduitMergeRequestDTO(Integer targetId, List<Integer> sourceIds, List<LotResolutionDTO> lotResolutions) {}
