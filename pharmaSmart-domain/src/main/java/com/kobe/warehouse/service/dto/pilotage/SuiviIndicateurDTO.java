package com.kobe.warehouse.service.dto.pilotage;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * Suivi d'un indicateur sur l'année.
 *
 * @param cumul mois clos depuis janvier ; {@code null} pour un taux (un taux ne se cumule pas en additionnant les mois)
 * @param projection {@code null} hors de l'année en cours
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record SuiviIndicateurDTO(IndicateurPilotageDTO indicateur, List<SuiviMoisDTO> mois, SuiviMoisDTO cumul, ProjectionObjectifDTO projection) {}
