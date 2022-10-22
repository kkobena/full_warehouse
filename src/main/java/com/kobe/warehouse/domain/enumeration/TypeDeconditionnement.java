package com.kobe.warehouse.domain.enumeration;

public enum TypeDeconditionnement {
    DECONDTION_IN("Décondtion entrant"),
    DECONDTION_OUT("Décondtion sortant");
    private final String value;
    TypeDeconditionnement(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
