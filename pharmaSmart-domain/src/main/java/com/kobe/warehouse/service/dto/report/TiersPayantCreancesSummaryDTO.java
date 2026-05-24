package com.kobe.warehouse.service.dto.report;

public record TiersPayantCreancesSummaryDTO(
    Integer groupeTiersPayantId,
    String groupeTiersPayantLibelle,
    Integer nombreFactures,
    Integer montantTotal,
    Integer montantMoinsDe30Jours,
    Integer montantEntre30Et60Jours,
    Integer montantEntre60Et90Jours,
    Integer montantPlusDe90Jours
) {}
