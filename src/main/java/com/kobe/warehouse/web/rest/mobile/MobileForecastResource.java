package com.kobe.warehouse.web.rest.mobile;

import com.kobe.warehouse.service.dto.mobile.DailySalesDTO;
import com.kobe.warehouse.service.dto.mobile.ForecastRequestDTO;
import com.kobe.warehouse.service.mobile.MobileForecastService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for mobile ML forecasting data.
 * Provides historical sales data for machine learning predictions on mobile devices.
 */
@RestController
@RequestMapping("/api/mobile/forecast")
public class MobileForecastResource {

    private static final Logger LOG = LoggerFactory.getLogger(MobileForecastResource.class);

    private final MobileForecastService forecastService;

    public MobileForecastResource(MobileForecastService forecastService) {
        this.forecastService = forecastService;
    }

    /**
     * POST /api/mobile/forecast/history
     * Get historical sales data for ML forecasting.
     *
     * @param request Forecast request parameters
     * @return List of daily sales data
     */
    @PostMapping("/history")
    public ResponseEntity<List<DailySalesDTO>> getForecastHistory(@Valid @RequestBody ForecastRequestDTO request) {
        LOG.debug("REST request to get forecast history: {}", request);

        List<DailySalesDTO> history = forecastService.getDailySalesHistory(request);
        return ResponseEntity.ok(history);
    }

    /**
     * GET /api/mobile/forecast/history
     * Get historical sales data for a specific date range.
     *
     * @param startDate Start date
     * @param endDate End date
     * @return List of daily sales data
     */
    @GetMapping("/history")
    public ResponseEntity<List<DailySalesDTO>> getForecastHistoryByDates(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        LOG.debug("REST request to get forecast history from {} to {}", startDate, endDate);

        List<DailySalesDTO> history = forecastService.getDailySalesHistory(startDate, endDate);
        return ResponseEntity.ok(history);
    }

    /**
     * GET /api/mobile/forecast/statistics
     * Get sales statistics for validation.
     * Used by mobile app to assess data quality before running ML models.
     *
     * @param days Number of days to analyze (default 30)
     * @return Sales statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<MobileForecastService.SalesStatistics> getForecastStatistics(
        @RequestParam(defaultValue = "30") int days
    ) {
        LOG.debug("REST request to get forecast statistics for {} days", days);

        MobileForecastService.SalesStatistics stats = forecastService.getSalesStatistics(days);
        return ResponseEntity.ok(stats);
    }
}
