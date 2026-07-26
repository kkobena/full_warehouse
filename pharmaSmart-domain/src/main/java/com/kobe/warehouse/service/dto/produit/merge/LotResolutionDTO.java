package com.kobe.warehouse.service.dto.produit.merge;

/** Résolution choisie par l'utilisateur pour un conflit de lot identifié par {@link ProduitMergePreviewDTO}. */
public record LotResolutionDTO(Integer lotId, LotConflictAction action) {}
