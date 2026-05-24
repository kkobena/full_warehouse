package com.kobe.warehouse.domain.enumeration;

/**
 * Type de classification de criticité.
 * Indique comment la classe de criticité a été déterminée.
 */
public enum ClassificationType {
    /**
     * Classification automatique par le système (reclassification mensuelle)
     */
    AUTO("Automatique"),

    /**
     * Classification manuelle par un administrateur (override)
     */
    MANUAL("Manuelle"),

    /**
     * Classification initiale lors de la création du produit
     */
    INITIAL("Initiale");

    private final String description;

    ClassificationType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
