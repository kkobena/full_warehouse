package com.kobe.warehouse.domain.enumeration;

public enum TypeSubstitut {
    GENERIQUE("Générique"),
    THERAPEUTIQUE("Thérapeutique");
    private final String libelle;
    TypeSubstitut(String libelle) {
        this.libelle = libelle;
    }
    public String getLibelle() {
        return libelle;
    }
}
