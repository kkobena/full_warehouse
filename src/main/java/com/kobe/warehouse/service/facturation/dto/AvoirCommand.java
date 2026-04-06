package com.kobe.warehouse.service.facturation.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record AvoirCommand(
    Long factureId,
    LocalDate factureDate,
    Integer tiersPayantId,
    BigDecimal montantAvoir,
    BigDecimal montantTva,
    BigDecimal montantHt,
    String motif,
    List<AvoirLineDto> lignes
) {}
