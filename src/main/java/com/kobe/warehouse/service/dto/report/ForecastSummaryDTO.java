package com.kobe.warehouse.service.dto.report;

import java.math.BigDecimal;

/**
 * DTO for forecast summary and statistics
 */
public record ForecastSummaryDTO(
    // Forecast summary
    Long totalForecastedCA3M,      // 3 months forecast
    Long totalForecastedCA6M,      // 6 months forecast
    Long totalForecastedCA12M,     // 12 months forecast

    // Growth trends
    BigDecimal averageMonthlyGrowthPct,
    BigDecimal predictedYearlyGrowthPct,

    // Historical accuracy — null si non calculable (données insuffisantes)
    BigDecimal modelAccuracyPct,
    BigDecimal meanAbsoluteError,

    // Seasonality detection
    Boolean seasonalityDetected,
    String peakMonth,
    String lowMonth,

    // Method used
    String forecastMethod,
    Integer dataPointsUsed,

    /**
     * Qualité des données :
     *   INSUFFICIENT  → < 4 mois  : prévisions très peu fiables
     *   LOW           → 4–11 mois : fiabilité limitée
     *   MEDIUM        → 12–17 mois
     *   HIGH          → >= 18 mois
     */
    String dataQuality
) {}
