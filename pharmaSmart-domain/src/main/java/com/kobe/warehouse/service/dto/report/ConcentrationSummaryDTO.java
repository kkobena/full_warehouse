package com.kobe.warehouse.service.dto.report;

import java.util.List;

public record ConcentrationSummaryDTO(
    List<ConcentrationOrganismeDTO> organismes,
    Long totalCaTp,
    Long totalRegle,
    Long totalImpaye,
    Integer hhiIndex,
    String riskLevel
) {}
