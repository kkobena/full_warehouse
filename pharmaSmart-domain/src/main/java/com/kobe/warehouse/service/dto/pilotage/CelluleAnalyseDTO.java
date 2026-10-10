package com.kobe.warehouse.service.dto.pilotage;

import com.fasterxml.jackson.annotation.JsonInclude;
/** Valeur d'un indicateur, sa référence et l'écart ; un taux varie en points ({@code ecart}), jamais en %. */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record CelluleAnalyseDTO(Double valeur, Double valeurReference, Double ecart, Double ecartPct) {}
