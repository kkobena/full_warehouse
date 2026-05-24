package com.kobe.warehouse.service.dto.report;

import java.time.LocalDate;

public record ClientRetentionRowDTO(
    long clientId,
    String nom,
    LocalDate premiereVisite,
    LocalDate derniereVisite,
    int nbAchats,
    long caTotal,
    int joursAbsence
) {
    public String segment() {
        if (joursAbsence <= 30)  return "ACTIF";
        if (joursAbsence <= 90)  return "RISQUE";
        return "PERDU";
    }
}
