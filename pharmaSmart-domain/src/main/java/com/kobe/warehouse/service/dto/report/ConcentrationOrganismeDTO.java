package com.kobe.warehouse.service.dto.report;

public record ConcentrationOrganismeDTO(
    String organisme,
    Long caTp,
    Integer nbFactures,
    Double partPct,
    Integer delaiReglement,
    Long stressImpact30j
) {}
