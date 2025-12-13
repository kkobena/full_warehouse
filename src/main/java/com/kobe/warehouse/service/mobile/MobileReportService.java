package com.kobe.warehouse.service.mobile;

import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.service.dto.mobile.DailyDigestDTO;
import com.kobe.warehouse.service.dto.mobile.MobileDashboardDTO;
import com.kobe.warehouse.service.dto.mobile.UserPerformanceDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for generating mobile report data for notifications and analytics.
 * Provides daily digests and user performance metrics.
 */
@Service
@Transactional(readOnly = true)
public class MobileReportService {

    private static final Logger LOG = LoggerFactory.getLogger(MobileReportService.class);
    private static final String SALES_CA_TYPE = "CA";

    @PersistenceContext
    private EntityManager entityManager;

    private final MobileAlertService alertService;

    public MobileReportService(MobileAlertService alertService) {
        this.alertService = alertService;
    }

    /**
     * Generate daily digest for managers.
     * Contains overall pharmacy performance for the day.
     *
     * @param date Date for which to generate the digest
     * @return Daily digest DTO
     */
    public DailyDigestDTO generateDailyDigest(LocalDate date) {
        LOG.debug("Generating daily digest for date: {}", date);

        // Get current day summary
        DailySalesSummary currentDay = getDailySalesSummary(date);

        // Get previous day summary for variation
        DailySalesSummary previousDay = getDailySalesSummary(date.minusDays(1));

        // Calculate variation
        double variation = calculateVariation(currentDay.totalCA(), previousDay.totalCA());

        // Get daily target (could be from configuration)
        long dailyTarget = getDailyTarget(date);

        // Calculate target progress
        double targetProgress = dailyTarget > 0
            ? (currentDay.totalCA() * 100.0 / dailyTarget)
            : 0.0;

        // Get alerts count
        int alertsCount = alertService.getAlertsSummary()
            .stream()
            .mapToInt(MobileDashboardDTO.MobileAlertDTO::count)
            .sum();

        return new DailyDigestDTO(
            currentDay.totalCA(),
            variation,
            currentDay.transactionCount(),
            alertsCount,
            dailyTarget,
            targetProgress,
            currentDay.customersCount(),
            currentDay.averageBasket()
        );
    }

    /**
     * Get user performance for a specific user and date.
     * Used for individual seller notifications.
     *
     * @param userId User ID
     * @param date Date for which to get performance
     * @return User performance DTO
     */
    public UserPerformanceDTO getUserPerformance(Integer userId, LocalDate date) {
        LOG.debug("Getting user performance for user {} on date {}", userId, date);

        // Get user sales summary for current day
        UserSalesSummary currentDay = getUserSalesSummary(userId, date);

        // Get previous day for variation
        UserSalesSummary previousDay = getUserSalesSummary(userId, date.minusDays(1));

        // Calculate variation
        double variation = calculateVariation(currentDay.totalCA(), previousDay.totalCA());

        // Get user name
        String userName = getUserName(userId);

        UserPerformanceDTO performance = new UserPerformanceDTO(
            userId.longValue(),
            userName,
            currentDay.totalCA(),
            currentDay.salesCount(),
            currentDay.averageBasket(),
            variation
        );

        // Set margin data
        performance.setMarginAmount(currentDay.marginAmount());
        performance.setMarginPercent(currentDay.marginPercent());

        return performance;
    }

    /**
     * Get daily sales summary for a date.
     */
    private DailySalesSummary getDailySalesSummary(LocalDate date) {
        String sql = """
            SELECT
                COALESCE(SUM(s.sales_amount), 0) as total_ca,
                COUNT(DISTINCT s.id) as transaction_count,
                COUNT(DISTINCT s.customer_id) as customers_count,
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

        Object[] row = (Object[]) query.getSingleResult();

        long totalCA = ((Number) row[0]).longValue();
        int transactionCount = ((Number) row[1]).intValue();
        int customersCount = ((Number) row[2]).intValue();
        long marginAmount = ((Number) row[3]).longValue();

        long averageBasket = transactionCount > 0 ? totalCA / transactionCount : 0;
        double marginPercent = totalCA > 0 ? (marginAmount * 100.0 / totalCA) : 0.0;

        return new DailySalesSummary(
            totalCA,
            transactionCount,
            customersCount,
            averageBasket,
            marginAmount,
            marginPercent
        );
    }

    /**
     * Get user sales summary for a specific user and date.
     */
    private UserSalesSummary getUserSalesSummary(Integer userId, LocalDate date) {
        String sql = """
            SELECT
                COALESCE(SUM(s.sales_amount), 0) as total_ca,
                COUNT(DISTINCT s.id) as sales_count,
                COALESCE(SUM(s.sales_amount - s.cost_amount), 0) as margin_amount
            FROM sales s
            WHERE s.sale_date = :date
              AND s.seller_id = :userId
              AND s.statut = :statut
              AND s.canceled = false
              AND s.ca = :caType
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("date", date);
        query.setParameter("userId", userId);
        query.setParameter("statut", SalesStatut.CLOSED.name());
        query.setParameter("caType", SALES_CA_TYPE);

        Object[] row = (Object[]) query.getSingleResult();

        long totalCA = ((Number) row[0]).longValue();
        int salesCount = ((Number) row[1]).intValue();
        long marginAmount = ((Number) row[2]).longValue();

        long averageBasket = salesCount > 0 ? totalCA / salesCount : 0;
        double marginPercent = totalCA > 0 ? (marginAmount * 100.0 / totalCA) : 0.0;

        return new UserSalesSummary(
            totalCA,
            salesCount,
            averageBasket,
            marginAmount,
            marginPercent
        );
    }

    /**
     * Get user name by ID.
     */
    private String getUserName(Integer userId) {
        String sql = "SELECT CONCAT(u.first_name, ' ', u.last_name) FROM app_user u WHERE u.id = :userId";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("userId", userId);

        try {
            return (String) query.getSingleResult();
        } catch (Exception e) {
            LOG.warn("Could not find user with ID: {}", userId);
            return "Unknown User";
        }
    }

    /**
     * Get daily target CA (could be from configuration).
     * Default: 2,000,000 FCFA
     */
    private long getDailyTarget(LocalDate date) {
        // TODO: Could be retrieved from configuration table
        // For now, return a default value
        return 2_000_000L;
    }

    /**
     * Calculate variation percentage between current and previous value.
     */
    private double calculateVariation(long current, long previous) {
        if (previous == 0) {
            return current > 0 ? 100.0 : 0.0;
        }

        return BigDecimal.valueOf(((current - previous) * 100.0) / previous)
            .setScale(2, RoundingMode.HALF_UP)
            .doubleValue();
    }

    // Helper records

    private record DailySalesSummary(
        long totalCA,
        int transactionCount,
        int customersCount,
        long averageBasket,
        long marginAmount,
        double marginPercent
    ) {}

    private record UserSalesSummary(
        long totalCA,
        int salesCount,
        long averageBasket,
        long marginAmount,
        double marginPercent
    ) {}
}
