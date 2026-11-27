package com.kobe.warehouse.service.dto.promotion;

import jakarta.validation.constraints.NotNull;

/**
 * {@code prixPromo} n'est lu que pour une promotion à prix fixe, {@code quantite} que pour un pack
 * (nulle : une unité).
 */
public record PromotionProduitFormDTO(@NotNull Integer produitId, Integer prixPromo, Integer quantite) {

    public PromotionProduitFormDTO(Integer produitId, Integer prixPromo) {
        this(produitId, prixPromo, null);
    }
}
