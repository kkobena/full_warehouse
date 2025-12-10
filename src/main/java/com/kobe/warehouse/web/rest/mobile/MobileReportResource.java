package com.kobe.warehouse.web.rest.mobile;

import com.kobe.warehouse.service.dto.mobile.MobileAlertDetailDTO;
import com.kobe.warehouse.service.dto.mobile.MobileDashboardDTO;
import com.kobe.warehouse.service.dto.mobile.MobilePerformanceDTO;
import com.kobe.warehouse.service.dto.mobile.MobileProductQuickInfoDTO;
import com.kobe.warehouse.service.dto.mobile.MobileTodoDTO;
import com.kobe.warehouse.service.mobile.MobileAlertService;
import com.kobe.warehouse.service.mobile.MobileDashboardService;
import com.kobe.warehouse.service.mobile.MobilePerformanceService;
import com.kobe.warehouse.service.mobile.MobileProductService;
import com.kobe.warehouse.service.mobile.MobileTodoService;
import com.kobe.warehouse.service.stock.dto.ProduitSearch;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for mobile report endpoints.
 * Optimized for mobile clients with minimal payload and single-request patterns.
 *
 * <p>All endpoints are designed for:
 * - Minimal payload size
 * - Single request for complete screen data
 * - Efficient caching support
 * - Mobile-friendly error responses
 */
@RestController
@RequestMapping("/api/mobile")
//@Secured({AuthoritiesConstants.ADMIN, AuthoritiesConstants.USER})
public class MobileReportResource {

    private static final Logger LOG = LoggerFactory.getLogger(MobileReportResource.class);

    private final MobileDashboardService dashboardService;
    private final MobileAlertService alertService;
    private final MobileProductService productService;
    private final MobileTodoService todoService;
    private final MobilePerformanceService performanceService;

    public MobileReportResource(
        MobileDashboardService dashboardService,
        MobileAlertService alertService,
        MobileProductService productService,
        MobileTodoService todoService,
        MobilePerformanceService performanceService
    ) {
        this.dashboardService = dashboardService;
        this.alertService = alertService;
        this.productService = productService;
        this.todoService = todoService;
        this.performanceService = performanceService;
    }

    // =========================================================================
    // PHASE 1: MVP ENDPOINTS
    // =========================================================================

    /**
     * GET /api/mobile/dashboard
     * Get complete dashboard data for home screen.
     *
     * <p>Returns all data needed for the main dashboard in a single request:
     * - Daily CA with target and progress
     * - Transaction stats
     * - Alerts summary
     * - Top products
     * - CA trend (7 days)
     *
     * @param date Optional date (defaults to today)
     * @return Complete dashboard data
     */
    @GetMapping("/dashboard")
    public ResponseEntity<MobileDashboardDTO> getDashboard(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        LOG.debug("REST request to get mobile dashboard for date: {}", targetDate);

        MobileDashboardDTO dashboard = dashboardService.getDashboard(targetDate);
        return ResponseEntity.ok(dashboard);
    }

    /**
     * GET /api/mobile/alerts
     * Get detailed list of alerts.
     *
     * @param types Optional filter by alert types (comma-separated)
     * @return List of detailed alerts
     */
    @GetMapping("/alerts")
    public ResponseEntity<List<MobileAlertDetailDTO>> getAlerts(@RequestParam(required = false) List<String> types) {
        LOG.debug("REST request to get mobile alerts with types: {}", types);

        List<MobileAlertDetailDTO> alerts = alertService.getAlerts(types);
        return ResponseEntity.ok(alerts);
    }

    /**
     * GET /api/mobile/products/{id}/quick-info
     * Get quick product information for modal view.
     *
     * @param id Product ID
     * @return Product quick info with stock, price, lots, and sales stats
     */
    @GetMapping("/products/{id}/quick-info")
    public ResponseEntity<MobileProductQuickInfoDTO> getProductQuickInfo(@PathVariable Integer id) {
        LOG.debug("REST request to get product quick info for ID: {}", id);

        MobileProductQuickInfoDTO productInfo = productService.getProductQuickInfo(id);

        if (productInfo == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(productInfo);
    }

    /**
     * GET /api/mobile/products/search
     * Search products by name or code.
     *
     * @param q Search query
     * @param limit Max results (default 20)
     * @return List of matching products
     */
    @GetMapping("/products/search")
    public ResponseEntity<List<ProduitSearch>> searchProducts(@RequestParam String q, @RequestParam(defaultValue = "20") int limit) {
        LOG.debug("REST request to search products with query: {}", q);

        if (q == null || q.trim().length() < 2) {
            return ResponseEntity.badRequest().build();
        }

        List<ProduitSearch> results = productService.searchProducts(q.trim(), limit);
        return ResponseEntity.ok(results);
    }

    /**
     * GET /api/mobile/todos
     * Get prioritized todo/action list.
     *
     * @return Todo list with urgent, important, and normal items
     */
    @GetMapping("/todos")
    public ResponseEntity<MobileTodoDTO> getTodoList() {
        LOG.debug("REST request to get mobile todo list");

        MobileTodoDTO todos = todoService.getTodoList();
        return ResponseEntity.ok(todos);
    }

    // =========================================================================
    // PHASE 2: ANALYTICS ENDPOINTS
    // =========================================================================

    /**
     * GET /api/mobile/performance
     * Get performance analytics data.
     *
     * @param period WEEK, MONTH, or YEAR
     * @param date Reference date (defaults to today)
     * @return Performance data with charts and breakdowns
     */
    @GetMapping("/performance")
    public ResponseEntity<MobilePerformanceDTO> getPerformance(
        @RequestParam(defaultValue = "WEEK") String period,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        LocalDate referenceDate = date != null ? date : LocalDate.now();
        LOG.debug("REST request to get mobile performance for period: {} from date: {}", period, referenceDate);

        try {
            MobilePerformanceDTO performance = performanceService.getPerformance(period, referenceDate);
            return ResponseEntity.ok(performance);
        } catch (IllegalArgumentException e) {
            LOG.warn("Invalid period parameter: {}", period);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * GET /api/mobile/ca-trend
     * Get CA trend for a date range.
     *
     * @param startDate Start date
     * @param endDate End date
     * @return List of daily CA summaries
     */
    @GetMapping("/ca-trend")
    public ResponseEntity<List<MobileDashboardDTO.DailyCASummaryDTO>> getCATrend(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        LOG.debug("REST request to get CA trend from {} to {}", startDate, endDate);

        // Get dashboard for end date which includes the trend
        MobileDashboardDTO dashboard = dashboardService.getDashboard(endDate);

        // Filter trend to requested date range
        List<MobileDashboardDTO.DailyCASummaryDTO> trend = dashboard
            .caTrend()
            .stream()
            .filter(d -> !d.date().isBefore(startDate) && !d.date().isAfter(endDate))
            .toList();

        return ResponseEntity.ok(trend);
    }

    // =========================================================================
    // UTILITY ENDPOINTS
    // =========================================================================

    /**
     * GET /api/mobile/health
     * Health check endpoint for mobile app connectivity testing.
     *
     * @return Simple health status
     */
    @GetMapping("/health")
    public ResponseEntity<HealthResponse> healthCheck() {
        return ResponseEntity.ok(new HealthResponse("OK", System.currentTimeMillis()));
    }

    /**
     * Health check response.
     */
    public record HealthResponse(String status, long timestamp) {}
}
