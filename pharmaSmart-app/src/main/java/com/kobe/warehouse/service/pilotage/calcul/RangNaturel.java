package com.kobe.warehouse.service.pilotage.calcul;

/** Rang d'un élément d'un axe ordonné : sa clé (heure, jour, tranche de période), ou le début de sa clé (tranche « 5-9 »). */
public final class RangNaturel {

    private RangNaturel() {}

    public static int lire(String cle) {
        int separateur = cle.indexOf('-');
        return Integer.parseInt(separateur < 0 ? cle : cle.substring(0, separateur));
    }
}
