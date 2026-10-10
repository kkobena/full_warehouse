package com.kobe.warehouse.service.dto.pilotage;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Objectifs d'un indicateur sur une année.
 *
 * @param mois 12 valeurs, {@code null} pour un mois sans objectif
 * @param modifiePar dernier à avoir modifié la ligne ; {@code null} si aucun objectif
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record LigneObjectifsDTO(IndicateurPilotageDTO indicateur, List<Double> mois, String modifiePar, LocalDateTime modifieLe) {}
