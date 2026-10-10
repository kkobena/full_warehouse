package com.kobe.warehouse.service.dto.pilotage;

import com.fasterxml.jackson.annotation.JsonInclude;
/** Période analysée et sa référence ({@code null} sans comparaison) ; {@code aDate} : période en cours arrêtée à aujourd'hui. */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record ComparaisonPeriodesDTO(PeriodeDTO periode, PeriodeDTO reference, boolean aDate) {}
