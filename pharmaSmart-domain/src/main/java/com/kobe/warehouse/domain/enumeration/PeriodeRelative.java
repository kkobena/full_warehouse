package com.kobe.warehouse.domain.enumeration;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;

/** Période d'un modèle d'export, recalculée à chaque exécution : un modèle « mois précédent » rejoué en novembre exporte octobre. */
public enum PeriodeRelative {
    HIER("Hier"),
    SEMAINE_PRECEDENTE("Semaine précédente"),
    MOIS_EN_COURS("Mois en cours"),
    MOIS_PRECEDENT("Mois précédent"),
    TRIMESTRE_PRECEDENT("Trimestre précédent"),
    ANNEE_EN_COURS("Année en cours"),
    ANNEE_PRECEDENTE("Année précédente");

    private final String libelle;

    PeriodeRelative(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }

    /** @return {du, au} */
    public LocalDate[] calculer(LocalDate aujourdhui) {
        return switch (this) {
            case HIER -> new LocalDate[] { aujourdhui.minusDays(1), aujourdhui.minusDays(1) };
            case SEMAINE_PRECEDENTE -> {
                LocalDate lundi = aujourdhui.minusWeeks(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                yield new LocalDate[] { lundi, lundi.plusDays(6) };
            }
            case MOIS_EN_COURS -> new LocalDate[] { aujourdhui.withDayOfMonth(1), aujourdhui };
            case MOIS_PRECEDENT -> {
                LocalDate debut = aujourdhui.minusMonths(1).withDayOfMonth(1);
                yield new LocalDate[] { debut, debut.with(TemporalAdjusters.lastDayOfMonth()) };
            }
            case TRIMESTRE_PRECEDENT -> {
                LocalDate debut = aujourdhui.minusMonths(3).with(IsoFields.DAY_OF_QUARTER, 1);
                yield new LocalDate[] { debut, debut.plusMonths(3).minusDays(1) };
            }
            case ANNEE_EN_COURS -> new LocalDate[] { aujourdhui.withDayOfYear(1), aujourdhui };
            case ANNEE_PRECEDENTE -> new LocalDate[] { aujourdhui.minusYears(1).withDayOfYear(1), aujourdhui.withDayOfYear(1).minusDays(1) };
        };
    }
}
