package com.kobe.warehouse.domain.enumeration;

public enum ModelFacture {
    MODEL_0176("0176", "Model 0176"),
    MODEL_0177("0177", "Model 0177"),
    MODEL_0109("0109", "Model 0109"),
    MODEL_0303("0303", "Model 0303"),
    MODEL_0301("0301", "Model 0301"),
    MODEL_0202("0202", "Model 0202");

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
