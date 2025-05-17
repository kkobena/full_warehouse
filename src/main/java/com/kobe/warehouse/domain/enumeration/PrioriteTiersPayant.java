package com.kobe.warehouse.domain.enumeration;

public enum PrioriteTiersPayant {
    R0(0, "R0"),
    R1(1, "C1"),
    R2(2, "C2"),
    R3(3, "C3");


    private final int value;
    private final String code;

    PrioriteTiersPayant(int value, String code) {
        this.value = value;
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public int getValue() {
        return value;
    }
}
