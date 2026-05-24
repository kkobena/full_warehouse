package com.kobe.warehouse.service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReglementFournisseurAPCommand(
    @NotNull @Min(1) Integer montant,
    @NotBlank String dateReglement,
    @NotBlank String reference,
    @NotBlank String modeReglement,
    String commentaire,
    Integer commandeId
) {}
