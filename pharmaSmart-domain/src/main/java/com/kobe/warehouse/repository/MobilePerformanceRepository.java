package com.kobe.warehouse.repository;

import com.kobe.warehouse.service.dto.mobile.PerformancePeriod;
import java.time.LocalDate;
import java.util.List;

/**
 * Repository for mobile performance queries.
 */
public interface MobilePerformanceRepository {

    /**
     * Get period summary (CA, transactions, customers, margin).
     *
     * @param startDate Start date of period
     * @param endDate End date of period
     * @return Period summary data
     */
    PeriodSummaryProjection getPeriodSummary(LocalDate startDate, LocalDate endDate);

    /**
     * Get payment methods summary for period.
     *
     * @param startDate Start date of period
     * @param endDate End date of period
     * @return List of payment method summaries
     */
    List<PaymentMethodProjection> getPaymentMethodsSummary(LocalDate startDate, LocalDate endDate);

    /**
     * Get top products with performance comparison.
     *
     * @param startDate Current period start date
     * @param endDate Current period end date
     * @param previousStartDate Previous period start date
     * @param previousEndDate Previous period end date
     * @param limit Maximum number of products to return
     * @return List of top products
     */
    List<TopProductProjection> getTopProducts(
        LocalDate startDate,
        LocalDate endDate,
        LocalDate previousStartDate,
        LocalDate previousEndDate,
        int limit
    );

    /**
     * Get data points for chart.
     *
     * @param startDate Start date of period
     * @param endDate End date of period
     * @param period Performance period (for grouping)
     * @return List of data points
     */
    List<DataPointProjection> getDataPoints(LocalDate startDate, LocalDate endDate, PerformancePeriod period);

    /**
     * Projection for period summary.
     */
    record PeriodSummaryProjection(
        long caTotal,
        int transactionsCount,
        int customersCount,
        long marginTotal
    ) {}

    /**
     * Projection for payment method summary.
     */
    record PaymentMethodProjection(
        String code,
        String libelle,
        long amount,
        int transactionsCount
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
        double percentOfTotal,
        double variationPercent
    ) {}

    /**
     * Projection for data point.
     */
    record DataPointProjection(
        LocalDate date,
        long caAmount,
        int transactionsCount,
        long marginAmount
    ) {}
}
