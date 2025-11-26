package com.kobe.warehouse.service.dto.report;

import java.math.BigDecimal;

/**
 * DTO for overall comparative summary
 */
public record ComparativeSummaryDTO(
    // Year to date comparisons
    Long ytdCurrentCA,
    Long ytdPreviousCA,
    BigDecimal ytdEvolutionPct,

    // Last 12 months comparisons
    Long last12MonthsCA,
    Long previous12MonthsCA,
    BigDecimal last12MonthsEvolutionPct,

    // Best/worst months
    String bestMonthLabel,
    Long bestMonthCA,
    String worstMonthLabel,
    Long worstMonthCA,

    // Average monthly CA
    BigDecimal avgMonthlyCA,
    BigDecimal avgMonthlyEvolution
) {}
