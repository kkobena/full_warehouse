package com.kobe.warehouse.service.dto.report;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kobe.warehouse.service.dto.enumeration.TypeVenteDTO;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for daily sales summary report from mv_daily_sales_summary materialized view
 */
public record DailySalesSummaryDTO(
    LocalDate saleDate,
    String type,
    Long nbVentes,
    Integer caTotal,
    Integer caNet,
    BigDecimal panierMoyen,
    Integer totalRemises
) {
    @JsonProperty("typeVente")
    public String typeVente() {
        return TypeVenteDTO.valueOf(type).getValue();

    }
}
