package com.kobe.warehouse.domain.enumeration;

public enum OrdreTrisFacture {
    DATE_FACTURE("Date de facture"),
    DATE_FACTURE_DESC("Date de facture desc"),
    CODE_FACTURE("Code de facture"),
    TAUX("Taux de de couverture"),
    MONTANT("Montant de facture"),
    NOM_TIER("Nom du tiers");

    private final String libelle;

    OrdreTrisFacture(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
