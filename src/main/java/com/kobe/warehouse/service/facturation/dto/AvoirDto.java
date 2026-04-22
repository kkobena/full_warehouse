package com.kobe.warehouse.service.facturation.dto;

import com.kobe.warehouse.domain.enumeration.AvoirStatut;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record AvoirDto(
    Long id,
    String numAvoir,
    Long factureOrigineId,
    LocalDate factureOrigineDate,
    String factureNum,
    BigDecimal montantAvoir,
    BigDecimal montantTva,
    BigDecimal montantHt,
    String motif,
    LocalDate avoirDate,
    AvoirStatut statut,
    Integer tiersPayantId,
    String tiersPayantName,
    List<AvoirLineDto> lignes
) {}
