package com.kobe.warehouse.service.dto.report;

import java.math.BigDecimal;

/**
 * DTO for supplier performance report from mv_supplier_performance materialized view
 */
public record SupplierPerformanceDTO(
    Integer fournisseurId,
    String fournisseurName,
    String fournisseurCode,
    String phone,
    String mobile,
    Integer nbOrdersLast30Days,
    Long purchaseAmountLast30Days,
    Integer nbOrdersLast12Months,
    Long purchaseAmountLast12Months,
    Integer avgDeliveryDays,
    Integer minDeliveryDays,
    Integer maxDeliveryDays,
    BigDecimal conformityRatePct,
    BigDecimal performanceScore
) {}
