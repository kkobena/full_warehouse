package com.kobe.warehouse.domain.enumeration;

/** The TypeMagasin enumeration. */
public enum StorageType {
    PRINCIPAL("Stock rayon"),
    SAFETY_STOCK("Reserve");

    public final String value;

    StorageType(String value) {
        this.value = value;
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
}
