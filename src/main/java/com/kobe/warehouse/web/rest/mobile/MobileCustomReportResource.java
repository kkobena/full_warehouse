package com.kobe.warehouse.web.rest.mobile;

import com.kobe.warehouse.service.dto.mobile.CustomReportMetricDTO;
import com.kobe.warehouse.service.mobile.MobileCustomReportService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for mobile custom report generation.
 * Allows users to create personalized reports with selected metrics.
 */
@RestController
@RequestMapping("/api/mobile/custom-reports")
public class MobileCustomReportResource {

    private static final Logger LOG = LoggerFactory.getLogger(MobileCustomReportResource.class);

    private final MobileCustomReportService customReportService;

    public MobileCustomReportResource(MobileCustomReportService customReportService) {
        this.customReportService = customReportService;
    }

    /**
     * POST /api/mobile/custom-reports/generate
     * Generate a custom report with selected metrics.
     *
     * @param request Custom report request
     * @return Map of metric code to metric data
     */
    @PostMapping("/generate")
    public ResponseEntity<Map<String, CustomReportMetricDTO>> generateCustomReport(
        @Valid @RequestBody CustomReportRequest request
    ) {
        LOG.debug("REST request to generate custom report: {}", request);

        Map<String, CustomReportMetricDTO> metrics = customReportService.generateMetrics(
            request.metricCodes(),
            request.startDate(),
            request.endDate()
        );

        return ResponseEntity.ok(metrics);
    }

    /**
     * GET /api/mobile/custom-reports/available-metrics
     * Get list of available metrics for custom reports.
     *
     * @return List of metric codes and descriptions
     */
    @GetMapping("/available-metrics")
    public ResponseEntity<List<MetricInfo>> getAvailableMetrics() {
        LOG.debug("REST request to get available metrics");

        List<MetricInfo> metrics = List.of(
            new MetricInfo("CA", "Chiffre d'affaires", "💰", "Revenus totaux"),
            new MetricInfo("TRANSACTIONS", "Nombre de ventes", "🛒", "Total des transactions"),
            new MetricInfo("AVERAGE_BASKET", "Panier moyen", "🛍️", "Montant moyen par vente"),
            new MetricInfo("MARGIN", "Marge brute", "📊", "Bénéfice brut"),
            new MetricInfo("TOP_PRODUCTS", "Top produits", "⭐", "Produits les plus vendus"),
            new MetricInfo("PAYMENT_METHODS", "Modes de paiement", "💳", "Répartition des paiements"),
            new MetricInfo("SALES_BY_CATEGORY", "Ventes par catégorie", "📦", "Distribution par catégorie"),
            new MetricInfo("CUSTOMER_STATS", "Statistiques clients", "👥", "Métriques clients"),
            new MetricInfo("ALERTS", "Alertes", "🔔", "Alertes actives"),
            new MetricInfo("STOCK_STATUS", "État du stock", "📋", "Statut des stocks")
        );

        return ResponseEntity.ok(metrics);
    }

    /**
     * GET /api/mobile/custom-reports/periods
     * Get available report periods.
     *
     * @return List of period options
     */
    @GetMapping("/periods")
    public ResponseEntity<List<PeriodOption>> getAvailablePeriods() {
        LOG.debug("REST request to get available periods");

        List<PeriodOption> periods = List.of(
            new PeriodOption("DAY", "Jour", 1),
            new PeriodOption("WEEK", "Semaine", 7),
            new PeriodOption("MONTH", "Mois", 30),
            new PeriodOption("QUARTER", "Trimestre", 90),
            new PeriodOption("YEAR", "Année", 365)
        );

        return ResponseEntity.ok(periods);
    }

    // =========================================================================
    // REQUEST/RESPONSE RECORDS
    // =========================================================================

    /**
     * Custom report generation request
     */
    public record CustomReportRequest(
        List<String> metricCodes,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {}

    /**
     * Metric information
     */
    public record MetricInfo(
        String code,
        String displayName,
        String icon,
        String description
    ) {}

    /**
     * Period option
     */
    public record PeriodOption(
        String code,
        String displayName,
        int days
    ) {}
}
