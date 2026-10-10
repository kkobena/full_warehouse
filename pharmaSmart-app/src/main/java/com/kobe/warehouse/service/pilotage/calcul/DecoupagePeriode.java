package com.kobe.warehouse.service.pilotage.calcul;

import com.kobe.warehouse.domain.enumeration.Granularite;
import com.kobe.warehouse.service.dto.pilotage.PeriodeDTO;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

/** Tranches d'une période selon la granularité ; la première et la dernière sont rognées aux bornes de la période. */
public final class DecoupagePeriode {

    private DecoupagePeriode() {}

    public static List<PeriodeDTO> decouper(PeriodeDTO periode, Granularite granularite) {
        List<PeriodeDTO> tranches = new ArrayList<>();
        LocalDate debut = periode.du();
        while (!debut.isAfter(periode.au())) {
            LocalDate fin = min(finDeTranche(debut, granularite), periode.au());
            tranches.add(new PeriodeDTO(debut, fin));
            debut = fin.plusDays(1);
        }
        return tranches;
    }

    /** Premier jour de la tranche complète qui contient ce jour. */
    public static LocalDate debutDeTranche(LocalDate jour, Granularite granularite) {
        return switch (granularite) {
            case JOUR -> jour;
            case SEMAINE -> jour.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case MOIS -> jour.withDayOfMonth(1);
            case TRIMESTRE -> jour.with(IsoFields.DAY_OF_QUARTER, 1);
            case ANNEE -> jour.withDayOfYear(1);
        };
    }

    /** Dernier jour de la tranche complète qui contient ce jour. */
    public static LocalDate finDeTranche(LocalDate jour, Granularite granularite) {
        return switch (granularite) {
            case JOUR -> jour;
            case SEMAINE -> jour.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
            case MOIS -> jour.with(TemporalAdjusters.lastDayOfMonth());
            case TRIMESTRE -> jour.with(IsoFields.DAY_OF_QUARTER, 1).plusMonths(3).minusDays(1);
            case ANNEE -> jour.with(TemporalAdjusters.lastDayOfYear());
        };
    }

    private static LocalDate min(LocalDate a, LocalDate b) {
        return a.isBefore(b) ? a : b;
    }
}
