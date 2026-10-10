package com.kobe.warehouse.service.dto.pilotage;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDate;

/** Un montant journalier (achats, encaissements). */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record MontantJourDTO(LocalDate jour, long montant) {}
