package com.kobe.warehouse.service.dto.report;

import com.kobe.warehouse.domain.enumeration.ClassePareto;
import java.math.BigDecimal;

/**
 * DTO pour l'analyse ABC Pareto depuis {@code v_abc_pareto_analysis}.
 * Classification 5 classes basée sur le % du CA cumulé.
 */
public record ABCParetoDTO(
    Integer produitId,
    String libelle,
    String codeCip,
    String famille,
    String classeActuelle,
    Integer caTotal,
    Integer qteVendue,
    Integer nbVentes,
    Integer frequenceMois,
    Long caGlobal,
    Long caCumule,
    BigDecimal contributionPct,
    BigDecimal caCumulePct,
    Integer rang,
    ClassePareto classePareto
) {}
