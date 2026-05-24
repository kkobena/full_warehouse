package com.kobe.warehouse.service.facturation.dto;

import com.kobe.warehouse.domain.enumeration.InvoiceStatut;
import java.time.LocalDate;
import java.util.Set;

public record InvoiceSearchParams(
    LocalDate startDate,
    LocalDate endDate,
    Set<Integer> groupIds,
    Set<Integer> tiersPayantIds,
    boolean factureProvisoire,
    Set<InvoiceStatut> statuts,
    String search
) {}
