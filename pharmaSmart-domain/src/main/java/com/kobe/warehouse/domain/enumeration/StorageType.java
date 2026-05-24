package com.kobe.warehouse.domain.enumeration;

public enum StorageType {
    PRINCIPAL("Stock rayon", true, true),
    SAFETY_STOCK("Réserve", false, true),
    TOXIQUE("Toxiques", true, false),
    QUARANTAINE("Quarantaine", false, false);

    private final String value;
    private final boolean vendable;
    private final boolean reassortSuggere;

    StorageType(String value, boolean vendable, boolean reassortSuggere) {
        this.value = value;
        this.vendable = vendable;
        this.reassortSuggere = reassortSuggere;
    }

    public static StorageType valueOfLabel(String value) {
        for (StorageType e : values()) {
            if (e.value.equals(value)) {
                return e;
            }
        }
        return null;
    }

    public String getValue() {
        return value;
    }

    /** Produits dispensables / vendables depuis ce storage. */
    public boolean isVendable() {
        return vendable;
    }

    /** Ce storage participe aux suggestions de réassort automatique. */
    public boolean isReassortSuggere() {
        return reassortSuggere;
    }
}
