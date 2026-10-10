package com.kobe.warehouse.service.dto.pilotage;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.kobe.warehouse.domain.enumeration.SourceAnalyse;
import java.util.List;

/**
 * Résultat de l'onglet Analyser.
 *
 * @param indicateursIgnores demandés, mais que la source retenue ne sait pas calculer (le nombre de ventes par famille…)
 * @param autres les éléments au-delà du top, regroupés ; {@code null} s'il n'y en a pas
 * @param croise {@code null} sans second axe
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record AnalysePilotageDTO(
    ComparaisonPeriodesDTO comparaison,
    SourceAnalyse source,
    List<IndicateurPilotageDTO> indicateurs,
    List<IndicateurPilotageDTO> indicateursIgnores,
    AxeAnalyseDTO axe,
    AxeAnalyseDTO axe2,
    List<CelluleAnalyseDTO> total,
    List<ElementAnalyseDTO> elements,
    ElementAnalyseDTO autres,
    int nombreElements,
    CroiseAnalyseDTO croise
) {}
