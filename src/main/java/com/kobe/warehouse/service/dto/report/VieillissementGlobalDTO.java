package com.kobe.warehouse.service.dto.report;

public record VieillissementGlobalDTO(
    Long totalEncours,
    Long tranche0_30,
    Long tranche31_60,
    Long tranche61_90,
    Long tranche90Plus,
    Long nbFactures,
    Long nbEnRetard
) {}
