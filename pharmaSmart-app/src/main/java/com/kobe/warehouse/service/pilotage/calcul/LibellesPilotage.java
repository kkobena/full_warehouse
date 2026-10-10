package com.kobe.warehouse.service.pilotage.calcul;

import com.kobe.warehouse.domain.enumeration.Granularite;
import com.kobe.warehouse.service.dto.pilotage.PeriodeDTO;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.IsoFields;
import java.util.Locale;

/** Mise en forme des libellés calculés (tranches de période, mois, jours de la semaine). */
public final class LibellesPilotage {

    private static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter JOUR_MOIS = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter MOIS_ANNEE = DateTimeFormatter.ofPattern("MMM yyyy", Locale.FRENCH);
    private static final DateTimeFormatter MOIS_COURT = DateTimeFormatter.ofPattern("MMM", Locale.FRENCH);

    private LibellesPilotage() {}

    public static String majuscule(String texte) {
        return Character.toUpperCase(texte.charAt(0)) + texte.substring(1);
    }

    /** « Janv. », « Févr. »… */
    public static String libellerMoisCourt(LocalDate jour) {
        return majuscule(jour.format(MOIS_COURT));
    }

    /**
     * « Oct. 2026 », « T4 2026 », « 2026 », « Sem. du 05/10 », « 10/10/2026 » ; une tranche rognée par la période le dit :
     * « Oct. 2026 (au 10) ».
     */
    public static String libellerTranche(PeriodeDTO tranche, Granularite granularite) {
        LocalDate debutNaturel = DecoupagePeriode.debutDeTranche(tranche.du(), granularite);
        boolean debutRogne = tranche.du().isAfter(debutNaturel);
        boolean finRognee = tranche.au().isBefore(DecoupagePeriode.finDeTranche(tranche.du(), granularite));
        String libelle = libellerDebut(debutNaturel, granularite);
        if (debutRogne && finRognee) {
            return libelle + " (du " + libellerJour(tranche.du(), granularite) + " au " + libellerJour(tranche.au(), granularite) + ")";
        }
        if (finRognee) {
            return libelle + " (au " + libellerJour(tranche.au(), granularite) + ")";
        }
        return debutRogne ? libelle + " (dès le " + libellerJour(tranche.du(), granularite) + ")" : libelle;
    }

    private static String libellerDebut(LocalDate debut, Granularite granularite) {
        return switch (granularite) {
            case JOUR -> debut.format(JOUR);
            case SEMAINE -> "Sem. du " + debut.format(JOUR_MOIS);
            case MOIS -> majuscule(debut.format(MOIS_ANNEE));
            case TRIMESTRE -> "T" + debut.get(IsoFields.QUARTER_OF_YEAR) + " " + debut.getYear();
            case ANNEE -> String.valueOf(debut.getYear());
        };
    }

    /** Dans un mois, le numéro du jour suffit ; ailleurs il faut aussi le mois. */
    private static String libellerJour(LocalDate jour, Granularite granularite) {
        return granularite == Granularite.MOIS ? String.valueOf(jour.getDayOfMonth()) : jour.format(JOUR_MOIS);
    }
}
