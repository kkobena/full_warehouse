package com.kobe.warehouse.domain;

public enum PrixRererenceType {
    RERERENCE("Prix de référence assurance"),
    POURCENTAGE("Pourcentage appliqué par l'assureur");
    private final String libelle;

    PrixRererenceType(String libelle) {
        this.libelle = libelle;
    }
    public String getLibelle() {
        return libelle;
    }
}
