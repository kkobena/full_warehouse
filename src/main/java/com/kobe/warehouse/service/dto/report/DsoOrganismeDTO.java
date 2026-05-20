package com.kobe.warehouse.service.dto.report;

public record DsoOrganismeDTO(
    String organisme,
    Long encours,
    Long tranche0_30,
    Long tranche31_60,
    Long tranche61_90,
    Long tranche90Plus,
    Long nbFactures,
    Long nbEnRetard,
    Integer dsoJours,
    Integer delaiReglement,
    String fiabilite
) {}
