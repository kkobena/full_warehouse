package com.kobe.warehouse.service.dto.report;

import java.math.BigDecimal;

/**
 * DTO for aggregated ABC Pareto summary from mv_pareto_summary materialized view
 * Provides distribution metrics for Pareto classification
 */
public record ABCParetoSummaryDTO(
    Integer totalProduits,
    Long caGlobal,
    // Classe A (80% du CA)
    Integer nbProduitsA,
    Long caClasseA,
    BigDecimal pctCaClasseA,
    // Classe B (15% du CA)
    Integer nbProduitsB,
    Long caClasseB,
    BigDecimal pctCaClasseB,
    // Classe C (5% du CA)
    Integer nbProduitsC,
    Long caClasseC,
    BigDecimal pctCaClasseC
) {}
