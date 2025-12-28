package com.kobe.warehouse.web.rest.mobile;

import com.kobe.warehouse.service.dto.mobile.MobileActivityReportDTO;
import com.kobe.warehouse.service.dto.mobile.MobileAlertDetailDTO;
import com.kobe.warehouse.service.dto.mobile.MobileCashBalanceDTO;
import com.kobe.warehouse.service.dto.mobile.MobileCashSummaryDTO;
import com.kobe.warehouse.service.dto.mobile.MobileDashboardDTO;
import com.kobe.warehouse.service.dto.mobile.MobilePerformanceDTO;
import com.kobe.warehouse.service.dto.mobile.MobilePharmacistDashboardDTO;
import com.kobe.warehouse.service.dto.mobile.MobileProductQuickInfoDTO;
import com.kobe.warehouse.service.dto.mobile.MobileTodoDTO;
import com.kobe.warehouse.service.dto.mobile.MobileTvaReportDTO;
import com.kobe.warehouse.service.mobile.MobileActivityReportService;
import com.kobe.warehouse.service.mobile.MobileAlertService;
import com.kobe.warehouse.service.mobile.MobileCashBalanceService;
import com.kobe.warehouse.service.mobile.MobileCashSummaryService;
import com.kobe.warehouse.service.mobile.MobileDashboardService;
import com.kobe.warehouse.service.mobile.MobilePerformanceService;
import com.kobe.warehouse.service.mobile.MobilePharmacistDashboardService;
import com.kobe.warehouse.service.mobile.MobileProductService;
import com.kobe.warehouse.service.mobile.MobileTodoService;
import com.kobe.warehouse.service.mobile.MobileTvaReportService;
import com.kobe.warehouse.service.mobile.util.PaginationHelper;
import com.kobe.warehouse.service.stock.dto.ProduitSearch;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
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
    private final MobilePharmacistDashboardService pharmacistDashboardService;
    private final MobileCashSummaryService cashSummaryService;
    private final MobileActivityReportService activityReportService;
    private final MobileCashBalanceService cashBalanceService;
    private final MobileTvaReportService tvaReportService;

    public MobileReportResource(
        MobileDashboardService dashboardService,
        MobileAlertService alertService,
        MobileProductService productService,
        MobileTodoService todoService,
        MobilePerformanceService performanceService,
        MobilePharmacistDashboardService pharmacistDashboardService,
        MobileCashSummaryService cashSummaryService,
        MobileActivityReportService activityReportService,
        MobileCashBalanceService cashBalanceService,
        MobileTvaReportService tvaReportService
    ) {
        this.dashboardService = dashboardService;
        this.alertService = alertService;
        this.productService = productService;
        this.todoService = todoService;
        this.performanceService = performanceService;
        this.pharmacistDashboardService = pharmacistDashboardService;
        this.cashSummaryService = cashSummaryService;
        this.activityReportService = activityReportService;
        this.cashBalanceService = cashBalanceService;
        this.tvaReportService = tvaReportService;
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
     * Get detailed list of alerts with pagination.
     *
     * @param types Optional filter by alert types (comma-separated)
     * @param page Page number (0-indexed, default 0)
     * @param size Page size (default 20)
     * @return List of detailed alerts with pagination headers
     */
    @GetMapping("/alerts")
    public ResponseEntity<List<MobileAlertDetailDTO>> getAlerts(
        @RequestParam(required = false) List<String> types,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        LOG.debug("REST request to get mobile alerts with types: {}, page: {}, size: {}", types, page, size);

        return PaginationHelper.createPaginatedResponse(
            () -> alertService.getAlerts(types, page, size),
            () -> alertService.getAlertsCount(types),
            page,
            size
        );
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

    /**
     * GET /api/mobile/todos/items
     * Get all todo items as a flat list with pagination.
     * Items are ordered by priority: URGENT first, then IMPORTANT, then NORMAL.
     *
     * @param page Page number (0-indexed, default 0)
     * @param size Page size (default 20)
     * @return List of todo items with pagination headers
     */
    @GetMapping("/todos/items")
    public ResponseEntity<List<MobileTodoDTO.TodoItemDTO>> getTodoItems(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        LOG.debug("REST request to get mobile todo items with page: {}, size: {}", page, size);

        return PaginationHelper.createPaginatedResponse(
            () -> todoService.getAllTodoItems(page, size),
            () -> todoService.getTodoItemsCount(),
            page,
            size
        );
    }

    /**
     * GET /api/mobile/todos/counts
     * Get todo counts by priority.
     *
     * @return Counts for urgent, important, and normal items
     */
    @GetMapping("/todos/counts")
    public ResponseEntity<MobileTodoService.TodoCountsDTO> getTodoCounts() {
        LOG.debug("REST request to get mobile todo counts");

        MobileTodoService.TodoCountsDTO counts = todoService.getTodoCounts();
        return ResponseEntity.ok(counts);
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
    // PHASE 3: PHARMACIST REPORTS ENDPOINTS
    // =========================================================================

    /**
     * GET /api/mobile/reports/pharmacist-dashboard
     * Get pharmacist dashboard (Tableau Pharmacien) data.
     *
     * <p>Returns comprehensive sales vs purchases analysis:
     * - Sales totals (comptant, credit, remise, net, TTC)
     * - Purchases totals by supplier
     * - Ratios (ventes/achats, achats/ventes)
     * - Margin calculation
     * - Trend charts
     *
     * @param fromDate Start date of the period
     * @param toDate End date of the period (defaults to fromDate if not specified)
     * @return Complete pharmacist dashboard data
     */
    @GetMapping("/reports/pharmacist-dashboard")
    public ResponseEntity<MobilePharmacistDashboardDTO> getPharmacistDashboard(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        LocalDate endDate = toDate != null ? toDate : fromDate;
        LOG.debug("REST request to get pharmacist dashboard from {} to {}", fromDate, endDate);

        MobilePharmacistDashboardDTO dashboard = pharmacistDashboardService.getPharmacistDashboard(fromDate, endDate);
        return ResponseEntity.ok(dashboard);
    }

    /**
     * GET /api/mobile/reports/cash-summary
     * Get cash summary (Ticket Z / Récapitulatif Caisse) data.
     *
     * <p>Returns comprehensive cash summary with:
     * - Global summary by payment mode
     * - Per-cashier breakdown
     * - Totals by payment type (cash, cards, mobile money, etc.)
     *
     * @param fromDate Start date of the period
     * @param toDate End date of the period (defaults to fromDate if not specified)
     * @param fromTime Start time for intra-day filtering (optional)
     * @param toTime End time for intra-day filtering (optional)
     * @param userIds Filter by specific user IDs (optional)
     * @param onlyVente If true, only include sales payments (default: false)
     * @return Complete cash summary data
     */
    @GetMapping("/reports/cash-summary")
    public ResponseEntity<MobileCashSummaryDTO> getCashSummary(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime fromTime,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime toTime,
        @RequestParam(required = false) Set<Integer> userIds,
        @RequestParam(defaultValue = "false") boolean onlyVente
    ) {
        LocalDate endDate = toDate != null ? toDate : fromDate;
        LOG.debug("REST request to get cash summary from {} to {}, time: {} to {}, users: {}, onlyVente: {}",
            fromDate, endDate, fromTime, toTime, userIds, onlyVente);

        MobileCashSummaryDTO cashSummary = cashSummaryService.getCashSummary(
            fromDate, endDate, fromTime, toTime, userIds, onlyVente
        );
        return ResponseEntity.ok(cashSummary);
    }

    /**
     * GET /api/mobile/reports/activity
     * Get activity report (Rapport d'Activité) data.
     *
     * <p>Returns comprehensive activity summary with:
     * - Chiffre d'affaires (revenue summary)
     * - Recettes by payment mode
     * - Mouvements de caisse
     * - Achats by supplier group
     * - Tiers payants summary
     *
     * @param fromDate Start date of the period
     * @param toDate End date of the period (defaults to fromDate if not specified)
     * @return Complete activity report data
     */
    @GetMapping("/reports/activity")
    public ResponseEntity<MobileActivityReportDTO> getActivityReport(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        LocalDate endDate = toDate != null ? toDate : fromDate;
        LOG.debug("REST request to get activity report from {} to {}", fromDate, endDate);

        MobileActivityReportDTO activityReport = activityReportService.getActivityReport(fromDate, endDate);
        return ResponseEntity.ok(activityReport);
    }

    /**
     * GET /api/mobile/reports/cash-balance
     * Get cash balance (Balance Caisse) data.
     *
     * <p>Returns comprehensive cash balance with:
     * - Totals (TTC, HT, Net, Remise, TVA)
     * - Payment breakdown (cash, cards, mobile money, etc.)
     * - Category balances (VO, VNO)
     * - Cash movements (entries/exits)
     * - Margin and ratio analysis
     *
     * @param fromDate Start date of the period
     * @param toDate End date of the period (defaults to fromDate if not specified)
     * @return Complete cash balance data
     */
    @GetMapping("/reports/cash-balance")
    public ResponseEntity<MobileCashBalanceDTO> getCashBalance(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        LocalDate endDate = toDate != null ? toDate : fromDate;
        LOG.debug("REST request to get cash balance from {} to {}", fromDate, endDate);

        MobileCashBalanceDTO cashBalance = cashBalanceService.getCashBalance(fromDate, endDate);
        return ResponseEntity.ok(cashBalance);
    }

    /**
     * GET /api/mobile/reports/tva
     * Get TVA (VAT) report data.
     *
     * <p>Returns comprehensive TVA breakdown with:
     * - Totals (HT, TVA, TTC, Net)
     * - Breakdown by TVA rate
     * - Chart data for visualization
     *
     * @param fromDate Start date of the period
     * @param toDate End date of the period (defaults to fromDate if not specified)
     * @param groupByDate Whether to group results by date (default: false)
     * @return Complete TVA report data
     */
    @GetMapping("/reports/tva")
    public ResponseEntity<MobileTvaReportDTO> getTvaReport(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
        @RequestParam(defaultValue = "false") boolean groupByDate
    ) {
        LocalDate endDate = toDate != null ? toDate : fromDate;
        LOG.debug("REST request to get TVA report from {} to {}, groupByDate: {}", fromDate, endDate, groupByDate);

        MobileTvaReportDTO tvaReport = tvaReportService.getTvaReport(fromDate, endDate, groupByDate);
        return ResponseEntity.ok(tvaReport);
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
