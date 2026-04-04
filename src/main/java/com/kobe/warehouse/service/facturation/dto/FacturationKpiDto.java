package com.kobe.warehouse.service.facturation.dto;

public record FacturationKpiDto(
    long totalFacture,
    long totalRegle,
    long totalRestant,
    double tauxRecouvrement,
    long countFactures,
    long countImpayees,
    long countEnRetard
) {}
