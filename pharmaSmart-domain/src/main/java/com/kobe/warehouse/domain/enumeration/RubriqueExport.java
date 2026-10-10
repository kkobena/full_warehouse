package com.kobe.warehouse.domain.enumeration;

/** Rubriques du menu Exports ; chacune a son droit ({@code nav_item}). */
public enum RubriqueExport {
    DONNEES("Données", "exports.donnees"),
    NOMINATIF("Données nominatives", "exports.nominatif"),
    BI("Pour un outil de BI", "exports.bi");

    private final String libelle;
    private final String droit;

    RubriqueExport(String libelle, String droit) {
        this.libelle = libelle;
        this.droit = droit;
    }

    public String getLibelle() {
        return libelle;
    }

    /** Code du {@code nav_item} qui ouvre la rubrique (droit d'export). */
    public String getDroit() {
        return droit;
    }
}
