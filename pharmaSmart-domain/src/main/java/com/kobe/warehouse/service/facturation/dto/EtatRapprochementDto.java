package com.kobe.warehouse.service.facturation.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record EtatRapprochementDto(
    String tiersPayantName,
    LocalDate debutPeriode,
    LocalDate finPeriode,
    BigDecimal totalFacture,
    BigDecimal totalRegle,
    BigDecimal ecartTotal,
    List<LigneRapprochementDto> lignes
) {}
