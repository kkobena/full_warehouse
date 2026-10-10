package com.kobe.warehouse.service.pilotage.calcul;

import com.kobe.warehouse.domain.enumeration.IndicateurPilotage;
import com.kobe.warehouse.domain.enumeration.UniteIndicateur;
import com.kobe.warehouse.service.dto.pilotage.CelluleAnalyseDTO;

/** Écart d'un indicateur à sa référence, une seule définition pour les séries et l'analyse. */
public final class Variations {

    private static final double CENT = 100.0;

    private Variations() {}

    public static Double ecart(Double valeur, Double valeurReference) {
        return valeur == null || valeurReference == null ? null : valeur - valeurReference;
    }

    /** En % de la référence ; jamais pour un taux, qui varie en points ({@link #ecart}). */
    public static Double ecartPct(IndicateurPilotage indicateur, Double valeur, Double valeurReference) {
        return ecartPct(indicateur.getUnite(), valeur, valeurReference);
    }

    public static Double ecartPct(UniteIndicateur unite, Double valeur, Double valeurReference) {
        if (unite == UniteIndicateur.POURCENTAGE || valeur == null || valeurReference == null || valeurReference == 0) {
            return null;
        }
        return (valeur - valeurReference) * CENT / Math.abs(valeurReference);
    }

    /** Part d'une valeur dans un total (%) ; {@code null} si le total est nul. */
    public static Double part(Double valeur, Double total) {
        return valeur == null || total == null || total == 0 ? null : valeur * CENT / total;
    }

    /** @param avecReference sans période de référence, la cellule n'a ni référence ni écart */
    public static CelluleAnalyseDTO cellule(IndicateurPilotage indicateur, MesuresComparees mesures, boolean avecReference) {
        Double valeur = CalculateurIndicateurs.calculer(indicateur, mesures.periode());
        Double valeurReference = avecReference ? CalculateurIndicateurs.calculer(indicateur, mesures.reference()) : null;
        return cellule(indicateur, valeur, valeurReference);
    }

    public static CelluleAnalyseDTO cellule(IndicateurPilotage indicateur, Double valeur, Double valeurReference) {
        return cellule(indicateur.getUnite(), valeur, valeurReference);
    }

    /** Pour une mesure hors dictionnaire (part des ventes remisées, remise moyenne…), d'unité connue. */
    public static CelluleAnalyseDTO cellule(UniteIndicateur unite, Double valeur, Double valeurReference) {
        return new CelluleAnalyseDTO(valeur, valeurReference, ecart(valeur, valeurReference), ecartPct(unite, valeur, valeurReference));
    }
}
