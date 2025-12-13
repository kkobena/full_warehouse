package com.kobe.warehouse.service.mobile;

import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.service.dto.mobile.CustomReportMetricDTO;
import com.kobe.warehouse.service.dto.mobile.CustomReportMetricDTO.ChartDataPointDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for generating custom report metrics (Phase 4).
 * Provides flexible metric generation for user-created reports.
 */
@Service
@Transactional(readOnly = true)
public class MobileCustomReportService {

    private static final Logger LOG = LoggerFactory.getLogger(MobileCustomReportService.class);
    private static final String SALES_CA_TYPE = "CA";

    @PersistenceContext
    private EntityManager entityManager;

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
        long currentCA = getTotalCA(startDate, endDate);
        long previousCA = getTotalCA(previousStartDate, previousEndDate);
        double trend = calculateTrend(currentCA, previousCA);

        List<ChartDataPointDTO> chartData = getDailyCAChart(startDate, endDate);

        return new CustomReportMetricDTO(
            "CA",
            "Chiffre d'affaires",
            formatAmount(currentCA),
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
        int currentCount = getTransactionsCount(startDate, endDate);
        int previousCount = getTransactionsCount(previousStartDate, previousEndDate);
        double trend = calculateTrend(currentCount, previousCount);

        return new CustomReportMetricDTO(
            "TRANSACTIONS",
            "Nombre de ventes",
            String.valueOf(currentCount),
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
        long currentCA = getTotalCA(startDate, endDate);
        int currentCount = getTransactionsCount(startDate, endDate);
        long currentAvg = currentCount > 0 ? currentCA / currentCount : 0;

        long previousCA = getTotalCA(previousStartDate, previousEndDate);
        int previousCount = getTransactionsCount(previousStartDate, previousEndDate);
        long previousAvg = previousCount > 0 ? previousCA / previousCount : 0;

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
        MarginData current = getMarginData(startDate, endDate);
        MarginData previous = getMarginData(previousStartDate, previousEndDate);

        double trend = calculateTrend(current.marginAmount(), previous.marginAmount());

        return new CustomReportMetricDTO(
            "MARGIN",
            "Marge brute",
            formatAmount(current.marginAmount()),
            trend,
            null,
            String.format("%.1f%% du CA", current.marginPercent())
        );
    }

    /**
     * Generate Top Products metric
     */
    private CustomReportMetricDTO generateTopProductsMetric(LocalDate startDate, LocalDate endDate) {
        List<ChartDataPointDTO> topProducts = getTopProducts(startDate, endDate);

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
        List<ChartDataPointDTO> paymentMethods = getPaymentMethods(startDate, endDate);

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
        List<ChartDataPointDTO> categories = getCategoryStats(startDate, endDate);

        return new CustomReportMetricDTO(
            "SALES_BY_CATEGORY",
            "Ventes par catégorie",
            String.valueOf(categories.size()),
            null,
            categories,
            "Catégories actives"
        );
    }

    /**
     * Generate Customer Stats metric
     */
    private CustomReportMetricDTO generateCustomerStatsMetric(LocalDate startDate, LocalDate endDate) {
        CustomerStats stats = getCustomerStats(startDate, endDate);

        return new CustomReportMetricDTO(
            "CUSTOMER_STATS",
            "Statistiques clients",
            String.valueOf(stats.totalCustomers()),
            null,
            null,
            String.format("Nouveaux clients: %d", stats.newCustomers())
        );
    }

    /**
     * Generate Alerts metric
     */
    private CustomReportMetricDTO generateAlertsMetric() {
        // Get current alerts count
        // This would typically query the alerts/notifications system
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
        // Get stock statistics
        // This would query the stock management system
        return new CustomReportMetricDTO(
            "STOCK_STATUS",
            "État du stock",
            "N/A",
            null,
            null,
            "Statut du stock"
        );
    }

    // Helper methods for data retrieval

    private long getTotalCA(LocalDate startDate, LocalDate endDate) {
        String sql = """
            SELECT COALESCE(SUM(s.sales_amount), 0)
            FROM sales s
            WHERE s.sale_date BETWEEN :startDate AND :endDate
              AND s.statut = :statut
              AND s.canceled = false
              AND s.ca = :caType
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);
        query.setParameter("statut", SalesStatut.CLOSED.name());
        query.setParameter("caType", SALES_CA_TYPE);

        return ((Number) query.getSingleResult()).longValue();
    }

    private int getTransactionsCount(LocalDate startDate, LocalDate endDate) {
        String sql = """
            SELECT COUNT(DISTINCT s.id)
            FROM sales s
            WHERE s.sale_date BETWEEN :startDate AND :endDate
              AND s.statut = :statut
              AND s.canceled = false
              AND s.ca = :caType
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);
        query.setParameter("statut", SalesStatut.CLOSED.name());
        query.setParameter("caType", SALES_CA_TYPE);

        return ((Number) query.getSingleResult()).intValue();
    }

    private MarginData getMarginData(LocalDate startDate, LocalDate endDate) {
        String sql = """
            SELECT
                COALESCE(SUM(s.sales_amount), 0) as ca,
                COALESCE(SUM(s.sales_amount - s.cost_amount), 0) as margin
            FROM sales s
            WHERE s.sale_date BETWEEN :startDate AND :endDate
              AND s.statut = :statut
              AND s.canceled = false
              AND s.ca = :caType
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);
        query.setParameter("statut", SalesStatut.CLOSED.name());
        query.setParameter("caType", SALES_CA_TYPE);

        Object[] row = (Object[]) query.getSingleResult();
        long ca = ((Number) row[0]).longValue();
        long margin = ((Number) row[1]).longValue();
        double marginPercent = ca > 0 ? (margin * 100.0 / ca) : 0;

        return new MarginData(margin, marginPercent);
    }

    private List<ChartDataPointDTO> getDailyCAChart(LocalDate startDate, LocalDate endDate) {
        String sql = """
            SELECT
                s.sale_date,
                COALESCE(SUM(s.sales_amount), 0) as amount
            FROM sales s
            WHERE s.sale_date BETWEEN :startDate AND :endDate
              AND s.statut = :statut
              AND s.canceled = false
              AND s.ca = :caType
            GROUP BY s.sale_date
            ORDER BY s.sale_date
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);
        query.setParameter("statut", SalesStatut.CLOSED.name());
        query.setParameter("caType", SALES_CA_TYPE);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        List<ChartDataPointDTO> dataPoints = new ArrayList<>();
        for (Object[] row : results) {
            LocalDate date = ((java.sql.Date) row[0]).toLocalDate();
            double amount = ((Number) row[1]).doubleValue();
            dataPoints.add(new ChartDataPointDTO(date.toString(), amount, null));
        }

        return dataPoints;
    }

    private List<ChartDataPointDTO> getTopProducts(LocalDate startDate, LocalDate endDate) {
        String sql = """
            SELECT
                p.libelle,
                SUM(sl.sales_amount) as amount
            FROM sales_line sl
            INNER JOIN sales s ON sl.sales_id = s.id AND sl.sale_date = s.sale_date
            INNER JOIN produit p ON sl.produit_id = p.id
            WHERE s.sale_date BETWEEN :startDate AND :endDate
              AND s.statut = :statut
              AND s.canceled = false
              AND s.ca = :caType
            GROUP BY p.libelle
            ORDER BY amount DESC
            LIMIT 10
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);
        query.setParameter("statut", SalesStatut.CLOSED.name());
        query.setParameter("caType", SALES_CA_TYPE);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        List<ChartDataPointDTO> products = new ArrayList<>();
        for (Object[] row : results) {
            String name = (String) row[0];
            double amount = ((Number) row[1]).doubleValue();
            products.add(new ChartDataPointDTO(name, amount, null));
        }

        return products;
    }

    private List<ChartDataPointDTO> getPaymentMethods(LocalDate startDate, LocalDate endDate) {
        String sql = """
            SELECT
                pm.libelle,
                COALESCE(SUM(pt.paid_amount), 0) as amount
            FROM payment_transaction pt
            INNER JOIN payment_mode pm ON pt.payment_mode_code = pm.code
            INNER JOIN sales s ON pt.sale_id = s.id AND pt.sale_date = s.sale_date
            WHERE s.sale_date BETWEEN :startDate AND :endDate
              AND s.statut = :statut
              AND s.canceled = false
              AND s.ca = :caType
            GROUP BY pm.libelle
            ORDER BY amount DESC
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);
        query.setParameter("statut", SalesStatut.CLOSED.name());
        query.setParameter("caType", SALES_CA_TYPE);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        List<ChartDataPointDTO> paymentMethods = new ArrayList<>();
        for (Object[] row : results) {
            String name = (String) row[0];
            double amount = ((Number) row[1]).doubleValue();
            paymentMethods.add(new ChartDataPointDTO(name, amount, null));
        }

        return paymentMethods;
    }

    private List<ChartDataPointDTO> getCategoryStats(LocalDate startDate, LocalDate endDate) {
        // This would query by product categories
        // Simplified version
        return List.of();
    }

    private CustomerStats getCustomerStats(LocalDate startDate, LocalDate endDate) {
        String sql = """
            SELECT
                COUNT(DISTINCT s.customer_id) as total
            FROM sales s
            WHERE s.sale_date BETWEEN :startDate AND :endDate
              AND s.statut = :statut
              AND s.canceled = false
              AND s.ca = :caType
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);
        query.setParameter("statut", SalesStatut.CLOSED.name());
        query.setParameter("caType", SALES_CA_TYPE);

        int total = ((Number) query.getSingleResult()).intValue();

        return new CustomerStats(total, 0);
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

    // Helper records

    private record MarginData(long marginAmount, double marginPercent) {}
    private record CustomerStats(int totalCustomers, int newCustomers) {}
}
