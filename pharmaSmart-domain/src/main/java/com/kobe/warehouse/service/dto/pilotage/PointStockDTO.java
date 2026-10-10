package com.kobe.warehouse.service.dto.pilotage;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDate;

/** Valeur du stock en fin de mois et au même mois de l'année précédente (photographies). */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record PointStockDTO(LocalDate mois, Long valeur, Long valeurN1) {}
