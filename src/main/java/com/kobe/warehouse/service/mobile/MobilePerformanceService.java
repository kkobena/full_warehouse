package com.kobe.warehouse.service.mobile;

import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.service.dto.mobile.MobilePerformanceDTO;
import com.kobe.warehouse.service.dto.mobile.MobilePerformanceDTO.PaymentMethodSummaryDTO;
import com.kobe.warehouse.service.dto.mobile.MobilePerformanceDTO.PeriodDataPointDTO;
import com.kobe.warehouse.service.dto.mobile.MobilePerformanceDTO.TopProductPerformanceDTO;
import com.kobe.warehouse.service.dto.mobile.PaymentMethodColor;
import com.kobe.warehouse.service.dto.mobile.PerformancePeriod;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for mobile performance reports (Phase 2).
 */
@Service
@Transactional(readOnly = true)
public class MobilePerformanceService {

    private static final Logger LOG = LoggerFactory.getLogger(MobilePerformanceService.class);

    private static final String SALES_CA_TYPE = "CA";

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Get performance data for a period.
     *
     * @param period WEEK, MONTH, or YEAR
     * @param referenceDate Reference date for period calculation
     * @return Performance data DTO
     */
    public MobilePerformanceDTO getPerformance(String period, LocalDate referenceDate) {
        LOG.debug("Getting performance for period: {} from date: {}", period, referenceDate);

        PerformancePeriod performancePeriod = PerformancePeriod.fromString(period);

        LocalDate startDate = performancePeriod.getStartDate(referenceDate);
        LocalDate endDate = performancePeriod.getEndDate(referenceDate);
        LocalDate previousStartDate = performancePeriod.getPreviousStartDate(referenceDate);
        LocalDate previousEndDate = performancePeriod.getPreviousEndDate(referenceDate);

        // Get current period summary
        PeriodSummary currentSummary = getPeriodSummary(startDate, endDate);
        PeriodSummary previousSummary = getPeriodSummary(previousStartDate, previousEndDate);

        // Calculate variation
        double variationPercent = calculateVariation(currentSummary.caTotal(), previousSummary.caTotal());

        // Get payment methods breakdown
        List<PaymentMethodSummaryDTO> paymentMethods = getPaymentMethodsSummary(startDate, endDate, currentSummary.caTotal());

        // Get top products
        List<TopProductPerformanceDTO> topProducts = getTopProducts(startDate, endDate, previousStartDate, previousEndDate);

        // Get data points for chart
        List<PeriodDataPointDTO> dataPoints = getDataPoints(startDate, endDate, performancePeriod);

        return MobilePerformanceDTO.builder()
            .period(performancePeriod.getCode())
            .startDate(startDate)
            .endDate(endDate)
            .caTotal(currentSummary.caTotal())
            .caPreviousPeriod(previousSummary.caTotal())
            .variationPercent(variationPercent)
            .transactionsCount(currentSummary.transactionsCount())
            .averageBasket(currentSummary.averageBasket())
            .customersCount(currentSummary.customersCount())
            .marginTotal(currentSummary.marginTotal())
            .marginPercent(currentSummary.marginPercent())
            .paymentMethods(paymentMethods)
            .topProducts(topProducts)
            .dataPoints(dataPoints)
            .build();
    }

    /**
     * Get performance data for a period using enum directly.
     *
     * @param period PerformancePeriod enum
     * @param referenceDate Reference date for period calculation
     * @return Performance data DTO
     */
    public MobilePerformanceDTO getPerformance(PerformancePeriod period, LocalDate referenceDate) {
        return getPerformance(period.getCode(), referenceDate);
    }

    /**
     * Get summary for a period.
     */
    private PeriodSummary getPeriodSummary(LocalDate startDate, LocalDate endDate) {
        String sql = """
            SELECT
                COALESCE(SUM(s.sales_amount), 0) as ca_total,
                COUNT(DISTINCT s.id) as transactions_count,
                COUNT(DISTINCT s.customer_id) as customers_count,
                COALESCE(SUM(s.sales_amount - s.cost_amount), 0) as margin_total
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

        long caTotal = ((Number) row[0]).longValue();
        int transactionsCount = ((Number) row[1]).intValue();
        int customersCount = ((Number) row[2]).intValue();
        long marginTotal = ((Number) row[3]).longValue();

        long averageBasket = transactionsCount > 0 ? caTotal / transactionsCount : 0;
        double marginPercent = caTotal > 0
            ? BigDecimal.valueOf((marginTotal * 100.0) / caTotal).setScale(2, RoundingMode.HALF_UP).doubleValue()
            : 0;

        return new PeriodSummary(caTotal, transactionsCount, customersCount, averageBasket, marginTotal, marginPercent);
    }

    /**
     * Get payment methods summary.
     */
    private List<PaymentMethodSummaryDTO> getPaymentMethodsSummary(LocalDate startDate, LocalDate endDate, long totalCA) {
        String sql = """
            SELECT
                pm.code,
                pm.libelle,
                COALESCE(SUM(pt.paid_amount), 0) as amount,
                COUNT(DISTINCT pt.id) as transactions_count
            FROM payment_transaction pt
            INNER JOIN payment_mode pm ON pt.payment_mode_code = pm.code
            INNER JOIN sales s ON pt.sale_id = s.id AND pt.sale_date = s.sale_date
            WHERE s.sale_date BETWEEN :startDate AND :endDate
              AND s.statut = :statut
              AND s.canceled = false
              AND s.ca = :caType
            GROUP BY pm.code, pm.libelle
            ORDER BY amount DESC
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);
        query.setParameter("statut", SalesStatut.CLOSED.name());
        query.setParameter("caType", SALES_CA_TYPE);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        List<PaymentMethodSummaryDTO> paymentMethods = new ArrayList<>();
        for (Object[] row : results) {
            String code = (String) row[0];
            String label = (String) row[1];
            long amount = ((Number) row[2]).longValue();
            int transactionsCount = ((Number) row[3]).intValue();

            double percent = totalCA > 0
                ? BigDecimal.valueOf((amount * 100.0) / totalCA).setScale(1, RoundingMode.HALF_UP).doubleValue()
                : 0;

            String color = PaymentMethodColor.getColorForCode(code);

            paymentMethods.add(new PaymentMethodSummaryDTO(code, label, amount, percent, transactionsCount, color));
        }

        return paymentMethods;
    }

    /**
     * Get top products with performance comparison.
     */
    private List<TopProductPerformanceDTO> getTopProducts(
        LocalDate startDate,
        LocalDate endDate,
        LocalDate previousStartDate,
        LocalDate previousEndDate
    ) {
        String sql = """
            WITH current_period AS (
                SELECT
                    p.id,
                    p.libelle,
                    fp.code_cip,
                    SUM(sl.sales_amount) as sales_amount,
                    SUM(sl.quantity_sold) as quantity_sold
                FROM sales_line sl
                INNER JOIN sales s ON sl.sales_id = s.id AND sl.sale_date = s.sale_date
                INNER JOIN produit p ON sl.produit_id = p.id
                LEFT JOIN fournisseur_produit fp ON p.fournisseur_produit_principal_id = fp.id
                WHERE s.sale_date BETWEEN :startDate AND :endDate
                  AND s.statut = :statut
                  AND s.canceled = false
                  AND s.ca = :caType
                GROUP BY p.id, p.libelle, fp.code_cip
            ),
            previous_period AS (
                SELECT
                    p.id,
                    SUM(sl.sales_amount) as sales_amount
                FROM sales_line sl
                INNER JOIN sales s ON sl.sales_id = s.id AND sl.sale_date = s.sale_date
                INNER JOIN produit p ON sl.produit_id = p.id
                WHERE s.sale_date BETWEEN :previousStartDate AND :previousEndDate
                  AND s.statut = :statut
                  AND s.canceled = false
                  AND s.ca = :caType
                GROUP BY p.id
            ),
            total_ca AS (
                SELECT COALESCE(SUM(sales_amount), 1) as total FROM current_period
            )
            SELECT
                cp.id,
                cp.libelle,
                cp.code_cip,
                cp.sales_amount,
                cp.quantity_sold,
                (cp.sales_amount * 100.0 / t.total) as percent_of_total,
                CASE
                    WHEN pp.sales_amount > 0
                    THEN ((cp.sales_amount - pp.sales_amount) * 100.0 / pp.sales_amount)
                    ELSE 0
                END as variation_percent
            FROM current_period cp
            CROSS JOIN total_ca t
            LEFT JOIN previous_period pp ON cp.id = pp.id
            ORDER BY cp.sales_amount DESC
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);
        query.setParameter("previousStartDate", previousStartDate);
        query.setParameter("previousEndDate", previousEndDate);
        query.setParameter("statut", SalesStatut.CLOSED.name());
        query.setParameter("caType", SALES_CA_TYPE);
        query.setMaxResults(10);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        List<TopProductPerformanceDTO> topProducts = new ArrayList<>();
        int rank = 1;
        for (Object[] row : results) {
            topProducts.add(
                new TopProductPerformanceDTO(
                    rank++,
                    ((Number) row[0]).longValue(),
                    (String) row[1],
                    (String) row[2],
                    ((Number) row[3]).longValue(),
                    ((Number) row[4]).intValue(),
                    BigDecimal.valueOf(((Number) row[5]).doubleValue()).setScale(1, RoundingMode.HALF_UP).doubleValue(),
                    BigDecimal.valueOf(((Number) row[6]).doubleValue()).setScale(1, RoundingMode.HALF_UP).doubleValue()
                )
            );
        }

        return topProducts;
    }

    /**
     * Get data points for chart based on period type.
     */
    private List<PeriodDataPointDTO> getDataPoints(LocalDate startDate, LocalDate endDate, PerformancePeriod period) {
        String groupBy = period.getSqlGroupBy();

        String sql = """
            SELECT
                %s as period_date,
                COALESCE(SUM(s.sales_amount), 0) as ca_amount,
                COUNT(DISTINCT s.id) as transactions_count,
                COALESCE(SUM(s.sales_amount - s.cost_amount), 0) as margin_amount
            FROM sales s
            WHERE s.sale_date BETWEEN :startDate AND :endDate
              AND s.statut = :statut
              AND s.canceled = false
              AND s.ca = :caType
            GROUP BY %s
            ORDER BY period_date
            """.formatted(groupBy, groupBy);

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);
        query.setParameter("statut", SalesStatut.CLOSED.name());
        query.setParameter("caType", SALES_CA_TYPE);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        List<PeriodDataPointDTO> dataPoints = new ArrayList<>();
        for (Object[] row : results) {
            LocalDate date = ((java.sql.Date) row[0]).toLocalDate();
            String label = getLabel(date, period);

            dataPoints.add(
                new PeriodDataPointDTO(
                    date,
                    label,
                    ((Number) row[1]).longValue(),
                    ((Number) row[2]).intValue(),
                    ((Number) row[3]).longValue()
                )
            );
        }

        return dataPoints;
    }

    /**
     * Get label for a date based on period type.
     */
    private String getLabel(LocalDate date, PerformancePeriod period) {
        return switch (period.getLabelFormat()) {
            case "day" -> date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.FRENCH);
            case "month" -> date.getMonth().getDisplayName(TextStyle.SHORT, Locale.FRENCH);
            default -> date.toString();
        };
    }

    /**
     * Calculate variation percentage between two values.
     */
    private double calculateVariation(long current, long previous) {
        if (previous == 0) {
            return current > 0 ? 100.0 : 0.0;
        }
        return BigDecimal.valueOf(((current - previous) * 100.0) / previous)
            .setScale(2, RoundingMode.HALF_UP)
            .doubleValue();
    }

    /**
     * Internal record for period summary.
     */
    private record PeriodSummary(
        long caTotal,
        int transactionsCount,
        int customersCount,
        long averageBasket,
        long marginTotal,
        double marginPercent
    ) {}
}
