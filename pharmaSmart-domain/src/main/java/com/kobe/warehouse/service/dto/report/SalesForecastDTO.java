package com.kobe.warehouse.service.dto.report;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for sales forecast data point
 */
public record SalesForecastDTO(
    LocalDate forecastDate,
    String forecastPeriod,        // "2024-01", "2024-Q1"
    Long forecastedCA,            // Predicted CA
    Long actualCA,                // Actual CA (if available for comparison)
    BigDecimal confidenceLevel,   // 0-100%
    Long lowerBound,              // Lower confidence interval
    Long upperBound,              // Upper confidence interval
    String forecastMethod         // "LINEAR_REGRESSION", "MOVING_AVERAGE", "SEASONAL"
) {}
