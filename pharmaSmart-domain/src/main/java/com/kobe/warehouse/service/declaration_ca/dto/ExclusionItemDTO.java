package com.kobe.warehouse.service.declaration_ca.dto;

/**
 * Une entité candidate à l'exclusion du chiffre d'affaires à déclarer — un rayon ou un tiers-payant.
 *
 * <p>Un seul DTO pour les deux référentiels : mêmes colonnes, même geste.
 */
public record ExclusionItemDTO(Integer id, String code, String libelle, boolean exclu) {}
