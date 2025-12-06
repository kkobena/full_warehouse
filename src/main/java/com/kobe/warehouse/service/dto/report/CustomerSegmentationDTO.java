package com.kobe.warehouse.service.dto.report;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for customer segmentation report using RFM analysis from mv_customer_rfm materialized view
 */
public record CustomerSegmentationDTO(
    Integer customerId,
    String customerName,
    String phone,
    LocalDate lastPurchaseDate,
    Integer daysSinceLastPurchase,
    Integer nbPurchasesLastYear,
    Integer totalSpentLastYear,
    BigDecimal avgBasketValue,
    Integer recencyScore,
    Integer frequencyScore,
    Integer monetaryScore,
    Integer rfmSegment,
    CustomerClassification customerClassification
) {

    public enum CustomerClassification {
        CHAMPION,      // Best customers (high RFM)
        LOYAL,         // Frequent buyers
        BIG_SPENDER,   // High monetary value
        ACTIVE,        // Recent customers
        AT_RISK,       // Haven't purchased in 60-90 days
        NEED_ATTENTION,// Inactive but were good customers
        INACTIVE       // Dormant customers
    }
}
