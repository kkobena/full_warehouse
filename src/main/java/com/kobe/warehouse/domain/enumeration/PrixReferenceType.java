package com.kobe.warehouse.domain.enumeration;

public enum PrixReferenceType {
    RERERENCE("Prix de référence assurance"),
    POURCENTAGE("Pourcentage appliqué par l'assureur");

    private final String libelle;

    PrixReferenceType(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
