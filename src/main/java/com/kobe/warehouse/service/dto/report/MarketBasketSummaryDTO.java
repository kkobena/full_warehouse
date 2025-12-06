package com.kobe.warehouse.service.dto.report;

import java.math.BigDecimal;

/**
 * Summary statistics for Market Basket Analysis
 */
public record MarketBasketSummaryDTO(
    Long totalTransactions,
    Long totalProducts,
    Long totalAssociations,
    BigDecimal averageBasketSize,
    BigDecimal maxConfidence,
    BigDecimal maxLift,
    String mostFrequentPair
) {}
