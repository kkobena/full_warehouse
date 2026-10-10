package com.kobe.warehouse.service.exports.impl;

import com.kobe.warehouse.domain.enumeration.ScheduledReportFrequency;
import com.kobe.warehouse.service.errors.GenericError;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;

/**
 * Prochaine exécution d'un modèle programmé, toujours dans le futur : chaque jour à l'heure dite, chaque semaine au jour dit
 * (1 = lundi), chaque mois au jour dit (1 à 28, pour exister tous les mois).
 */
final class ProgrammationExport {

    static final LocalTime HEURE_PAR_DEFAUT = LocalTime.of(7, 0);
    private static final int JOUR_DU_MOIS_MAX = 28;

    private ProgrammationExport() {}

    static LocalDateTime calculerProchaine(ScheduledReportFrequency frequence, LocalTime heure, Integer jour, LocalDateTime maintenant) {
        LocalTime a = heure == null ? HEURE_PAR_DEFAUT : heure;
        return switch (frequence) {
            case DAILY -> {
                LocalDateTime aujourdhui = maintenant.toLocalDate().atTime(a);
                yield aujourdhui.isAfter(maintenant) ? aujourdhui : aujourdhui.plusDays(1);
            }
            case WEEKLY -> {
                DayOfWeek jourDeLaSemaine = DayOfWeek.of(verifier(jour, 7));
                LocalDateTime candidate = maintenant.toLocalDate().with(TemporalAdjusters.nextOrSame(jourDeLaSemaine)).atTime(a);
                yield candidate.isAfter(maintenant) ? candidate : candidate.plusWeeks(1);
            }
            case MONTHLY -> {
                LocalDateTime candidate = maintenant.toLocalDate().withDayOfMonth(verifier(jour, JOUR_DU_MOIS_MAX)).atTime(a);
                yield candidate.isAfter(maintenant) ? candidate : candidate.plusMonths(1);
            }
            case CUSTOM -> throw new GenericError("Programmation personnalisée non prise en charge pour un export");
        };
    }

    private static int verifier(Integer jour, int max) {
        if (jour == null || jour < 1 || jour > max) {
            throw new GenericError("Le jour de la programmation va de 1 à " + max);
        }
        return jour;
    }
}
