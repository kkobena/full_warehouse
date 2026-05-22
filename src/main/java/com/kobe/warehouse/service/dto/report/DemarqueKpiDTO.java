package com.kobe.warehouse.service.dto.report;

public record DemarqueKpiDTO(
    int nbAjustements,
    long totalQtyPerdue,
    long valeurPerdue
) {}
