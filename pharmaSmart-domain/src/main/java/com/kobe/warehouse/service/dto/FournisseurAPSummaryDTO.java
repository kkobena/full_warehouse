package com.kobe.warehouse.service.dto;

public record FournisseurAPSummaryDTO(
    long totalDu,
    long echeancesDepassees,
    long echeancesProchaines,
    long nbFournisseursActifs
) {}
