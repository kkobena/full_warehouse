package com.kobe.warehouse.domain.enumeration;

public enum ModelFacture {
    MODEL_0907("0907", "Model 0907"),
    MODEL_0903("0903", "Model 0903"),
    MODEL_0203("0203", "Model 0203");

    private final String value;
    private final String libelle;

    ModelFacture(String value, String libelle) {
        this.value = value;
        this.libelle = libelle;
    }

    public String getValue() {
        return value;
    }

    public String getLibelle() {
        return libelle;
    }
}
