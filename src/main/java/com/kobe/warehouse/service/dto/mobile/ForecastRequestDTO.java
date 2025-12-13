package com.kobe.warehouse.service.dto.mobile;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Request DTO for forecast generation (Phase 4)
 */
public record ForecastRequestDTO(
    @Min(7) @Max(90) Integer historicalDays,
    @Min(1) @Max(30) Integer forecastDays
) {

    /**
     * Default constructor with standard values
     */
    public ForecastRequestDTO() {
        this(30, 7); // 30 days history, 7 days forecast
    }

    /**
     * Validate and create with defaults
     */
    public static ForecastRequestDTO withDefaults(Integer historicalDays, Integer forecastDays) {
        int histDays = historicalDays != null && historicalDays >= 7 && historicalDays <= 90
            ? historicalDays
            : 30;

        int fcstDays = forecastDays != null && forecastDays >= 1 && forecastDays <= 30
            ? forecastDays
            : 7;

        return new ForecastRequestDTO(histDays, fcstDays);
    }
}
