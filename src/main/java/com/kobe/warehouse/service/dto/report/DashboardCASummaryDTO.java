package com.kobe.warehouse.service.dto.report;

import java.math.BigDecimal;

/**
 * DTO for Dashboard CA summary with KPIs for different periods
 */
public record DashboardCASummaryDTO(
    // Today
    Long caToday,
    Long caTodayPrevious,
    BigDecimal caTodayEvolutionPct,

    // Week
    Long caWeek,
    Long caWeekPrevious,
    BigDecimal caWeekEvolutionPct,

    // Month
    Long caMonth,
    Long caMonthPrevious,
    BigDecimal caMonthEvolutionPct,

    // Year
    Long caYear,
    Long caYearPrevious,
    BigDecimal caYearEvolutionPct,

    // Additional metrics
    Integer nbTransactionsToday,
    Integer nbTransactionsWeek,
    Integer nbTransactionsMonth,
    Integer nbTransactionsYear,

    BigDecimal panierMoyenToday,
    BigDecimal panierMoyenWeek,
    BigDecimal panierMoyenMonth,
    BigDecimal panierMoyenYear,

    BigDecimal tauxMargeToday,
    BigDecimal tauxMargeWeek,
    BigDecimal tauxMargeMonth,
    BigDecimal tauxMargeYear
) {}
