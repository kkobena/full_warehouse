package com.kobe.warehouse.domain.enumeration;

public enum TypeReassort {
    RAYON("Suggession réassort rayon"),
    RESERVE("Suggession réassort réserve");
    private final String libelle;

    TypeReassort(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
