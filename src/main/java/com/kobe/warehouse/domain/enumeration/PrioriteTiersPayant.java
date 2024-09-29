package com.kobe.warehouse.domain.enumeration;

public enum PrioriteTiersPayant {
    R0(0),
    R1(1),
    R2(2),
    R3(3);

    private final int value;

    PrioriteTiersPayant(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
