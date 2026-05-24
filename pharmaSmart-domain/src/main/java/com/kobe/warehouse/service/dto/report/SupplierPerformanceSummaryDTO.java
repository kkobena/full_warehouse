package com.kobe.warehouse.service.dto.report;

import java.math.BigDecimal;

/**
 * DTO for supplier performance summary (aggregated metrics)
 */
public record SupplierPerformanceSummaryDTO(
    Integer totalSuppliers,
    Long totalPurchaseAmountLast12Months,
    Long totalPurchaseAmountLast30Days,
    Integer totalOrdersLast12Months,
    Integer totalOrdersLast30Days,
    BigDecimal avgDeliveryDays,
    BigDecimal avgConformityRate,
    Integer suppliersWithGoodPerformance, // score >= 70
    Integer suppliersWithAveragePerformance, // 50 <= score < 70
    Integer suppliersWithPoorPerformance // score < 50
) {}
