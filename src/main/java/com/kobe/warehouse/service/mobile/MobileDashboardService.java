package com.kobe.warehouse.service.mobile;

import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.service.dto.mobile.MobileDashboardDTO;
import com.kobe.warehouse.service.dto.mobile.MobileDashboardDTO.DailyCASummaryDTO;
import com.kobe.warehouse.service.dto.mobile.MobileDashboardDTO.MobileAlertDTO;
import com.kobe.warehouse.service.dto.mobile.MobileDashboardDTO.TopProductDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for mobile dashboard data aggregation.
 * Optimized to return all dashboard data in a single query batch.
 */
@Service
@Transactional(readOnly = true)
public class MobileDashboardService {

    private static final Logger LOG = LoggerFactory.getLogger(MobileDashboardService.class);
    private static final String SALES_CA_TYPE = "CA";

    private final MobileAlertService alertService;

    @PersistenceContext
    private EntityManager entityManager;

    public MobileDashboardService(MobileAlertService alertService) {
        this.alertService = alertService;
    }

    /**
     * Get complete dashboard data for a given date.
     *
     * @param date The date for which to get dashboard data
     * @return Complete dashboard DTO with all widgets data
     */
    public MobileDashboardDTO getDashboard(LocalDate date) {
        LOG.debug("Getting mobile dashboard for date: {}", date);

        // Get daily sales summary
        DailySalesSummary dailySummary = getDailySalesSummary(date);

        // Get previous day for variation calculation
        DailySalesSummary previousDaySummary = getDailySalesSummary(date.minusDays(1));

        // Calculate variation
        double variationPercent = calculateVariation(dailySummary.caTotal(), previousDaySummary.caTotal());

        // Get daily target (could be from configuration or calculated)
        long dailyTarget = getDailyTarget(date);

        // Calculate progress percent
        int progressPercent = dailyTarget > 0 ? (int) Math.min(100, (dailySummary.caTotal() * 100) / dailyTarget) : 0;

        // Get alerts summary
        List<MobileAlertDTO> alerts = alertService.getAlertsSummary();

        // Get top products for the day
        List<TopProductDTO> topProducts = getTopProducts(date, 5);

        // Get CA trend for last 7 days
        List<DailyCASummaryDTO> caTrend = getCATrend(date.minusDays(6), date);

        return new MobileDashboardDTO(
            dailySummary.caTotal(),
            dailyTarget,
            variationPercent,
            progressPercent,
            dailySummary.transactionsCount(),
            dailySummary.averageBasket(),
            dailySummary.customersCount(),
            dailySummary.amountCollected(),
            dailySummary.amountCredit(),
            dailySummary.marginAmount(),
            dailySummary.marginPercent(),
            alerts,
            alerts.stream().mapToInt(MobileAlertDTO::count).sum(),
            topProducts,
            caTrend,
            date,
            LocalDateTime.now()
        );
    }

    /**
     * Get daily sales summary from materialized view or direct query.
     */
    private DailySalesSummary getDailySalesSummary(LocalDate date) {
        String sql = """
            SELECT
                COALESCE(SUM(s.sales_amount), 0) as ca_total,
                COUNT(DISTINCT s.id) as transactions_count,
                COUNT(DISTINCT s.customer_id) as customers_count,
                COALESCE(SUM(
                    CASE WHEN s.rest_to_pay = 0 AND s.part_tiers_payant = 0
                    THEN s.sales_amount ELSE 0 END
                ), 0) as amount_collected,
                COALESCE(SUM(s.rest_to_pay + s.part_tiers_payant), 0) as amount_credit,
                COALESCE(SUM(s.sales_amount - s.cost_amount), 0) as margin_amount
            FROM sales s
            WHERE s.sale_date = :date
              AND s.statut = :statut
              AND s.canceled = false
              AND s.ca = :caType
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("date", date);
        query.setParameter("statut", SalesStatut.CLOSED.name());
        query.setParameter("caType", SALES_CA_TYPE);

        Object[] result = (Object[]) query.getSingleResult();

        long caTotal = ((Number) result[0]).longValue();
        int transactionsCount = ((Number) result[1]).intValue();
        int customersCount = ((Number) result[2]).intValue();
        long amountCollected = ((Number) result[3]).longValue();
        long amountCredit = ((Number) result[4]).longValue();
        long marginAmount = ((Number) result[5]).longValue();

        long averageBasket = transactionsCount > 0 ? caTotal / transactionsCount : 0;
        double marginPercent = caTotal > 0
            ? BigDecimal.valueOf((marginAmount * 100.0) / caTotal).setScale(2, RoundingMode.HALF_UP).doubleValue()
            : 0;

        return new DailySalesSummary(
            caTotal,
            transactionsCount,
            customersCount,
            averageBasket,
            amountCollected,
            amountCredit,
            marginAmount,
            marginPercent
        );
    }

    /**
     * Get top selling products for a given date.
     * Uses composite key join for partitioned sales_line table.
     */
    private List<TopProductDTO> getTopProducts(LocalDate date, int limit) {
        String sql = """
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
            WHERE s.sale_date = :date
              AND s.statut = :statut
              AND s.canceled = false
              AND s.ca = :caType
            GROUP BY p.id, p.libelle, fp.code_cip
            ORDER BY sales_amount DESC
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("date", date);
        query.setParameter("statut", SalesStatut.CLOSED.name());
        query.setParameter("caType", SALES_CA_TYPE);
        query.setMaxResults(limit);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        List<TopProductDTO> topProducts = new ArrayList<>();
        int rank = 1;
        for (Object[] row : results) {
            topProducts.add(
                new TopProductDTO(
                    ((Number) row[0]).longValue(),
                    (String) row[1],
                    (String) row[2],
                    ((Number) row[3]).longValue(),
                    ((Number) row[4]).intValue(),
                    rank++
                )
            );
        }

        return topProducts;
    }

    /**
     * Get CA trend for a date range.
     */
    private List<DailyCASummaryDTO> getCATrend(LocalDate startDate, LocalDate endDate) {
        String sql = """
            SELECT
                s.sale_date,
                COALESCE(SUM(s.sales_amount), 0) as ca_total,
                COUNT(DISTINCT s.id) as transactions_count
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

        List<DailyCASummaryDTO> trend = new ArrayList<>();
        for (Object[] row : results) {
            LocalDate date = ((java.sql.Date) row[0]).toLocalDate();
            String dayLabel = getDayLabel(date.getDayOfWeek());

            trend.add(new DailyCASummaryDTO(date, dayLabel, ((Number) row[1]).longValue(), ((Number) row[2]).intValue()));
        }

        return trend;
    }

    /**
     * Get daily target (can be configured or calculated from historical data).
     */
    private long getDailyTarget(LocalDate date) {
        // Simple implementation: average of last 30 days * 1.1 (10% growth target)
        String sql = """
            SELECT COALESCE(AVG(daily_ca), 0) as avg_ca
            FROM (
                SELECT SUM(s.sales_amount) as daily_ca
                FROM sales s
                WHERE s.sale_date BETWEEN :startDate AND :endDate
                  AND s.statut = :statut
                  AND s.canceled = false
                  AND s.ca = :caType
                GROUP BY s.sale_date
            ) daily_sales
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("startDate", date.minusDays(30));
        query.setParameter("endDate", date.minusDays(1));
        query.setParameter("statut", SalesStatut.CLOSED.name());
        query.setParameter("caType", SALES_CA_TYPE);

        Number avgCa = (Number) query.getSingleResult();
        return (long) (avgCa.doubleValue() * 1.1);
    }

    private double calculateVariation(long current, long previous) {
        if (previous == 0) {
            return current > 0 ? 100.0 : 0.0;
        }
        return BigDecimal.valueOf(((current - previous) * 100.0) / previous).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private String getDayLabel(DayOfWeek dayOfWeek) {
        return dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.FRENCH);
    }

    /**
     * Internal record for daily sales summary.
     */
    private record DailySalesSummary(
        long caTotal,
        int transactionsCount,
        int customersCount,
        long averageBasket,
        long amountCollected,
        long amountCredit,
        long marginAmount,
        double marginPercent
    ) {}
}
