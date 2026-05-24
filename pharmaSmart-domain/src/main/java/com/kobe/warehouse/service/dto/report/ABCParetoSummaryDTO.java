package com.kobe.warehouse.service.dto.report;

import java.math.BigDecimal;

/**
 * DTO de synthèse de l'analyse ABC Pareto — 5 classes.
 * Calculé inline depuis {@code v_abc_pareto_analysis} (mv_pareto_summary supprimée).
 */
public record ABCParetoSummaryDTO(
    Integer totalProduits,
    Long caGlobal,
    // Classe A_PLUS
    Integer nbProduitsAPlus,
    Long caClasseAPlus,
    BigDecimal pctCaClasseAPlus,
    // Classe A
    Integer nbProduitsA,
    Long caClasseA,
    BigDecimal pctCaClasseA,
    // Classe B
    Integer nbProduitsB,
    Long caClasseB,
    BigDecimal pctCaClasseB,
    // Classe C
    Integer nbProduitsC,
    Long caClasseC,
    BigDecimal pctCaClasseC,
    // Classe D
    Integer nbProduitsD,
    Long caClasseD,
    BigDecimal pctCaClasseD
) {}
