package com.kobe.warehouse.service.dto.report;

import com.kobe.warehouse.domain.enumeration.CategorieABC;
import java.math.BigDecimal;

/**
 * DTO for stock rotation report from mv_stock_rotation materialized view
 * Uses ABC analysis with statistical Z-score for classification
 */
public record StockRotationDTO(
    Integer produitId,
    String libelle,
    String codeCip,
    String categorie,
    Integer stockQuantity,
    Integer unitCost,
    Long stockValue,
    Integer caLast30Days,
    Integer qtySoldLast30Days,
    Integer nbSalesLast30Days,
    Integer caLast12Months,
    Integer qtySoldLast12Months,
    BigDecimal rotationRateAnnual,
    Integer avgDaysInStock,
    CategorieABC categorieABC
) {}
