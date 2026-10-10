package com.kobe.warehouse.service.dto.pilotage;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDate;

/** Valeur du stock photographié en fin de mois, au prix d'achat (projection). */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record MoisStockDTO(LocalDate mois, Long valeur) {}
