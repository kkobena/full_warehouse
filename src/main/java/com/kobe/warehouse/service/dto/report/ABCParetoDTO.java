package com.kobe.warehouse.service.dto.report;

import com.kobe.warehouse.domain.enumeration.ClassePareto;
import java.math.BigDecimal;

/**
 * DTO for ABC Pareto analysis from mv_abc_pareto_analysis materialized view
 * Classification based on 80/20 rule (cumulative revenue contribution)
 */
public record ABCParetoDTO(
    Integer produitId,
    String libelle,
    String codeCip,
    String categorie,
    Integer caTotal,
    Integer qteVendue,
    Integer nbVentes,
    Long caGlobal,
    Long caCumule,
    BigDecimal contributionPct,
    BigDecimal caCumulePct,
    ClassePareto classePareto,
    Integer rang
) {}
