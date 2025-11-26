package com.kobe.warehouse.service.dto.report;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for daily sales summary report from mv_daily_sales_summary materialized view
 */
public record DailySalesSummaryDTO(
    LocalDate saleDate,
    String typeVente,
    Long nbVentes,
    Integer caTotal,
    Integer caNet,
    BigDecimal panierMoyen,
    Integer totalRemises
) {}
