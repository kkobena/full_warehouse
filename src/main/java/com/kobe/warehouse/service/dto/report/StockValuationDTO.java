package com.kobe.warehouse.service.dto.report;

import java.math.BigDecimal;

/**
 * DTO for stock valuation report from mv_stock_valuation materialized view
 */
public record StockValuationDTO(
    Integer produitId,
    String libelle,
    String codeCip,
    String categorie,
    String storageLocation,
    Integer stockQuantity,
    Integer purchasePrice,
    Integer salesPrice,
    Long totalPurchaseValue,
    Long totalSalesValue,
    Long potentialMargin,
    BigDecimal marginPercentage
) {}
