package com.kobe.warehouse.domain.enumeration;

public enum OptionPrixType {
    REFERENCE("Prix de référence assurance"),
    POURCENTAGE("Pourcentage appliqué par l'assureur"),
    MIXED_REFERENCE_POURCENTAGE("Pourcentage appliqué au prix de référence");

    private final String libelle;

    OptionPrixType(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
