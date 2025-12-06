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

    // Historical accuracy
    BigDecimal modelAccuracyPct,
    BigDecimal meanAbsoluteError,

    // Seasonality detection
    Boolean seasonalityDetected,
    String peakMonth,
    String lowMonth,

    // Method used
    String forecastMethod,
    Integer dataPointsUsed
) {}
