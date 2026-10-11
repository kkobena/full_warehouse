package com.kobe.warehouse.service.dto.pilotage.rapport;

/**
 * Place d'un graphe sur la page A4 paysage ; la taille de dessin suit la taille imprimée, pour que le texte du graphe garde le
 * même corps partout. Les classes CSS {@code graphe}, {@code graphe-large} et {@code graphe-moitie} en sont le pendant.
 */
public enum FormatGraphe {
    /** Deux tiers de la largeur (168 mm), à côté d'un encart. */
    NORMAL(760, 260),
    /** Pleine largeur (250 mm). */
    LARGE(1130, 386),
    /** Demi-largeur (128 mm), deux graphes côte à côte. */
    MOITIE(580, 198);

    private final int largeur;
    private final int hauteur;

    FormatGraphe(int largeur, int hauteur) {
        this.largeur = largeur;
        this.hauteur = hauteur;
    }

    public int getLargeur() {
        return largeur;
    }

    public int getHauteur() {
        return hauteur;
    }
}
