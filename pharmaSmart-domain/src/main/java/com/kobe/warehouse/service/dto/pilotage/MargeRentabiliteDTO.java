package com.kobe.warehouse.service.dto.pilotage;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * Sous-section Marge de « Rentabilité & remises ».
 *
 * @param effetMix part de la variation du taux de marge (points) due au mix : plus ou moins de CA sur des éléments à fort taux
 * @param effetTaux part due aux taux de chaque élément ; effetMix + effetTaux = variation du taux
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record MargeRentabiliteDTO(
    ComparaisonPeriodesDTO comparaison,
    AxeAnalyseDTO axe,
    Double tauxMarge,
    Double tauxMargeReference,
    Double effetMix,
    Double effetTaux,
    List<LigneMargeDTO> lignes,
    int seuilFaibleMarge,
    List<LigneMargeDTO> faiblesMarges,
    List<VenteMargeDTO> ventesAMargeNegative
) {}
