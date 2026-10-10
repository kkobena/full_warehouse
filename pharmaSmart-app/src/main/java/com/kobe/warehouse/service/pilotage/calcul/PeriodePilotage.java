package com.kobe.warehouse.service.pilotage.calcul;

import com.kobe.warehouse.service.dto.pilotage.PeriodeDTO;
import com.kobe.warehouse.service.errors.GenericError;
import java.time.LocalDate;

/** Contrôle commun des périodes demandées au pilotage. */
public final class PeriodePilotage {

    private PeriodePilotage() {}

    public static PeriodeDTO verifier(LocalDate du, LocalDate au) {
        if (du == null || au == null) {
            throw new GenericError("La période doit avoir une date de début et une date de fin");
        }
        if (au.isBefore(du)) {
            throw new GenericError("La date de fin précède la date de début");
        }
        return new PeriodeDTO(du, au);
    }
}
