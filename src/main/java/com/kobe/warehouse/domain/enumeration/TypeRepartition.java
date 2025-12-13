package com.kobe.warehouse.domain.enumeration;

public enum TypeRepartition {
    AUTO("Automatique"),
    MANUEL("Manuelle");
    private final String libelle;

    TypeRepartition(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
