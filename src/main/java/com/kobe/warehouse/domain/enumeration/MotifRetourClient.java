package com.kobe.warehouse.domain.enumeration;

public enum MotifRetourClient {
    ERREUR_DISPENSATION("Erreur dispensation"),
    PRODUIT_DEFECTUEUX("Produit défectueux"),
    ERREUR_QUANTITE("Erreur quantité"),
    INSATISFACTION("Insatisfaction"),
    AUTRE("Autre");

    private final String libelle;

    MotifRetourClient(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
