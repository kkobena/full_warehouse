package com.kobe.warehouse.service.utils;

import com.kobe.warehouse.domain.PaymentMode;
import com.kobe.warehouse.domain.enumeration.ModePaimentCode;
import com.kobe.warehouse.service.stock.dto.PeremptionStatut;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;

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

    public static long computeHtaxe(long ttc, int taxe) {
        return ttc / (1 + (taxe / 100));
    }

    public static int computeHtaxe(int ttc, int taxe) {
        return ttc / (1 + (taxe / 100));
    }

    public static long computeHtaxe(long ttc, double taxe) {
        return (long) (ttc / (1 + (taxe / 100)));
    }
    public static int arrondiTauxCouverture(int taux) {

        int arrondi = Math.round(taux / 5f) * 5;

        return Math.min(100, arrondi);

    }
    public static double calculHt(int ttc, int tva) {
        return (ttc) * 1.0 / (1 + (tva / 100.f));
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
        return new PeremptionStatut(libelle, days, months, years);
    }
}
