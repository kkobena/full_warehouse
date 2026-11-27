package com.kobe.warehouse.service.remise.dto;

/** Les trois journaux : ce que les promotions et les grilles ont remisé, ce que les UG ont rapporté. */
public enum NatureJournal {
    /** Lignes portant une promotion, unités offertes d'une offre N+M comprises. */
    PROMOTIONS("Promotions"),
    /** Lignes portant une remise produit (grille), seule ou cumulée avec une promotion. */
    REMISES("Remises produit"),
    /** Lignes ayant vendu des UG : unités reçues gratuitement du fournisseur, vendues au prix normal. */
    UG("UG vendues");

    private final String libelle;

    NatureJournal(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
