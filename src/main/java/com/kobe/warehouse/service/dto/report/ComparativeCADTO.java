package com.kobe.warehouse.service.dto.report;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for comparative CA analysis (year over year, month over month)
 */
public record ComparativeCADTO(
    LocalDate period,
    String periodLabel,          // e.g., "2024-01", "2024-Q1"
    Long currentCA,
    Long previousCA,
    BigDecimal evolutionPct,
    BigDecimal evolutionAmount,
    Integer currentTransactions,
    Integer previousTransactions,
    String comparisonType        // "YEARLY", "MONTHLY", "QUARTERLY"
) {}
