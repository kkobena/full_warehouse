package com.kobe.warehouse.service.pharmaml.dto.response.enumeration;

/**
 * @author koben
 */
public enum TypeRemplacement {
    RL("Remplaçant Livré"),
    EL("Equivalent livré"),
    EP("Equivalent proposé");

    private final String label;

    TypeRemplacement(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
