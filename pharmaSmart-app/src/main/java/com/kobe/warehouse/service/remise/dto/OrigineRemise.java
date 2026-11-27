package com.kobe.warehouse.service.remise.dto;

/** D'où vient la remise d'une ligne. */
public enum OrigineRemise {
    PROMOTION("Promotion"),
    REMISE_PRODUIT("Remise produit"),
    /** Une promotion cumulable posée après la remise produit, sur la même ligne. */
    CUMUL("Promotion et remise produit");

    private final String libelle;

    OrigineRemise(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
