package com.kobe.warehouse.service.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public record AvoirFromBonLignesCommand(
    @NotNull Integer commandeId,
    @NotNull LocalDate commandeOrderDate,
    String commentaire,
    @NotNull List<BonLigneItem> lignes
) {
    public record BonLigneItem(
        @NotNull Integer orderLineId,
        @NotNull LocalDate orderLineOrderDate,
        @NotNull Integer produitId,
        String produitCip,
        @NotNull Integer qtyRetour,
        @NotNull Integer motifRetourId,
        Integer prixAchat   // nullable — falls back to OrderLine.orderCostAmount
    ) {}
}
