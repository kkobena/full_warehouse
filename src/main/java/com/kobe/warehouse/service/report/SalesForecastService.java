package com.kobe.warehouse.service.report;

import com.kobe.warehouse.service.dto.report.ForecastSummaryDTO;
import com.kobe.warehouse.service.dto.report.SalesForecastDTO;
import java.time.LocalDate;
import java.util.List;

/**
 * Service interface for Sales Forecasting
 */
public interface SalesForecastService {

    /**
     * Get sales forecast for next N months
     * @param monthsAhead number of months to forecast (3, 6, or 12)
     * @param method forecasting method (LINEAR_REGRESSION, MOVING_AVERAGE, SEASONAL)
     * @return list of forecast data points
     */
    List<SalesForecastDTO> getForecast(Integer monthsAhead, String method);

    /**
     * Get forecast summary with statistics
     * @return summary with totals, trends, and accuracy metrics
     */
    ForecastSummaryDTO getForecastSummary();

    /**
     * Get historical data vs forecast for comparison
     * @param startDate start date for historical data
     * @param endDate end date for historical data
     * @return list combining historical and forecast data
     */
    List<SalesForecastDTO> getHistoricalVsForecast(LocalDate startDate, LocalDate endDate, Integer monthsAhead);

    /**
     * Detect seasonality in historical sales data
     * @return true if seasonality detected
     */
    Boolean detectSeasonality();
}
