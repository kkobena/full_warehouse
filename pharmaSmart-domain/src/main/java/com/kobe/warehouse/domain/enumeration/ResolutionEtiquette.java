package com.kobe.warehouse.domain.enumeration;

/**
 * Résolution de la tête d'impression, en points par pouce.
 *
 * <p>Le ZPL ne connaît que le point : toute cote en millimètres doit être convertie, et une
 * conversion faite avec la mauvaise résolution sort une étiquette à la bonne forme mais à la
 * mauvaise taille — 203 contre 300 dpi, c'est un tiers d'écart.
 *
 * <p>203 dpi par défaut : c'est la résolution des Zebra, TSC et Godex d'entrée de gamme, celles
 * qu'on trouve sur le marché ivoirien.
 */
public enum ResolutionEtiquette {
    DPI_203(203),
    DPI_300(300),
    DPI_600(600);

    /** Points par pouce du modèle d'imprimante. */
    private final int dpi;

    ResolutionEtiquette(int dpi) {
        this.dpi = dpi;
    }

    /** Résolution retenue quand l'appelant n'en précise pas. */
    public static ResolutionEtiquette parDefaut() {
        return DPI_203;
    }

    public int getDpi() {
        return dpi;
    }

    /** Taille d'un point, en millimètres : ce qu'une cote ne peut pas descendre en dessous. */
    public double getPasDuPointMm() {
        return 25.4 / dpi;
    }
}
