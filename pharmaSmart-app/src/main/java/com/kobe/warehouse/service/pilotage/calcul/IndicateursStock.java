package com.kobe.warehouse.service.pilotage.calcul;

/** Rotation et couverture du stock, rapportées au coût des ventes des douze derniers mois. */
public final class IndicateursStock {

    private static final int JOURS_PAR_AN = 365;

    private IndicateursStock() {}

    /** Fois par an : coût des ventes des 12 derniers mois / valeur du stock. */
    public static Double rotation(long coutDouzeMois, long valeur) {
        return valeur == 0 ? null : (double) coutDouzeMois / valeur;
    }

    /** Jours de ventes que couvre le stock, au rythme des 12 derniers mois. */
    public static Double couverture(long valeur, long coutDouzeMois) {
        return coutDouzeMois == 0 ? null : valeur * (double) JOURS_PAR_AN / coutDouzeMois;
    }
}
