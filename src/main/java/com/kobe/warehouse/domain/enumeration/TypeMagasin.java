package com.kobe.warehouse.domain.enumeration;

public enum TypeMagasin {
    OFFICINE("Officine"),
    DEPOT("Dépôt extension"),
    DEPOT_AGGREE("Dépôt Agréé");
    private final String libelle;

    TypeMagasin(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
