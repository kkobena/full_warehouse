package com.kobe.warehouse.domain.enumeration;

public enum PrioriteTiersPayant {
    T0(0),
    T1(1),
    T2(2),
    T3(3);
    private final int value;

    PrioriteTiersPayant(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
