package com.kobe.warehouse.service.facturation.dto;

import com.kobe.warehouse.domain.enumeration.AvoirStatut;
import java.time.LocalDate;
import java.util.List;

public record AvoirSearchParams(
    Integer tiersPayantId,
    LocalDate startDate,
    LocalDate endDate,
    List<AvoirStatut> statuts
) {}
