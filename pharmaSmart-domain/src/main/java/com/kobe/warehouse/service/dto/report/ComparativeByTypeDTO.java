package com.kobe.warehouse.service.dto.report;

import java.math.BigDecimal;

/**
 * DTO for comparative CA by sales type (VNO, VO, VA, VE)
 */
public record ComparativeByTypeDTO(
    String saleType,              // VNO, VO, VA, VE
    String saleTypeLabel,         // "Vente Normale Officine", etc.
    Long currentYearCA,
    Long previousYearCA,
    BigDecimal evolutionPct,
    Integer currentYearCount,
    Integer previousYearCount
) {}
