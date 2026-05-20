package com.kobe.warehouse.service.dto.report;

import java.math.BigDecimal;

public record ComparativeByFournisseurDTO(
    Integer fournisseurId,
    String fournisseurLibelle,
    Long currentYearCA,
    Long previousYearCA,
    BigDecimal evolutionPct,
    BigDecimal evolutionAmount,
    Integer currentYearCount,
    Integer previousYearCount
) {}
