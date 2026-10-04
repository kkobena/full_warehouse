package com.kobe.warehouse.service.sale.dto;

import com.kobe.warehouse.domain.enumeration.ModeClotureAvoir;

public record CloturerAvoirRequest(
    ModeClotureAvoir modeCloture,
    String commentaire,
    Integer montantUtilise,
    /** Remise du produit seulement : nombre d'unités remises. Vide = toutes les unités restantes. */
    Integer quantiteRemise
) {
    public CloturerAvoirRequest(ModeClotureAvoir modeCloture, String commentaire, Integer montantUtilise) {
        this(modeCloture, commentaire, montantUtilise, null);
    }
}
