package com.kobe.warehouse.service.dto.pilotage;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.kobe.warehouse.domain.enumeration.IndicateurPilotage;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Objectifs d'un indicateur pour une année : 12 valeurs, {@code null} efface l'objectif du mois. */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record SaisieObjectifsDTO(int annee, @NotNull IndicateurPilotage indicateur, @NotNull @Size(min = 12, max = 12) List<Double> mois) {}
