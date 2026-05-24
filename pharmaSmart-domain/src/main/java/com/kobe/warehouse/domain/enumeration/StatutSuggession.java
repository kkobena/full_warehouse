package com.kobe.warehouse.domain.enumeration;

public enum StatutSuggession {
    OPEN("Ouvert"),
    CLOSED("Cloturé"),
    GENEREE("Générée"),
    EN_ATTENTE_VALIDATION("En attente de validation"),
    VALIDEE("Validée"),
    COMMANDEE("Commandée");

    private final String libelle;

    public String getLibelle() {
        return libelle;
    }

    StatutSuggession(String libelle) {
        this.libelle = libelle;
    }
}
