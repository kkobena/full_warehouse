package com.kobe.warehouse.domain.enumeration;

public enum StatutSuggession {
    OPEN("Ouvert"),
    CLOSED("Cloturé");

    private final String libelle;

    public String getLibelle() {
        return libelle;
    }

    StatutSuggession(String libelle) {
        this.libelle = libelle;
    }
}
