package com.kobe.warehouse.domain.enumeration;

/**
 * Véhicule physique de l'étiquette.
 *
 * <p>Ce n'est pas qu'une cote de plus : une planche a une grille, des marges de page et une case
 * de départ — on reprend une planche entamée. Un rouleau n'a rien de tout cela : une étiquette par
 * page, au format de l'étiquette, et l'imprimante déroule. Les écrans masquent donc la case de
 * départ en rouleau, et les gabarits ne sont pas les mêmes.
 */
public enum SupportEtiquette {
    /** Planche A4 autocollante, laser ou jet d'encre : le consommable bureautique. */
    PLANCHE_A4("Planche A4"),

    /** Rouleau d'étiquettes prédécoupées sur imprimante dédiée (Dymo, Brother, Zebra, TSC…). */
    ROULEAU("Rouleau");

    private final String libelle;

    SupportEtiquette(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
