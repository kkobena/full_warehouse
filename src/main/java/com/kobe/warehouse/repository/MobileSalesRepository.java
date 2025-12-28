package com.kobe.warehouse.repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository for common mobile sales queries.
 * Used by MobileDashboardService, MobileReportService, and MobileCustomReportService.
 */
public interface MobileSalesRepository {

    /**
     * Get daily sales summary for a date.
     *
     * @param date Date for summary
     * @return Daily sales summary
     */
    DailySalesSummaryProjection getDailySalesSummary(LocalDate date);

    /**
     * Get sales summary for a date range.
     *
     * @param startDate Start date
     * @param endDate End date
     * @return Sales summary for period
     */
    SalesSummaryProjection getSalesSummary(LocalDate startDate, LocalDate endDate);

    /**
     * Get user sales summary for a specific user and date.
     *
     * @param userId User ID
     * @param date Date
     * @return User sales summary
     */
    UserSalesSummaryProjection getUserSalesSummary(int userId, LocalDate date);

    /**
     * Get top products for a date.
     *
     * @param date Date
     * @param limit Max results
     * @return List of top products
     */
    List<TopProductProjection> getTopProducts(LocalDate date, int limit);

    /**
     * Get top products for a date range.
     *
     * @param startDate Start date
     * @param endDate End date
     * @param limit Max results
     * @return List of top products
     */
    List<TopProductProjection> getTopProducts(LocalDate startDate, LocalDate endDate, int limit);

    /**
     * Get CA trend for a date range.
     *
     * @param startDate Start date
     * @param endDate End date
     * @return List of daily CA summaries
     */
    List<DailyCATrendProjection> getCATrend(LocalDate startDate, LocalDate endDate);

    /**
     * Get daily target based on historical average.
     *
     * @param date Reference date
     * @param lookbackDays Number of days to look back
     * @param growthFactor Growth factor (e.g., 1.1 for 10% growth)
     * @return Daily target
     */
    long getDailyTarget(LocalDate date, int lookbackDays, double growthFactor);

    /**
     * Get payment methods summary for a date range.
     *
     * @param startDate Start date
     * @param endDate End date
     * @return List of payment methods
     */
    List<PaymentMethodProjection> getPaymentMethodsSummary(LocalDate startDate, LocalDate endDate);

    /**
     * Get customer count for a date range.
     *
     * @param startDate Start date
     * @param endDate End date
     * @return Customer count
     */
    int getCustomersCount(LocalDate startDate, LocalDate endDate);

    /**
     * Get user name by ID.
     *
     * @param userId User ID
     * @return User full name
     */
    String getUserName(int userId);

    /**
     * Projection for daily sales summary.
     */
    record DailySalesSummaryProjection(
        long caTotal,
        int transactionsCount,
        int customersCount,
        long averageBasket,
        long amountCollected,
        long amountCredit,
        long marginAmount,
        double marginPercent
    ) {}

    /**
     * Projection for sales summary (date range).
     */
    record SalesSummaryProjection(
        long caTotal,
        int transactionsCount,
        int customersCount,
        long marginAmount
    ) {}

    /**
     * Projection for user sales summary.
     */
    record UserSalesSummaryProjection(
        long totalCA,
        int salesCount,
        long averageBasket,
        long marginAmount,
        double marginPercent
    ) {}

    /**
     * Projection for top product.
     */
    record TopProductProjection(
        long productId,
        String productName,
        String codeCip,
        long salesAmount,
        int quantitySold,
        int rank
    ) {}

    /**
     * Projection for daily CA trend.
     */
    record DailyCATrendProjection(
        LocalDate date,
        long caTotal,
        int transactionsCount
    ) {}

    /**
     * Projection for payment method.
     */
    record PaymentMethodProjection(
        String code,
        String libelle,
        long amount
    ) {}
}
