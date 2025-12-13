package com.kobe.warehouse.service.dto.mobile;

import java.time.LocalDate;

/**
 * DTO for daily sales data (Phase 4 - ML Forecasting)
 */
public record DailySalesDTO(
    LocalDate date,
    String dateLabel,
    Long salesAmount,
    Integer transactionsCount,
    Long averageBasket,
    Integer customersCount
) {

    /**
     * Create from query result array
     */
    public static DailySalesDTO fromQueryResult(Object[] row) {
        LocalDate date = ((java.sql.Date) row[0]).toLocalDate();
        Long amount = ((Number) row[1]).longValue();
        Integer count = ((Number) row[2]).intValue();
        Integer customers = row.length > 3 ? ((Number) row[3]).intValue() : 0;

        Long avgBasket = count > 0 ? amount / count : 0L;

        return new DailySalesDTO(
            date,
            date.toString(),
            amount,
            count,
            avgBasket,
            customers
        );
    }
}
