package com.kobe.warehouse.domain.enumeration;

public enum ModeReglementRetour {
    REMBOURSEMENT_ESPECES("Remboursement espèces"),
    REMBOURSEMENT_CB("Remboursement CB"),
    AVOIR_CLIENT("Avoir client");

    private final String libelle;

    ModeReglementRetour(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
