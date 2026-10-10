package com.kobe.warehouse.service.dto.pilotage;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDate;

/** Sommes d'un groupe : les mesures que la source ne connaît pas valent 0. */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record MesuresVentileesDTO(
    String cle1,
    String libelle1,
    String cle2,
    String libelle2,
    LocalDate jour,
    long nbVentes,
    long caTtc,
    long caHt,
    long remise,
    long coutHt,
    long quantite,
    long partTiersPayant
) {}
