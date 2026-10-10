package com.kobe.warehouse.service.dto.pilotage;

import com.fasterxml.jackson.annotation.JsonInclude;
/** Le mois le plus élevé de toutes les années comparées. */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record MoisRecordDTO(int annee, int mois, double valeur) {}
