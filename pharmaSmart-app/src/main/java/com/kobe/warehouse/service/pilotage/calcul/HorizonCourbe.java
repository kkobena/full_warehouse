package com.kobe.warehouse.service.pilotage.calcul;

import com.kobe.warehouse.domain.enumeration.Granularite;
import com.kobe.warehouse.domain.enumeration.IndicateurPilotage;
import com.kobe.warehouse.service.dto.pilotage.RequetePilotageDTO;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

/** Pendant serveur de {@code horizon-courbe.ts} et {@code libelle-reference.ts} : le PDF montre la même courbe que l'écran. */
public final class HorizonCourbe {

    private static final int TRANCHES = 13;
    private static final int ANNEES = 6;

    private HorizonCourbe() {}

    /** La requête élargie aux 13 dernières tranches (6 pour l'année), jamais rétrécie, pour un seul indicateur. */
    public static RequetePilotageDTO elargirAuxTreizeTranches(RequetePilotageDTO requete, IndicateurPilotage indicateur) {
        LocalDate debut = lireDebutHorizon(requete.au(), requete.granularite());
        return new RequetePilotageDTO(
            debut.isBefore(requete.du()) ? debut : requete.du(),
            requete.au(),
            requete.comparaison(),
            requete.anneesEnArriere(),
            requete.referenceDu(),
            requete.referenceAu(),
            requete.aDate(),
            requete.granularite(),
            List.of(indicateur)
        );
    }

    public static String libellerHorizon(Granularite granularite) {
        return switch (granularite) {
            case JOUR -> "13 derniers jours";
            case SEMAINE -> "13 dernières semaines";
            case MOIS -> "13 derniers mois";
            case TRIMESTRE -> "13 derniers trimestres";
            case ANNEE -> ANNEES + " dernières années";
        };
    }

    public static String libellerReference(RequetePilotageDTO requete) {
        int annees = requete.anneesEnArriere();
        return switch (requete.comparaison()) {
            case MEME_PERIODE_N_1 -> "Même période, année précédente (N-1)";
            case ANNEE_N_MOINS_K -> "Même période, il y a " + annees + " an" + (annees > 1 ? "s" : "") + " (N-" + annees + ")";
            case PERIODE_PRECEDENTE -> "Période précédente (juste avant)";
            case PERSONNALISEE -> "Période de comparaison choisie";
            case OBJECTIF -> "Objectif";
            case AUCUNE -> "Référence";
        };
    }

    private static LocalDate lireDebutHorizon(LocalDate fin, Granularite granularite) {
        return switch (granularite) {
            case JOUR -> fin.minusDays(TRANCHES - 1);
            case SEMAINE -> fin.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(TRANCHES - 1);
            case MOIS -> fin.withDayOfMonth(1).minusMonths(TRANCHES - 1);
            case TRIMESTRE -> fin.withDayOfMonth(1).minusMonths((fin.getMonthValue() - 1) % 3).minusMonths((TRANCHES - 1) * 3L);
            case ANNEE -> fin.withDayOfYear(1).minusYears(ANNEES - 1);
        };
    }
}
