package com.kobe.warehouse.domain.enumeration;

public enum CodeGrilleRemise {
    NONE("0", CodeGrilleType.NONE),
    CODE_12("12", CodeGrilleType.VNO),
    CODE_13("13", CodeGrilleType.VNO),
    CODE_14("14", CodeGrilleType.VNO),
    CODE_15("15", CodeGrilleType.VNO),
    CODE_16("16", CodeGrilleType.VNO),
    CODE_17("17", CodeGrilleType.VO),
    CODE_18("18", CodeGrilleType.VO),
    CODE_19("19", CodeGrilleType.VO),
    CODE_20("20", CodeGrilleType.VO),
    CODE_21("21", CodeGrilleType.VO),
    CODE_22("22", CodeGrilleType.VNO),
    CODE_23("23", CodeGrilleType.VO),
    CODE_24("24", CodeGrilleType.VNO),
    CODE_25("25", CodeGrilleType.VO),
    CODE_26("26", CodeGrilleType.VNO),
    CODE_27("27", CodeGrilleType.VO),
    CODE_28("28", CodeGrilleType.VNO),
    CODE_29("29", CodeGrilleType.VO);

    private final String value;
    private final CodeGrilleType codeGrilleType;

    CodeGrilleRemise(String value, CodeGrilleType codeGrilleType) {
        this.value = value;
        this.codeGrilleType = codeGrilleType;
    }

    public static CodeGrilleRemise fromValue(String value) {
        for (CodeGrilleRemise codeRemise : values()) {
            if (codeRemise.getValue().equals(value)) {
                return codeRemise;
            }
        }
        return NONE;
    }

    public CodeGrilleType getCodeGrilleType() {
        return codeGrilleType;
    }

    public String getValue() {
        return value;
    }
}
