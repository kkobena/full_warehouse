package com.kobe.warehouse.service.dto.ordonnance;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/** Une ligne désigne un produit du catalogue, ou à défaut un texte libre. */
public record OrdonnanceLigneCreationDTO(
    Integer produitId,
    @Size(max = 255) String texteLu,
    @Size(max = 150) String posologie,
    @Min(1) Integer dureeJours,
    @Min(1) int quantitePrescrite
) {}
