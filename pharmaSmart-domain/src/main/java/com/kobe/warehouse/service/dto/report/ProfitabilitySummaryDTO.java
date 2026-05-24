package com.kobe.warehouse.service.dto.report;

import java.math.BigDecimal;

/**
 * DTO for aggregated profitability summary from mv_profitability_summary materialized view
 * Provides overall profitability metrics and BCG distribution
 */
public record ProfitabilitySummaryDTO(
    Integer totalProduits,
    Long caTotalGlobal,
    Long coutAchatGlobal,
    Long margeBruteGlobale,
    BigDecimal tauxMargeMoyen,
    // BCG counts
    Integer nbStars,
    Integer nbCashCows,
    Integer nbQuestionMarks,
    Integer nbDogs,
    // BCG revenue
    Long caStars,
    Long caCashCows,
    Long caQuestionMarks,
    Long caDogs,
    // BCG margins
    Long margeStars,
    Long margeCashCows,
    Long margeQuestionMarks,
    Long margeDogs
) {}
