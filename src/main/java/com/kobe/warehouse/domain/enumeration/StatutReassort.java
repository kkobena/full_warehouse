package com.kobe.warehouse.domain.enumeration;

public enum StatutReassort {
    OPEN("Ouvert"),
    CLOSED("Traité");

    private final String libelle;

    public String getLibelle() {
        return libelle;
    }

    StatutReassort(String libelle) {
        this.libelle = libelle;
    }
}
