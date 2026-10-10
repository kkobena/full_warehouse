package com.kobe.warehouse.service.dto.pilotage;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.kobe.warehouse.domain.enumeration.AxeAnalyse;

/** Part d'un élément dans l'écart d'un indicateur additif entre la période et sa référence. */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record ContributionDTO(AxeAnalyse axe, String libelleAxe, String cle, String libelle, long valeur, long valeurReference, long ecart) {}
