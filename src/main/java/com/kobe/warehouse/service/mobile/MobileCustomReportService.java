package com.kobe.warehouse.service.mobile;

import com.kobe.warehouse.repository.MobileSalesRepository;
import com.kobe.warehouse.repository.MobileSalesRepository.DailyCATrendProjection;
import com.kobe.warehouse.repository.MobileSalesRepository.PaymentMethodProjection;
import com.kobe.warehouse.repository.MobileSalesRepository.SalesSummaryProjection;
import com.kobe.warehouse.repository.MobileSalesRepository.TopProductProjection;
import com.kobe.warehouse.service.dto.mobile.CustomReportMetricDTO;
import com.kobe.warehouse.service.dto.mobile.CustomReportMetricDTO.ChartDataPointDTO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for generating custom report metrics (Phase 4).
 * Uses MobileSalesRepository for data access.
 */
@Service
@Transactional(readOnly = true)
public class MobileCustomReportService {

    private static final Logger LOG = LoggerFactory.getLogger(MobileCustomReportService.class);
    private static final int TOP_PRODUCTS_LIMIT = 10;

    private final MobileSalesRepository salesRepository;

    public MobileCustomReportService(MobileSalesRepository salesRepository) {
        this.salesRepository = salesRepository;
    }

    /**
     * Available metric codes
     */
    public enum MetricCode {
        CA,
        TRANSACTIONS,
        AVERAGE_BASKET,
        MARGIN,
        TOP_PRODUCTS,
        PAYMENT_METHODS,
        SALES_BY_CATEGORY,
        CUSTOMER_STATS,
        ALERTS,
        STOCK_STATUS
    }

    /**
     * Generate metrics for a custom report.
     *
     * @param metricCodes List of metric codes to generate
     * @param startDate Period start date
     * @param endDate Period end date
     * @return Map of metric code to metric data
     */
    public Map<String, CustomReportMetricDTO> generateMetrics(
        List<String> metricCodes,
        LocalDate startDate,
        LocalDate endDate
    ) {
        LOG.debug("Generating custom report metrics: {} for period {} to {}", metricCodes, startDate, endDate);

        Map<String, CustomReportMetricDTO> metrics = new HashMap<>();

        LocalDate previousStartDate = startDate.minusDays(endDate.toEpochDay() - startDate.toEpochDay() + 1);
        LocalDate previousEndDate = startDate.minusDays(1);

        for (String code : metricCodes) {
            try {
                MetricCode metricCode = MetricCode.valueOf(code);
                CustomReportMetricDTO metric = generateMetric(metricCode, startDate, endDate, previousStartDate, previousEndDate);
                metrics.put(code, metric);
            } catch (IllegalArgumentException e) {
                LOG.warn("Unknown metric code: {}", code);
            }
        }

        return metrics;
    }

    /**
     * Generate a single metric
     */
    private CustomReportMetricDTO generateMetric(
        MetricCode metricCode,
        LocalDate startDate,
        LocalDate endDate,
        LocalDate previousStartDate,
        LocalDate previousEndDate
    ) {
        return switch (metricCode) {
            case CA -> generateCAMetric(startDate, endDate, previousStartDate, previousEndDate);
            case TRANSACTIONS -> generateTransactionsMetric(startDate, endDate, previousStartDate, previousEndDate);
            case AVERAGE_BASKET -> generateAverageBasketMetric(startDate, endDate, previousStartDate, previousEndDate);
            case MARGIN -> generateMarginMetric(startDate, endDate, previousStartDate, previousEndDate);
            case TOP_PRODUCTS -> generateTopProductsMetric(startDate, endDate);
            case PAYMENT_METHODS -> generatePaymentMethodsMetric(startDate, endDate);
            case SALES_BY_CATEGORY -> generateSalesByCategoryMetric(startDate, endDate);
            case CUSTOMER_STATS -> generateCustomerStatsMetric(startDate, endDate);
            case ALERTS -> generateAlertsMetric();
            case STOCK_STATUS -> generateStockStatusMetric();
        };
    }

    /**
     * Generate CA (Revenue) metric
     */
    private CustomReportMetricDTO generateCAMetric(
        LocalDate startDate,
        LocalDate endDate,
        LocalDate previousStartDate,
        LocalDate previousEndDate
    ) {
        SalesSummaryProjection current = salesRepository.getSalesSummary(startDate, endDate);
        SalesSummaryProjection previous = salesRepository.getSalesSummary(previousStartDate, previousEndDate);
        double trend = calculateTrend(current.caTotal(), previous.caTotal());

        List<ChartDataPointDTO> chartData = mapDailyCAChart(salesRepository.getCATrend(startDate, endDate));

        return new CustomReportMetricDTO(
            "CA",
            "Chiffre d'affaires",
            formatAmount(current.caTotal()),
            trend,
            chartData,
            String.format("Période: %s - %s", startDate, endDate)
        );
    }

    /**
     * Generate Transactions metric
     */
    private CustomReportMetricDTO generateTransactionsMetric(
        LocalDate startDate,
        LocalDate endDate,
        LocalDate previousStartDate,
        LocalDate previousEndDate
    ) {
        SalesSummaryProjection current = salesRepository.getSalesSummary(startDate, endDate);
        SalesSummaryProjection previous = salesRepository.getSalesSummary(previousStartDate, previousEndDate);
        double trend = calculateTrend(current.transactionsCount(), previous.transactionsCount());

        return new CustomReportMetricDTO(
            "TRANSACTIONS",
            "Nombre de ventes",
            String.valueOf(current.transactionsCount()),
            trend,
            null,
            "Transactions enregistrées"
        );
    }

    /**
     * Generate Average Basket metric
     */
    private CustomReportMetricDTO generateAverageBasketMetric(
        LocalDate startDate,
        LocalDate endDate,
        LocalDate previousStartDate,
        LocalDate previousEndDate
    ) {
        SalesSummaryProjection current = salesRepository.getSalesSummary(startDate, endDate);
        SalesSummaryProjection previous = salesRepository.getSalesSummary(previousStartDate, previousEndDate);

        long currentAvg = current.transactionsCount() > 0 ? current.caTotal() / current.transactionsCount() : 0;
        long previousAvg = previous.transactionsCount() > 0 ? previous.caTotal() / previous.transactionsCount() : 0;

        double trend = calculateTrend(currentAvg, previousAvg);

        return new CustomReportMetricDTO(
            "AVERAGE_BASKET",
            "Panier moyen",
            formatAmount(currentAvg),
            trend,
            null,
            "Valeur moyenne par transaction"
        );
    }

    /**
     * Generate Margin metric
     */
    private CustomReportMetricDTO generateMarginMetric(
        LocalDate startDate,
        LocalDate endDate,
        LocalDate previousStartDate,
        LocalDate previousEndDate
    ) {
        SalesSummaryProjection current = salesRepository.getSalesSummary(startDate, endDate);
        SalesSummaryProjection previous = salesRepository.getSalesSummary(previousStartDate, previousEndDate);

        double currentMarginPercent = current.caTotal() > 0
            ? (current.marginAmount() * 100.0 / current.caTotal())
            : 0;

        double trend = calculateTrend(current.marginAmount(), previous.marginAmount());

        return new CustomReportMetricDTO(
            "MARGIN",
            "Marge brute",
            formatAmount(current.marginAmount()),
            trend,
            null,
            String.format("%.1f%% du CA", currentMarginPercent)
        );
    }

    /**
     * Generate Top Products metric
     */
    private CustomReportMetricDTO generateTopProductsMetric(LocalDate startDate, LocalDate endDate) {
        List<TopProductProjection> projections = salesRepository.getTopProducts(startDate, endDate, TOP_PRODUCTS_LIMIT);
        List<ChartDataPointDTO> topProducts = projections.stream()
            .map(p -> new ChartDataPointDTO(p.productName(), (double) p.salesAmount(), null))
            .toList();

        StringBuilder details = new StringBuilder();
        int count = Math.min(5, topProducts.size());
        for (int i = 0; i < count; i++) {
            ChartDataPointDTO product = topProducts.get(i);
            details.append(String.format("%s: %s FCFA", product.label(), formatNumber(product.value())));
            if (i < count - 1) details.append("\n");
        }

        return new CustomReportMetricDTO(
            "TOP_PRODUCTS",
            "Top produits",
            String.valueOf(topProducts.size()),
            null,
            topProducts,
            details.toString()
        );
    }

    /**
     * Generate Payment Methods metric
     */
    private CustomReportMetricDTO generatePaymentMethodsMetric(LocalDate startDate, LocalDate endDate) {
        List<PaymentMethodProjection> projections = salesRepository.getPaymentMethodsSummary(startDate, endDate);
        List<ChartDataPointDTO> paymentMethods = projections.stream()
            .map(p -> new ChartDataPointDTO(p.libelle(), (double) p.amount(), null))
            .toList();

        return new CustomReportMetricDTO(
            "PAYMENT_METHODS",
            "Modes de paiement",
            String.valueOf(paymentMethods.size()),
            null,
            paymentMethods,
            "Répartition des paiements"
        );
    }

    /**
     * Generate Sales by Category metric
     */
    private CustomReportMetricDTO generateSalesByCategoryMetric(LocalDate startDate, LocalDate endDate) {
        // This would query by product categories
        // Simplified version - can be implemented later
        return new CustomReportMetricDTO(
            "SALES_BY_CATEGORY",
            "Ventes par catégorie",
            "0",
            null,
            List.of(),
            "Catégories actives"
        );
    }

    /**
     * Generate Customer Stats metric
     */
    private CustomReportMetricDTO generateCustomerStatsMetric(LocalDate startDate, LocalDate endDate) {
        int customersCount = salesRepository.getCustomersCount(startDate, endDate);

        return new CustomReportMetricDTO(
            "CUSTOMER_STATS",
            "Statistiques clients",
            String.valueOf(customersCount),
            null,
            null,
            "Clients uniques"
        );
    }

    /**
     * Generate Alerts metric
     */
    private CustomReportMetricDTO generateAlertsMetric() {
        return new CustomReportMetricDTO(
            "ALERTS",
            "Alertes actives",
            "0",
            null,
            null,
            "Aucune alerte critique"
        );
    }

    /**
     * Generate Stock Status metric
     */
    private CustomReportMetricDTO generateStockStatusMetric() {
        return new CustomReportMetricDTO(
            "STOCK_STATUS",
            "État du stock",
            "N/A",
            null,
            null,
            "Statut du stock"
        );
    }

    /**
     * Map daily CA trend to chart data points.
     */
    private List<ChartDataPointDTO> mapDailyCAChart(List<DailyCATrendProjection> projections) {
        return projections.stream()
            .map(p -> new ChartDataPointDTO(p.date().toString(), (double) p.caTotal(), null))
            .toList();
    }

    private double calculateTrend(long current, long previous) {
        if (previous == 0) {
            return current > 0 ? 100.0 : 0.0;
        }
        return BigDecimal.valueOf(((current - previous) * 100.0) / previous)
            .setScale(2, RoundingMode.HALF_UP)
            .doubleValue();
    }

    private String formatAmount(long amount) {
        return String.format("%,d FCFA", amount);
    }

    private String formatNumber(double number) {
        return String.format("%,.0f", number);
    }
}
