package com.kobe.warehouse.service.pilotage.calcul;

import com.kobe.warehouse.domain.enumeration.SensFavorable;
import com.kobe.warehouse.domain.enumeration.UniteIndicateur;
import com.kobe.warehouse.service.dto.pilotage.rapport.MontantRapportDTO;
import com.kobe.warehouse.service.dto.pilotage.rapport.VariationRapportDTO;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Mise en forme des valeurs du rapport PDF, calquée sur le pipe {@code valeurIndicateur} et {@code app-variation} de l'écran.
 * Espace ordinaire comme séparateur de milliers et signes ASCII : les polices de base du PDF n'ont ni espace fine ni flèches.
 */
public final class FormatRapport {

    private static final String ABSENT = "—";
    private static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String FAVORABLE = "favorable";
    private static final String DEFAVORABLE = "defavorable";
    private static final String NEUTRE = "neutre";

    private FormatRapport() {}

    public static String formaterValeur(Double valeur, UniteIndicateur unite) {
        if (valeur == null || valeur.isNaN() || valeur.isInfinite()) {
            return ABSENT;
        }
        return switch (unite) {
            case MONTANT -> formaterMontantAbrege(valeur);
            case POURCENTAGE -> formaterFixe(valeur, 1) + " %";
            case RATIO -> formaterFixe(valeur, 2);
            default -> valeur == Math.rint(valeur) ? formaterDecimal(valeur, 0) : formaterFixe(valeur, 1);
        };
    }

    /** Pour un tableau : montant complet (« 19 194 652 »), le reste comme {@link #formaterValeur}. */
    public static String formaterValeurComplete(Double valeur, UniteIndicateur unite) {
        if (unite != UniteIndicateur.MONTANT || valeur == null || valeur.isNaN() || valeur.isInfinite()) {
            return formaterValeur(valeur, unite);
        }
        return formaterDecimal(valeur, 0);
    }

    public static String formaterDate(LocalDate jour) {
        return jour == null ? ABSENT : jour.format(JOUR);
    }

    /** « 19,2 M », « 850 k », « 940 ». */
    public static String formaterMontantAbrege(double valeur) {
        return formaterMontantAbrege(valeur, 1, 0);
    }

    /** Graduation d'axe : une décimale de plus aux millions et aux milliers, sinon deux graduations proches s'écriraient pareil. */
    public static String formaterGraduation(double valeur, UniteIndicateur unite) {
        return unite == UniteIndicateur.MONTANT ? formaterMontantAbrege(valeur, 2, 1) : formaterValeur(valeur, unite);
    }

    private static String formaterMontantAbrege(double valeur, int decimalesMillions, int decimalesMilliers) {
        double absolu = Math.abs(valeur);
        if (absolu >= 1_000_000) {
            return formaterDecimal(valeur / 1_000_000, decimalesMillions) + " M";
        }
        if (absolu >= 10_000) {
            return formaterDecimal(valeur / 1_000, decimalesMilliers) + " k";
        }
        return formaterDecimal(valeur, 0);
    }

    /** Un taux varie en points, le reste en % de la référence ; « n.s. » sans référence comparable. */
    public static VariationRapportDTO formaterVariation(Double ecart, Double ecartPct, UniteIndicateur unite, SensFavorable sens) {
        boolean enPoints = unite == UniteIndicateur.POURCENTAGE;
        Double valeur = enPoints ? ecart : ecartPct;
        if (valeur == null || valeur.isNaN() || valeur.isInfinite()) {
            return new VariationRapportDTO("n.s.", NEUTRE);
        }
        String texte = lireSigne(valeur) + formaterPoints(valeur) + (enPoints ? " pt" : " %");
        return new VariationRapportDTO(texte, apprecier(valeur, sens));
    }

    /** Valeur absolue à une décimale, toujours écrite (« 0,6 », « 12,0 ») : les variations et points de l'écran. */
    public static String formaterPoints(double valeur) {
        return formaterFixe(Math.abs(valeur), 1);
    }

    /** Écart de CA signé et abrégé : une hausse est favorable. */
    public static MontantRapportDTO formaterEcart(String libelle, double ecart) {
        return new MontantRapportDTO(libelle, lireSigne(ecart) + formaterMontantAbrege(Math.abs(ecart)), apprecier(ecart, SensFavorable.HAUSSE));
    }

    private static String apprecier(double valeur, SensFavorable sens) {
        if (valeur == 0 || sens == SensFavorable.NEUTRE) {
            return NEUTRE;
        }
        return (valeur > 0) == (sens == SensFavorable.HAUSSE) ? FAVORABLE : DEFAVORABLE;
    }

    private static String lireSigne(double valeur) {
        return valeur > 0 ? "+ " : valeur < 0 ? "- " : "= ";
    }

    /** Au plus {@code decimales} décimales (« 19,2 M », « 19 M ») : les montants abrégés de l'écran. */
    private static String formaterDecimal(double valeur, int decimales) {
        return lireFormat(0, decimales).format(valeur);
    }

    /** Exactement {@code decimales} décimales (« 31,0 % », « 4,00 ») : les taux et ratios de l'écran. */
    private static String formaterFixe(double valeur, int decimales) {
        return lireFormat(decimales, decimales).format(valeur);
    }

    private static DecimalFormat lireFormat(int minimum, int maximum) {
        DecimalFormatSymbols symboles = DecimalFormatSymbols.getInstance(Locale.FRENCH);
        symboles.setGroupingSeparator(' ');
        DecimalFormat format = new DecimalFormat("#,##0", symboles);
        format.setMinimumFractionDigits(minimum);
        format.setMaximumFractionDigits(maximum);
        format.setRoundingMode(RoundingMode.HALF_UP);
        return format;
    }
}
