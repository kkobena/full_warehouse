package com.kobe.warehouse.service.facturation.dto;

import com.kobe.warehouse.domain.enumeration.InvoiceStatut;
import java.time.LocalDate;
import java.util.Set;

public record RapprochementParams(
    Integer tiersPayantId,
    LocalDate startDate,
    LocalDate endDate,
    Set<InvoiceStatut> statuts
) {}
