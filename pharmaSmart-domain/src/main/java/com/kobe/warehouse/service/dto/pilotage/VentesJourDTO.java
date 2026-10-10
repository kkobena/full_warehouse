package com.kobe.warehouse.service.dto.pilotage;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDate;

/** En-têtes de vente d'un jour, CA de l'officine. */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record VentesJourDTO(
    LocalDate jour,
    long nbVentes,
    long nbVentesAnnulees,
    long caTtc,
    long caHt,
    long remises,
    long partTiersPayant
) {}
