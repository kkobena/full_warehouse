package com.kobe.warehouse.service.dto.pilotage;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDate;
import java.util.List;

/** Suivi des objectifs d'une année, réalisé lu jusqu'au jour {@code jusquAu} compris. */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record SuiviObjectifsDTO(int annee, LocalDate jusquAu, List<SuiviIndicateurDTO> indicateurs) {}
