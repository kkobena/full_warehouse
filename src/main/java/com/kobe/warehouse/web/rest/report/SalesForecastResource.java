package com.kobe.warehouse.web.rest.report;

import com.kobe.warehouse.service.dto.report.ForecastSummaryDTO;
import com.kobe.warehouse.service.dto.report.SalesForecastDTO;
import com.kobe.warehouse.service.report.SalesForecastService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for Sales Forecasting
 */
@RestController
@RequestMapping("/api/sales-forecast")
public class SalesForecastResource {

    private final SalesForecastService salesForecastService;

    public SalesForecastResource(SalesForecastService salesForecastService) {
        this.salesForecastService = salesForecastService;
    }

    /**
     * GET /api/sales-forecast : Get sales forecast
     *
     * @param monthsAhead number of months to forecast (default 6)
     * @param method forecasting method (LINEAR_REGRESSION, MOVING_AVERAGE, SEASONAL)
     * @return list of forecast data points
     */
    @GetMapping("")
    public ResponseEntity<List<SalesForecastDTO>> getForecast(
        @RequestParam(defaultValue = "6") Integer monthsAhead,
        @RequestParam(defaultValue = "LINEAR_REGRESSION") String method
    ) {
        List<SalesForecastDTO> forecast = salesForecastService.getForecast(monthsAhead, method);
        return ResponseEntity.ok(forecast);
    }

    /**
     * GET /api/sales-forecast/summary : Get forecast summary
     *
     * @return forecast summary with statistics
     */
    @GetMapping("/summary")
    public ResponseEntity<ForecastSummaryDTO> getForecastSummary() {
        ForecastSummaryDTO summary = salesForecastService.getForecastSummary();
        return ResponseEntity.ok(summary);
    }

    /**
     * GET /api/sales-forecast/historical : Get historical vs forecast
     *
     * @param startDate start date for historical data
     * @param endDate end date for historical data
     * @param monthsAhead months to forecast ahead
     * @return combined historical and forecast data
     */
    @GetMapping("/historical")
    public ResponseEntity<List<SalesForecastDTO>> getHistoricalVsForecast(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        @RequestParam(defaultValue = "6") Integer monthsAhead
    ) {
        List<SalesForecastDTO> data = salesForecastService.getHistoricalVsForecast(startDate, endDate, monthsAhead);
        return ResponseEntity.ok(data);
    }

    /**
     * GET /api/sales-forecast/seasonality : Detect seasonality
     *
     * @return true if seasonality detected
     */
    @GetMapping("/seasonality")
    public ResponseEntity<Boolean> detectSeasonality() {
        Boolean seasonalityDetected = salesForecastService.detectSeasonality();
        return ResponseEntity.ok(seasonalityDetected);
    }
}
