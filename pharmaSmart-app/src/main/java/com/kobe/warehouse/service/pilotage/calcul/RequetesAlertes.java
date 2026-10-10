package com.kobe.warehouse.service.pilotage.calcul;

import com.kobe.warehouse.domain.enumeration.Granularite;
import com.kobe.warehouse.domain.enumeration.IndicateurPilotage;
import com.kobe.warehouse.domain.enumeration.TypeComparaison;
import com.kobe.warehouse.service.dto.pilotage.RequetePilotageDTO;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

/** Périodes lues par les alertes : le mois en cours face au même mois N-1 à date, ou les derniers jours. */
public final class RequetesAlertes {

    private RequetesAlertes() {}

    public static RequetePilotageDTO lireMoisADate(LocalDate aujourdhui, List<IndicateurPilotage> indicateurs) {
        return new RequetePilotageDTO(aujourdhui.withDayOfMonth(1), aujourdhui, TypeComparaison.MEME_PERIODE_N_1, 1, null, null, true, Granularite.MOIS, indicateurs);
    }

    /** Les {@code jours} derniers jours, aujourd'hui compris, face aux mêmes jours N-1 (sept jours couvrent chaque jour de la semaine une fois). */
    public static RequetePilotageDTO lireDerniersJours(LocalDate aujourdhui, int jours, TypeComparaison comparaison, List<IndicateurPilotage> indicateurs) {
        return new RequetePilotageDTO(aujourdhui.minusDays(jours - 1L), aujourdhui, comparaison, 1, null, null, false, Granularite.JOUR, indicateurs);
    }

    /** « 12,3 » : une décimale, virgule française. */
    public static String formaterDecimal(double valeur) {
        return String.format(Locale.FRENCH, "%.1f", valeur);
    }
}
