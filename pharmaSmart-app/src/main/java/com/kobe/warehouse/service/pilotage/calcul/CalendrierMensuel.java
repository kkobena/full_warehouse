package com.kobe.warehouse.service.pilotage.calcul;

import com.kobe.warehouse.domain.enumeration.ModeAnnees;
import java.util.ArrayList;
import java.util.List;

/**
 * Mesures mois par mois depuis janvier d'une année de départ, rangées en sommes préfixées : le mois, le cumul depuis janvier et
 * les douze mois glissants se lisent chacun en une soustraction. Les mois non encore écoulés n'existent pas.
 */
public final class CalendrierMensuel {

    private static final int MOIS_PAR_AN = 12;

    private final int anneeDeDepart;
    /** {@code cumuls.get(i)} : somme des {@code i} premiers mois. */
    private final List<MesuresPilotage> cumuls;

    private CalendrierMensuel(int anneeDeDepart, List<MesuresPilotage> cumuls) {
        this.anneeDeDepart = anneeDeDepart;
        this.cumuls = cumuls;
    }

    /** @param mois une entrée par mois, de janvier de {@code anneeDeDepart} au mois en cours */
    public static CalendrierMensuel ranger(int anneeDeDepart, List<MesuresPilotage> mois) {
        List<MesuresPilotage> cumuls = new ArrayList<>(mois.size() + 1);
        MesuresPilotage cumul = MesuresPilotage.AUCUNE;
        cumuls.add(cumul);
        for (MesuresPilotage mesures : mois) {
            cumul = cumul.plus(mesures);
            cumuls.add(cumul);
        }
        return new CalendrierMensuel(anneeDeDepart, cumuls);
    }

    public boolean existe(int annee, int mois) {
        int rang = rang(annee, mois);
        return rang >= 0 && rang < cumuls.size() - 1;
    }

    /** Mesures de la fenêtre qui se termine au mois donné, selon le mode. */
    public MesuresPilotage lireFenetre(int annee, int mois, ModeAnnees mode) {
        int fin = rang(annee, mois);
        int debut = switch (mode) {
            case MENSUEL -> fin;
            case CUMULE -> rang(annee, 1);
            case GLISSANT -> fin - (MOIS_PAR_AN - 1);
        };
        return sommer(debut, fin);
    }

    /** Mesures des mois {@code premier} à {@code dernier} de l'année, bornées aux mois écoulés. */
    public MesuresPilotage lireMois(int annee, int premier, int dernier) {
        return sommer(rang(annee, premier), rang(annee, dernier));
    }

    private MesuresPilotage sommer(int debut, int fin) {
        int borneHaute = Math.min(fin + 1, cumuls.size() - 1);
        int borneBasse = Math.max(debut, 0);
        return borneHaute <= borneBasse ? MesuresPilotage.AUCUNE : cumuls.get(borneHaute).moins(cumuls.get(borneBasse));
    }

    private int rang(int annee, int mois) {
        return (annee - anneeDeDepart) * MOIS_PAR_AN + mois - 1;
    }
}
