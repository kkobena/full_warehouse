package com.kobe.warehouse.domain.enumeration;

public enum CodeGrilleRemise {
    CODE_12("12"),
    CODE_13("13"),
    CODE_14("14"),
    CODE_15("15"),
    CODE_16("16"),
    CODE_17("17"),
    CODE_18("18"),
    CODE_19("19"),
    CODE_20("20"),
    CODE_21("21"),
    NONE("0");

    private final String value;

    CodeGrilleRemise(String value) {
        this.value = value;
    }

    public static CodeGrilleRemise fromValue(String value) {
        for (CodeGrilleRemise codeRemise : values()) {
            if (codeRemise.getValue().equals(value)) {
                return codeRemise;
            }
        }
        return NONE;
    }

    public String getValue() {
        return value;
    }
}
