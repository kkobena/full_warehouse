package com.kobe.warehouse.service.dto;

public record FinancesSummaryDTO(
    long totalDetteFournisseurs,
    long totalCreancesTP,
    long nbEcheancesEnRetard,
    long nbFacturesImpayees
) {}
