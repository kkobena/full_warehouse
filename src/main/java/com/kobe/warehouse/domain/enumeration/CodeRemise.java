package com.kobe.warehouse.domain.enumeration;

public enum CodeRemise {
    CODE_0("1", CodeGrilleRemise.CODE_12, CodeGrilleRemise.CODE_17),
    CODE_1("2", CodeGrilleRemise.CODE_13, CodeGrilleRemise.CODE_18),
    CODE_2("3", CodeGrilleRemise.CODE_14, CodeGrilleRemise.CODE_19),
    CODE_3("4", CodeGrilleRemise.CODE_15, CodeGrilleRemise.CODE_20),
    CODE_4("5", CodeGrilleRemise.CODE_16, CodeGrilleRemise.CODE_21),
    NONE("0", CodeGrilleRemise.NONE, CodeGrilleRemise.NONE);

    private final String value;
    private final CodeGrilleRemise codeVno;
    private final CodeGrilleRemise codeVo;

    CodeRemise(String value, CodeGrilleRemise codeVno, CodeGrilleRemise codeVo) {
        this.value = value;
        this.codeVno = codeVno;
        this.codeVo = codeVo;
    }

    public static CodeRemise fromValue(String value) {
        for (CodeRemise codeRemise : values()) {
            if (codeRemise.getValue().equals(value)) {
                return codeRemise;
            }
        }
        return NONE;
    }

    public CodeGrilleRemise getCodeVno() {
        return codeVno;
    }

    public CodeGrilleRemise getCodeVo() {
        return codeVo;
    }

    public String getValue() {
        return value;
    }
}
