package com.kobe.warehouse.service.facturation.dto;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public record RecapitulatifMensuelDto(
    int tiersPayantId,
    String tiersPayantName,
    String tiersPayantCode,
    YearMonth periode,
    BigDecimal soldePrecedent,
    BigDecimal totalFacture,
    BigDecimal totalRegle,
    BigDecimal soldeActuel,
    BigDecimal soldeCumule,
    int nombreFactures,
    int nombreImpayees,
    List<RecapitulatifMensuelRow> lignes
) {}
