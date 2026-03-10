package com.kobe.warehouse.web.rest.mobile;

import com.kobe.warehouse.domain.StockValuationView;
import com.kobe.warehouse.domain.enumeration.CategorieABC;
import com.kobe.warehouse.domain.enumeration.ClassePareto;
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
import com.kobe.warehouse.service.dto.report.ABCParetoDTO;
import com.kobe.warehouse.service.dto.report.ABCParetoSummaryDTO;
import com.kobe.warehouse.service.dto.report.MargeDTO;
import com.kobe.warehouse.service.dto.report.MargeSummaryDTO;
import com.kobe.warehouse.service.dto.report.StockRotationDTO;
import com.kobe.warehouse.service.dto.report.StockValuationSummaryDTO;
import com.kobe.warehouse.service.dto.report.SupplierPerformanceDTO;
import com.kobe.warehouse.service.dto.report.SupplierPerformanceSummaryDTO;
import com.kobe.warehouse.service.dto.report.TiersPayantCreancesSummaryDTO;
import com.kobe.warehouse.service.dto.report.TiersPayantInvoiceDTO;
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
import com.kobe.warehouse.service.report.ABCParetoReportService;
import com.kobe.warehouse.service.report.MargeReportService;
import com.kobe.warehouse.service.report.StockRotationReportService;
import com.kobe.warehouse.service.report.StockValuationReportService;
import com.kobe.warehouse.service.report.SupplierPerformanceReportService;
import com.kobe.warehouse.service.report.RecapProduitVenduService;
import com.kobe.warehouse.service.report.TiersPayantReportService;
import com.kobe.warehouse.service.stock.dto.ProduitSearch;
import com.kobe.warehouse.service.stock.dto.RecapProduitVendu;
import com.kobe.warehouse.service.stock.dto.RecapProduitVenduRequestParam;
import com.kobe.warehouse.service.stock.dto.RecapProduitVenduSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Objects.isNull;

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

    private final RecapProduitVenduService recapProduitVenduService;

    // Phase 4: Statistical Reports Services
    private final TiersPayantReportService tiersPayantReportService;
    private final SupplierPerformanceReportService supplierPerformanceReportService;
    private final StockValuationReportService stockValuationReportService;
    private final MargeReportService margeReportService;
    private final StockRotationReportService stockRotationReportService;
    private final ABCParetoReportService abcParetoReportService;

    public MobileReportResource(
        RecapProduitVenduService recapProduitVenduService,
        MobileDashboardService dashboardService,
        MobileAlertService alertService,
        MobileProductService productService,
        MobileTodoService todoService,
        MobilePerformanceService performanceService,
        MobilePharmacistDashboardService pharmacistDashboardService,
        MobileCashSummaryService cashSummaryService,
        MobileActivityReportService activityReportService,
        MobileCashBalanceService cashBalanceService,
        MobileTvaReportService tvaReportService,
        TiersPayantReportService tiersPayantReportService,
        SupplierPerformanceReportService supplierPerformanceReportService,
        StockValuationReportService stockValuationReportService,
        MargeReportService margeReportService,
        StockRotationReportService stockRotationReportService,
        ABCParetoReportService abcParetoReportService
    ) {
        this.recapProduitVenduService = recapProduitVenduService;
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
        this.tiersPayantReportService = tiersPayantReportService;
        this.supplierPerformanceReportService = supplierPerformanceReportService;
        this.stockValuationReportService = stockValuationReportService;
        this.margeReportService = margeReportService;
        this.stockRotationReportService = stockRotationReportService;
        this.abcParetoReportService = abcParetoReportService;
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
     * @param page  Page number (0-indexed, default 0)
     * @param size  Page size (default 20)
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
     * @param q     Search query
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
            todoService::getTodoItemsCount,
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
     * @param date   Reference date (defaults to today)
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
     * @param endDate   End date
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
     * @param toDate   End date of the period (defaults to fromDate if not specified)
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
     * @param fromDate  Start date of the period
     * @param toDate    End date of the period (defaults to fromDate if not specified)
     * @param fromTime  Start time for intra-day filtering (optional)
     * @param toTime    End time for intra-day filtering (optional)
     * @param userIds   Filter by specific user IDs (optional)
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
     * @param toDate   End date of the period (defaults to fromDate if not specified)
     * @return Complete activity report data
     */
    @GetMapping("/reports/activity")
    public ResponseEntity<MobileActivityReportDTO> getActivityReport(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        LocalDate endDate = toDate != null ? toDate : fromDate;
        LOG.debug("REST request to get activity report from {} to {}", fromDate, endDate);


        return ResponseEntity.ok(activityReportService.getActivityReport(fromDate, endDate));
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
     * @param toDate   End date of the period (defaults to fromDate if not specified)
     * @return Complete cash balance data
     */
    @GetMapping("/reports/cash-balance")
    public ResponseEntity<MobileCashBalanceDTO> getCashBalance(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        LocalDate endDate = toDate != null ? toDate : fromDate;
        LOG.debug("REST request to get cash balance from {} to {}", fromDate, endDate);


        return ResponseEntity.ok(cashBalanceService.getCashBalance(fromDate, endDate));
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
     * @param fromDate    Start date of the period
     * @param toDate      End date of the period (defaults to fromDate if not specified)
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


        return ResponseEntity.ok(tvaReportService.getTvaReport(fromDate, endDate, groupByDate));
    }

    // =========================================================================
    // PHASE 4: STATISTICAL REPORTS ENDPOINTS
    // =========================================================================

    // -------------------------------------------------------------------------
    // Créances Tiers Payant (Third-Party Payer Receivables)
    // -------------------------------------------------------------------------

    /**
     * GET /api/mobile/reports/tiers-payant/creances/summary
     * Get créances summary grouped by tiers payant.
     *
     * @return List of créances summaries
     */
    @GetMapping("/reports/tiers-payant/creances/summary")
    public ResponseEntity<List<TiersPayantCreancesSummaryDTO>> getCreancesSummary() {
        LOG.debug("REST request to get créances summary");
        return ResponseEntity.ok(tiersPayantReportService.getCreancesSummary());
    }

    /**
     * GET /api/mobile/reports/tiers-payant/creances/unpaid
     * Get unpaid invoices with optional filters.
     *
     * @param groupeId    Optional groupe tiers payant ID filter
     * @param ageCategory Optional age category filter (LESS_THAN_30, BETWEEN_30_60, BETWEEN_60_90, MORE_THAN_90)
     * @return List of unpaid invoices
     */
    @GetMapping("/reports/tiers-payant/creances/unpaid")
    public ResponseEntity<List<TiersPayantInvoiceDTO>> getUnpaidInvoices(
        @RequestParam(required = false) Integer groupeId,
        @RequestParam(required = false) TiersPayantInvoiceDTO.AgeCategory ageCategory
    ) {
        LOG.debug("REST request to get unpaid invoices - groupeId: {}, ageCategory: {}", groupeId, ageCategory);
        return ResponseEntity.ok(tiersPayantReportService.getUnpaidInvoices(groupeId, ageCategory));
    }

    /**
     * GET /api/mobile/reports/tiers-payant/payment-history
     * Get payment history for a specific groupe tiers payant.
     *
     * @param groupeId  Groupe tiers payant ID
     * @param startDate Start date
     * @param endDate   End date
     * @return List of paid invoices
     */
    @GetMapping("/reports/tiers-payant/payment-history")
    public ResponseEntity<List<TiersPayantInvoiceDTO>> getPaymentHistory(
        @RequestParam Integer groupeId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        LOG.debug("REST request to get payment history - groupeId: {}, from {} to {}", groupeId, startDate, endDate);
        return ResponseEntity.ok(tiersPayantReportService.getPaymentHistory(groupeId, startDate, endDate));
    }

    // -------------------------------------------------------------------------
    // Performance Fournisseurs (Supplier Performance)
    // -------------------------------------------------------------------------

    /**
     * GET /api/mobile/reports/supplier-performance/all
     * Get all supplier performance data.
     *
     * @return List of all suppliers with performance metrics
     */
    @GetMapping("/reports/supplier-performance/all")
    public ResponseEntity<List<SupplierPerformanceDTO>> getAllSupplierPerformance() {
        LOG.debug("REST request to get all supplier performance");
        return ResponseEntity.ok(supplierPerformanceReportService.getAllSupplierPerformance());
    }

    /**
     * GET /api/mobile/reports/supplier-performance/top
     * Get top suppliers by purchase volume.
     *
     * @param limit Max number of suppliers (default 10)
     * @return List of top suppliers
     */
    @GetMapping("/reports/supplier-performance/top")
    public ResponseEntity<List<SupplierPerformanceDTO>> getTopSuppliers(
        @RequestParam(defaultValue = "10") Integer limit
    ) {
        LOG.debug("REST request to get top {} suppliers", limit);
        return ResponseEntity.ok(supplierPerformanceReportService.getTopSuppliersByVolume(limit));
    }

    /**
     * GET /api/mobile/reports/supplier-performance/summary
     * Get aggregated supplier performance summary.
     *
     * @return Supplier performance summary with aggregate metrics
     */
    @GetMapping("/reports/supplier-performance/summary")
    public ResponseEntity<SupplierPerformanceSummaryDTO> getSupplierPerformanceSummary() {
        LOG.debug("REST request to get supplier performance summary");
        return ResponseEntity.ok(supplierPerformanceReportService.getSupplierPerformanceSummary());
    }

    /**
     * GET /api/mobile/reports/supplier-performance/by-score
     * Get suppliers filtered by minimum performance score.
     *
     * @param minScore Minimum performance score (0-100)
     * @return List of suppliers with score >= minScore
     */
    @GetMapping("/reports/supplier-performance/by-score")
    public ResponseEntity<List<SupplierPerformanceDTO>> getSuppliersByScore(
        @RequestParam Double minScore
    ) {
        LOG.debug("REST request to get suppliers with score >= {}", minScore);
        return ResponseEntity.ok(supplierPerformanceReportService.getSuppliersByPerformanceScore(minScore));
    }

    /**
     * GET /api/mobile/reports/supplier-performance/issues
     * Get suppliers with delivery issues.
     *
     * @return List of suppliers with delivery problems
     */
    @GetMapping("/reports/supplier-performance/issues")
    public ResponseEntity<List<SupplierPerformanceDTO>> getSuppliersWithIssues() {
        LOG.debug("REST request to get suppliers with delivery issues");
        return ResponseEntity.ok(supplierPerformanceReportService.getSuppliersWithDeliveryIssues());
    }

    // -------------------------------------------------------------------------
    // Valorisation Stock (Stock Valuation)
    // -------------------------------------------------------------------------

    /**
     * GET /api/mobile/reports/stock-valuation
     * Get all stock valuation data with pagination.
     *
     * @return List of products with stock valuation and pagination headers
     */
    @GetMapping("/reports/stock-valuation")
    public ResponseEntity<List<StockValuationView>> getAllStockValuation(Pageable pageable,
                                                                         @RequestParam(value = "familleProduitId", required = false) Integer familleProduitId,
                                                                         @RequestParam(value = "rayonId", required = false) Integer rayonId) {

        Page<StockValuationView> page = stockValuationReportService.getStockValuationPaginated(familleProduitId, rayonId, pageable);
        return PaginationHelper.createPaginatedResponse(
            page::getContent,
            page::getTotalElements,
            pageable.getPageNumber(),
            pageable.getPageSize()
        );
    }

    /**
     * GET /api/mobile/reports/stock-valuation/summary
     * Get aggregated stock valuation summary.
     *
     * @return Stock valuation summary with totals
     */
    @GetMapping("/reports/stock-valuation/summary")
    public ResponseEntity<StockValuationSummaryDTO> getStockValuationSummary(@RequestParam(value = "familleProduitId", required = false) Integer familleProduitId,
                                                                             @RequestParam(value = "rayonId", required = false) Integer rayonId) {
        LOG.debug("REST request to get stock valuation summary");
        if (isNull(familleProduitId) && isNull(rayonId)) {
            return ResponseEntity.ok(stockValuationReportService.getStockValuationSummary());
        } else {
            return ResponseEntity.ok(stockValuationReportService.getStockValuationSummary(familleProduitId, rayonId));
        }

    }


    // -------------------------------------------------------------------------
    // Rentabilité / Marges (basé sur mv_marge_produit — sans BCG)
    // -------------------------------------------------------------------------

    /**
     * GET /api/mobile/reports/profitability/all
     * Liste paginée des marges produit avec filtres optionnels famille et recherche.
     *
     * @param familleProduitId filtre optionnel sur la famille produit
     * @param search           filtre textuel sur libellé ou code CIP
     * @param pageable         pagination + tri (défaut : marge_brute DESC, 20 par page)
     */
    @GetMapping("/reports/profitability/all")
    public ResponseEntity<List<MargeDTO>> getAllProductProfitability(
        @RequestParam(required = false) Integer familleProduitId,
        @RequestParam(required = false) String search,
        Pageable pageable
    ) {
        LOG.debug("REST request to get marges — famille: {}, search: {}", familleProduitId, search);
        Page<MargeDTO> page = margeReportService.getMarges(familleProduitId, search, pageable);
        return PaginationHelper.createPaginatedResponse(
            page::getContent,
            page::getTotalElements,
            pageable.getPageNumber(),
            pageable.getPageSize()
        );
    }

    /**
     * GET /api/mobile/reports/profitability/summary
     * Résumé global des marges avec seuils configurables, sans distribution BCG.
     *
     * @param familleProduitId filtre optionnel famille produit
     * @param seuilBas         seuil bas en % (défaut 10)
     * @param seuilHaut        seuil haut en % (défaut 20)
     */
    @GetMapping("/reports/profitability/summary")
    public ResponseEntity<MargeSummaryDTO> getProfitabilitySummary(
        @RequestParam(required = false) Integer familleProduitId,
        @RequestParam(defaultValue = "10") int seuilBas,
        @RequestParam(defaultValue = "20") int seuilHaut
    ) {
        LOG.debug("REST request to get marge summary — famille: {}, bas: {}%, haut: {}%", familleProduitId, seuilBas, seuilHaut);
        return ResponseEntity.ok(margeReportService.getMargeSummary(familleProduitId, seuilBas, seuilHaut));
    }

    /**
     * GET /api/mobile/reports/profitability/top
     * Top N produits par marge brute décroissante, paginé.
     *
     * @param limit    nombre de produits dans le top (défaut 20)
     * @param pageable pagination à l'intérieur du top
     */
    @GetMapping("/reports/profitability/top")
    public ResponseEntity<List<MargeDTO>> getTopProfitableProducts(
        @RequestParam(defaultValue = "20") int limit,
        Pageable pageable
    ) {
        LOG.debug("REST request to get top {} profitable products", limit);
        Page<MargeDTO> page = margeReportService.getTopProduitsParMarge(limit, pageable);
        return PaginationHelper.createPaginatedResponse(
            page::getContent,
            page::getTotalElements,
            pageable.getPageNumber(),
            pageable.getPageSize()
        );
    }

    /**
     * GET /api/mobile/reports/profitability/low-margin
     * Produits dont le taux de marge est inférieur au seuil, paginés.
     *
     * @param seuil    seuil en % (défaut 10)
     * @param pageable pagination
     */
    @GetMapping("/reports/profitability/low-margin")
    public ResponseEntity<List<MargeDTO>> getLowMarginProducts(
        @RequestParam(defaultValue = "10") int seuil,
        Pageable pageable
    ) {
        LOG.debug("REST request to get low margin products (< {}%)", seuil);
        Page<MargeDTO> page = margeReportService.getProduitsMargeInsuffisante(seuil, pageable);
        return PaginationHelper.createPaginatedResponse(
            page::getContent,
            page::getTotalElements,
            pageable.getPageNumber(),
            pageable.getPageSize()
        );
    }

    // -------------------------------------------------------------------------
    // Rotation Stock (Stock Rotation)
    // -------------------------------------------------------------------------

    /**
     * GET /api/mobile/reports/stock-rotation/all
     * Get all stock rotation data with pagination.
     *
     * @param page Page number (0-indexed, default 0)
     * @param size Page size (default 50)
     * @return List of products with rotation metrics and pagination headers
     */
    @GetMapping("/reports/stock-rotation/all")
    public ResponseEntity<List<StockRotationDTO>> getAllStockRotation(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "50") int size
    ) {
        LOG.debug("REST request to get all stock rotation - page: {}, size: {}", page, size);
        return PaginationHelper.createPaginatedResponse(
            () -> stockRotationReportService.getStockRotationPaginated(page, size),
            stockRotationReportService::getStockRotationCount,
            page,
            size
        );
    }

    /**
     * GET /api/mobile/reports/stock-rotation/slow-moving
     * Get slow moving products (ABC category C).
     *
     * @return List of slow moving products
     */
    @GetMapping("/reports/stock-rotation/slow-moving")
    public ResponseEntity<List<StockRotationDTO>> getSlowMovingProducts() {
        LOG.debug("REST request to get slow moving products");
        return ResponseEntity.ok(stockRotationReportService.getSlowMovingProducts());
    }

    /**
     * GET /api/mobile/reports/stock-rotation/abc-counts
     * Get product counts by ABC classification.
     *
     * @return Map of ABC category to count
     */
    @GetMapping("/reports/stock-rotation/abc-counts")
    public ResponseEntity<Map<CategorieABC, Long>> getStockRotationABCCounts() {
        LOG.debug("REST request to get stock rotation ABC counts");
        return ResponseEntity.ok(stockRotationReportService.getStockRotationCountByABCClassification());
    }

    /**
     * GET /api/mobile/reports/stock-rotation/by-abc
     * Get products filtered by ABC classification with pagination.
     *
     * @param category ABC category (A, B, C)
     * @param page     Page number (0-indexed, default 0)
     * @param size     Page size (default 50)
     * @return List of products in ABC category with pagination headers
     */
    @GetMapping("/reports/stock-rotation/by-abc")
    public ResponseEntity<List<StockRotationDTO>> getStockRotationByABC(
        @RequestParam CategorieABC category,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "50") int size
    ) {
        LOG.debug("REST request to get stock rotation by ABC category: {} - page: {}, size: {}", category, page, size);
        return PaginationHelper.createPaginatedResponse(
            () -> stockRotationReportService.getStockRotationByABCPaginated(category, page, size),
            () -> stockRotationReportService.getStockRotationCountByABC(category),
            page,
            size
        );
    }

    // -------------------------------------------------------------------------
    // ABC Pareto Analysis
    // -------------------------------------------------------------------------

    /**
     * GET /api/mobile/reports/abc-pareto/all
     * Get all ABC Pareto analysis data with pagination.
     *
     * @param page Page number (0-indexed, default 0)
     * @param size Page size (default 50)
     * @return List of products with Pareto analysis and pagination headers
     */
    @GetMapping("/reports/abc-pareto/all")
    public ResponseEntity<List<ABCParetoDTO>> getAllABCPareto(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "50") int size
    ) {
        LOG.debug("REST request to get all ABC Pareto analysis - page: {}, size: {}", page, size);
        return PaginationHelper.createPaginatedResponse(
            () -> abcParetoReportService.getABCParetoPaginated(page, size),
            abcParetoReportService::getABCParetoCount,
            page,
            size
        );
    }

    /**
     * GET /api/mobile/reports/abc-pareto/summary
     * Get ABC Pareto summary.
     *
     * @return ABC Pareto summary with class distribution
     */
    @GetMapping("/reports/abc-pareto/summary")
    public ResponseEntity<ABCParetoSummaryDTO> getABCParetoSummary() {
        LOG.debug("REST request to get ABC Pareto summary");
        return ResponseEntity.ok(abcParetoReportService.getABCParetoSummary());
    }

    /**
     * GET /api/mobile/reports/abc-pareto/by-class
     * Get products filtered by Pareto class with pagination.
     *
     * @param classePareto Pareto class (A, B, C)
     * @param page         Page number (0-indexed, default 0)
     * @param size         Page size (default 50)
     * @return List of products in Pareto class with pagination headers
     */
    @GetMapping("/reports/abc-pareto/by-class")
    public ResponseEntity<List<ABCParetoDTO>> getByParetoClass(
        @RequestParam ClassePareto classePareto,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "50") int size
    ) {
        LOG.debug("REST request to get ABC Pareto by class: {} - page: {}, size: {}", classePareto, page, size);
        return PaginationHelper.createPaginatedResponse(
            () -> abcParetoReportService.getABCParetoByClassPaginated(classePareto, page, size),
            () -> abcParetoReportService.getABCParetoCountByClass(classePareto),
            page,
            size
        );
    }

    /**
     * GET /api/mobile/reports/abc-pareto/top
     * Get top N revenue contributors.
     *
     * @param limit Number of products to return (default 20)
     * @return List of top revenue contributors
     */
    @GetMapping("/reports/abc-pareto/top")
    public ResponseEntity<List<ABCParetoDTO>> getTopRevenueContributors(
        @RequestParam(defaultValue = "20") int limit
    ) {
        LOG.debug("REST request to get top {} revenue contributors", limit);
        return ResponseEntity.ok(abcParetoReportService.getTopRevenueContributors(limit));
    }

    // -------------------------------------------------------------------------
    // Récap Produits Vendus (Sold Products)
    // -------------------------------------------------------------------------

    /**
     * GET /api/mobile/reports/sold-products
     * Get sold products with search and date filter, paginated.
     *
     * @param startDate Start date (required)
     * @param endDate   End date (defaults to startDate)
     * @param search    Optional search term (libelle or code)
     * @param pageable  Pagination
     */
    @GetMapping("/reports/sold-products")
    public ResponseEntity<List<RecapProduitVendu>> getSoldProducts(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        @RequestParam(required = false) String search,
        Pageable pageable
    ) {
        LocalDate end = endDate != null ? endDate : startDate;
        LOG.debug("REST request to get sold products from {} to {}, search: {}", startDate, end, search);
        var params = new RecapProduitVenduRequestParam(
            startDate, end, null, null, null, search,
            null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, false
        );
        Page<RecapProduitVendu> page = recapProduitVenduService.getRecapProduitVenduReport(params, pageable);
        return PaginationHelper.createPaginatedResponse(
            page::getContent, page::getTotalElements,
            pageable.getPageNumber(), pageable.getPageSize()
        );
    }

    /**
     * GET /api/mobile/reports/sold-products/summary
     * Get aggregated summary of sold products for the given period.
     *
     * @param startDate Start date (required)
     * @param endDate   End date (defaults to startDate)
     * @param search    Optional search term
     */
    @GetMapping("/reports/sold-products/summary")
    public ResponseEntity<RecapProduitVenduSummary> getSoldProductsSummary(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        @RequestParam(required = false) String search
    ) {
        LocalDate end = endDate != null ? endDate : startDate;
        LOG.debug("REST request to get sold products summary from {} to {}", startDate, end);
        var params = new RecapProduitVenduRequestParam(
            startDate, end, null, null, null, search,
            null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, false
        );
        return ResponseEntity.ok(recapProduitVenduService.getRecapProduitVenduSummary(params));
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
    public record HealthResponse(String status, long timestamp) {
    }
}
