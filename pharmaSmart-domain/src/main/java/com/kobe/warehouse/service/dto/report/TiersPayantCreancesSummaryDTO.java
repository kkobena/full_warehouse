package com.kobe.warehouse.service.dto.report;

/**
 * Synthèse des créances d'un groupe de tiers payants : un encours et sa ventilation par ancienneté.
 *
 * @param montantEncours reste dû, et non le TTC facturé : il vaut la somme des quatre tranches
 */
public record TiersPayantCreancesSummaryDTO(
    Integer groupeTiersPayantId,
    String groupeTiersPayantLibelle,
    Integer nombreFactures,
    Integer montantEncours,
    Integer montantMoinsDe30Jours,
    Integer montantEntre30Et60Jours,
    Integer montantEntre60Et90Jours,
    Integer montantPlusDe90Jours
) {}
