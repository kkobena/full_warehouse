package com.kobe.warehouse.service.utils;

import com.kobe.warehouse.domain.PaymentMode;
import com.kobe.warehouse.domain.enumeration.ModePaimentCode;
import com.kobe.warehouse.service.stock.dto.PeremptionStatut;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class ServiceUtil {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    public static PaymentMode getPaymentMode(String code) {
        PaymentMode paymentMode = new PaymentMode();
        paymentMode.setCode(code);
        return paymentMode;
    }

    public static Integer dateDimensionKey(LocalDate date) {
        return Integer.valueOf(date.format(DateTimeFormatter.ofPattern("yyyyMMdd")));
    }

    /**
     * Montant hors taxe correspondant à un montant TTC, pour un taux de TVA exprimé en pourcentage
     * entier (0, 9, 18…).
     *
     * <p>Pendant Java de la fonction SQL {@code ht_from_ttc} (cf.
     * {@code V1.9.0__fix_tva_division_entiere.sql}) : les deux doivent rendre la même valeur, faute
     * de quoi le rapport TVA et la facture normalisée cesseraient de se recouper.
     *
     * <p>Le calcul est mené en {@link BigDecimal} et non en {@code double} : sur des montants qui
     * alimentent une déclaration fiscale, l'arithmétique binaire approchée n'a pas sa place. Le
     * quotient est conservé à 10 décimales, l'arrondi étant à la charge de l'appelant — arrondir ici
     * puis sommer accumulerait les erreurs ligne à ligne.
     *
     * @param ttc montant toutes taxes comprises
     * @param tauxTva taux en pourcentage entier ; {@code 0} rend {@code ttc} inchangé
     * @throws IllegalArgumentException si le taux rend le diviseur nul (taux de -100 %)
     */
    public static BigDecimal htFromTtc(BigDecimal ttc, int tauxTva) {
        BigDecimal diviseur = BigDecimal.ONE.add(BigDecimal.valueOf(tauxTva).movePointLeft(2));
        if (diviseur.signum() == 0) {
            throw new IllegalArgumentException("Taux de TVA invalide : " + tauxTva);
        }
        return ttc.divide(diviseur, 10, RoundingMode.HALF_UP);
    }

    /** Variante entière : le montant HT est arrondi au franc le plus proche. */
    public static long htFromTtc(long ttc, int tauxTva) {
        return htFromTtc(BigDecimal.valueOf(ttc), tauxTva).setScale(0, RoundingMode.HALF_UP).longValue();
    }

    public static int arrondiTauxCouverture(int taux) {

        int arrondi = Math.round(taux / 5f) * 5;

        return Math.min(100, arrondi);

    }
    public static double calculHt(int ttc, int tva) {
        return htFromTtc(BigDecimal.valueOf(ttc), tva).doubleValue();
    }
    public static boolean isPaymentMode(String modePayment) {
        return (
            ModePaimentCode.MTN.name().equalsIgnoreCase(modePayment) ||
            ModePaimentCode.MOOV.name().equalsIgnoreCase(modePayment) ||
            ModePaimentCode.OM.name().equalsIgnoreCase(modePayment) ||
            ModePaimentCode.WAVE.name().equalsIgnoreCase(modePayment)
        );
    }
    public static String buildCodeCip(String initialCodeCip) {

        // If null or empty → generate 8-digit numeric code
        if (!StringUtils.hasLength(initialCodeCip)) {
            return generateNumeric(8);
        }

        // If length < 7 → append 2 random digits
        if (initialCodeCip.length() < 7) {
            return initialCodeCip + String.format("%02d", SECURE_RANDOM.nextInt(100));
        }

        return initialCodeCip;
    }

    private static String generateNumeric(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(SECURE_RANDOM.nextInt(10));
        }
        return sb.toString();
    }
    /**
     * Calcule le montant effectivement payé par le client en CFA (sans centime).
     *
     * <p>Règles appliquées :
     * <ol>
     *   <li>Si {@code montantVerse >= montantAttendu}, le client couvre la totalité → on retourne
     *       {@code montantAttendu} tel quel (pas de rendu de monnaie à gérer côté vente).</li>
     *   <li>Sinon, on arrondit {@code montantVerse} au multiple de 5 le plus proche, car le CFA ne
     *       possède pas de coupure inférieure à 5.</li>
     * </ol>
     *
     * @param montantVerse   montant remis par le client (en CFA, entier)
     * @param montantAttendu montant total de la vente (en CFA, entier)
     * @return montant retenu comme paiement effectif, arrondi au multiple de 5 le plus proche
     */
    public static int resoudreMontantPaye(int montantVerse, int montantAttendu) {
        if (montantVerse >= montantAttendu) {
            return montantAttendu;
        }
        return arrondirAuMultipleDe5(montantVerse);
    }


    /**
     * Arrondit {@code valeur} au multiple de 5 le plus proche (0, 5, 10, 15, …).
     *
     * <p>Exemples : 7 → 5 · 8 → 10 · 12 → 10 · 13 → 15</p>
     *
     * @param valeur montant CFA à arrondir
     * @return multiple de 5 le plus proche de {@code valeur}
     */
    public static int arrondirAuMultipleDe5(int valeur) {
        return (int) Math.round(valeur / 5.0) * 5;
    }

    /** Surcharge {@code long} de {@link #arrondirAuMultipleDe5(int)}. */
    public static long arrondirAuMultipleDe5(long valeur) {
        return Math.round(valeur / 5.0) * 5L;
    }

    public static PeremptionStatut buildPeremptionStatut(LocalDate datePeremption) {
        if (datePeremption == null) {
            return null;
        }
        LocalDate toDate = LocalDate.now();
        Period p = Period.between(toDate, datePeremption);

        int years = p.getYears();
        int months = p.getMonths();
        int days = p.getDays();
        String libelle;


        if (datePeremption.isBefore(toDate)) {
            Period diff = Period.between(datePeremption, toDate);

            String txtYears = diff.getYears() > 0 ? diff.getYears() + " an(s) " : "";
            String txtMonths = diff.getMonths() > 0 ? diff.getMonths() + " mois " : "";
            String txtDays = diff.getDays() > 0 ? diff.getDays() + " jour(s)" : "";
            libelle = "Périmé il y a " + txtYears + txtMonths + txtDays;
        } else if (datePeremption.isEqual(toDate)) {
            libelle = "Périme aujourd'hui";
        } else {
            String txtYears = years > 0 ? years + " an(s) " : "";
            String txtMonths = months > 0 ? months + " mois " : "";
            String txtDays = days > 0 ? days + " jour(s)" : "";
            libelle = "Périme dans " + txtYears + txtMonths + txtDays;
        }
        return new PeremptionStatut(libelle,  ChronoUnit.DAYS.between(toDate,datePeremption) , ChronoUnit.MONTHS.between(toDate,datePeremption), years);
    }
}
