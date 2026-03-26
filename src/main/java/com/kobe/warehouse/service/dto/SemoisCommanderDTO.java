package com.kobe.warehouse.service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * DTO pour déclencher la création de commandes depuis les suggestions SEMOIS.
 * Les lignes sont groupées automatiquement par fournisseur côté service.
 */
public record SemoisCommanderDTO(
    @NotEmpty List<LigneSemois> lignes
) {

    /**
     * Une ligne de suggestion SEMOIS à commander.
     *
     * @param produitId     ID du produit
     * @param fournisseurId ID du fournisseur principal du produit
     * @param quantite      Quantité à commander (>= 1)
     */
    public record LigneSemois(
        @NotNull Integer produitId,
        @NotNull Integer fournisseurId,
        @Min(1) int quantite
    ) {}
}

