package com.kobe.warehouse.service.dto.mobile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Performance report data for mobile analytics screen.
 * Supports weekly, monthly, and yearly views.
 */
public record MobilePerformanceDTO(
    // Period info
    String period,            // WEEK, MONTH, YEAR
    LocalDate startDate,
    LocalDate endDate,

    // CA Summary
    long caTotal,
    long caPreviousPeriod,
    double variationPercent,

    // Transactions
    int transactionsCount,
    long averageBasket,
    int customersCount,

    // Margins
    long marginTotal,
    double marginPercent,

    // Payment methods breakdown
    List<PaymentMethodSummaryDTO> paymentMethods,

    // Top products
    List<TopProductPerformanceDTO> topProducts,

    // Daily/weekly data for charts
    List<PeriodDataPointDTO> dataPoints,

    // Metadata
    LocalDateTime generatedAt
) {
    /**
     * Payment method summary.
     */
    public record PaymentMethodSummaryDTO(
        String code,
        String label,
        long amount,
        double percent,
        int transactionsCount,
        String color
    ) {}

    /**
     * Top product with performance data.
     */
    public record TopProductPerformanceDTO(
        int rank,
        Long productId,
        String productName,
        String codeCip,
        long salesAmount,
        int quantitySold,
        double percentOfTotal,
        double variationPercent
    ) {}

    /**
     * Data point for charts (daily for week/month, monthly for year).
     */
    public record PeriodDataPointDTO(
        LocalDate date,
        String label,         // "Lun", "Mar", "Jan", etc.
        long caAmount,
        int transactionsCount,
        long marginAmount
    ) {}

    /**
     * Builder for creating performance data.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String period;
        private LocalDate startDate;
        private LocalDate endDate;
        private long caTotal;
        private long caPreviousPeriod;
        private double variationPercent;
        private int transactionsCount;
        private long averageBasket;
        private int customersCount;
        private long marginTotal;
        private double marginPercent;
        private List<PaymentMethodSummaryDTO> paymentMethods = List.of();
        private List<TopProductPerformanceDTO> topProducts = List.of();
        private List<PeriodDataPointDTO> dataPoints = List.of();

        public Builder period(String period) {
            this.period = period;
            return this;
        }

        public Builder startDate(LocalDate startDate) {
            this.startDate = startDate;
            return this;
        }

        public Builder endDate(LocalDate endDate) {
            this.endDate = endDate;
            return this;
        }

        public Builder caTotal(long caTotal) {
            this.caTotal = caTotal;
            return this;
        }

        public Builder caPreviousPeriod(long caPreviousPeriod) {
            this.caPreviousPeriod = caPreviousPeriod;
            return this;
        }

        public Builder variationPercent(double variationPercent) {
            this.variationPercent = variationPercent;
            return this;
        }

        public Builder transactionsCount(int transactionsCount) {
            this.transactionsCount = transactionsCount;
            return this;
        }

        public Builder averageBasket(long averageBasket) {
            this.averageBasket = averageBasket;
            return this;
        }

        public Builder customersCount(int customersCount) {
            this.customersCount = customersCount;
            return this;
        }

        public Builder marginTotal(long marginTotal) {
            this.marginTotal = marginTotal;
            return this;
        }

        public Builder marginPercent(double marginPercent) {
            this.marginPercent = marginPercent;
            return this;
        }

        public Builder paymentMethods(List<PaymentMethodSummaryDTO> paymentMethods) {
            this.paymentMethods = paymentMethods;
            return this;
        }

        public Builder topProducts(List<TopProductPerformanceDTO> topProducts) {
            this.topProducts = topProducts;
            return this;
        }

        public Builder dataPoints(List<PeriodDataPointDTO> dataPoints) {
            this.dataPoints = dataPoints;
            return this;
        }

        public MobilePerformanceDTO build() {
            return new MobilePerformanceDTO(
                period, startDate, endDate, caTotal, caPreviousPeriod, variationPercent,
                transactionsCount, averageBasket, customersCount, marginTotal, marginPercent,
                paymentMethods, topProducts, dataPoints, LocalDateTime.now()
            );
        }
    }
}
