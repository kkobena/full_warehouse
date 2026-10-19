package com.kobe.warehouse.domain.enumeration;

/**
 * Feuilles de l'export comptable, telles que l'écran Mouvements de caisse les propose. Aucune section retenue : les six, comme
 * le défaut de l'écran.
 */
public enum SectionExportComptable {
    VENTES("Ventes"),
    ACHATS("Achats"),
    MVT_CAISSE("Mouvements de caisse"),
    TIERS_PAYANT("Tiers payants"),
    DIFFERES("Différés"),
    TVA("TVA");

    private final String libelle;

    SectionExportComptable(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
