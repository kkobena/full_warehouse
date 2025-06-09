package com.kobe.warehouse.domain.enumeration;

public enum OptionPrixType {
    RERERENCE("Prix de référence assurance"),
    POURCENTAGE("Pourcentage appliqué par l'assureur");

    private final String libelle;

    OptionPrixType(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
