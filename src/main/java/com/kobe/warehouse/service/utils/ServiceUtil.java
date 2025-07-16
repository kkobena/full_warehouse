package com.kobe.warehouse.service.utils;

import com.kobe.warehouse.domain.PaymentMode;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.enumeration.ModePaimentCode;
import com.kobe.warehouse.service.stock.dto.PeremptionStatut;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;

public class ServiceUtil {

    public static Produit produitFromId(Long id) {
        Produit produit = new Produit();
        produit.setId(id);
        return produit;
    }

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

    public static boolean isPaymentMode(String modePayment) {
        return (
            ModePaimentCode.MTN.name().equalsIgnoreCase(modePayment) ||
            ModePaimentCode.MOOV.name().equalsIgnoreCase(modePayment) ||
            ModePaimentCode.OM.name().equalsIgnoreCase(modePayment) ||
            ModePaimentCode.WAVE.name().equalsIgnoreCase(modePayment)
        );
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
