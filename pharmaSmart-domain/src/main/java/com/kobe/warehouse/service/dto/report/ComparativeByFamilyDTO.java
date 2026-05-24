package com.kobe.warehouse.service.dto.report;

import java.math.BigDecimal;

public record ComparativeByFamilyDTO(
    Integer familleId,
    String familleLibelle,
    Long currentYearCA,
    Long previousYearCA,
    BigDecimal evolutionPct,
    BigDecimal evolutionAmount,
    Integer currentYearCount,
    Integer previousYearCount
) {}
