package com.kobe.warehouse.service.dto.pilotage;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Période fermée [du, au]. */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record PeriodeDTO(LocalDate du, LocalDate au) {
    public long nombreDeJours() {
        return ChronoUnit.DAYS.between(du, au) + 1;
    }
}
