package com.kobe.warehouse.service.dto.report;

import java.math.BigDecimal;

/**
 * DTO for stock valuation summary (aggregated totals)
 */
public record StockValuationSummaryDTO(
    Long totalPurchaseValue,
    Long totalSalesValue,
    Long totalPotentialMargin,
    BigDecimal averageMarginPercentage,
    Integer totalProducts,
    Integer totalQuantity
) {}
