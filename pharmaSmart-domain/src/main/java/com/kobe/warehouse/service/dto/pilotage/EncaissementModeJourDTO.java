package com.kobe.warehouse.service.dto.pilotage;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDate;

/** Encaissé un jour, pour un mode de paiement (projection). */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record EncaissementModeJourDTO(LocalDate jour, String mode, String libelle, Long montant) {}
