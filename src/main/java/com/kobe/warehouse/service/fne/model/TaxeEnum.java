package com.kobe.warehouse.service.fne.model;

import java.util.stream.Stream;

public enum TaxeEnum {
    TVA(18),
    TVAB(9),
    TVAD(0);

    private final int value;

    TaxeEnum(int value) {
        this.value = value;
    }

    public static TaxeEnum getByValue(int p) {
        return Stream.of(values())
            .filter(r -> r.value == p)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("La valeure de la tva n'est pas connue " + p));
    }

    public int getValue() {
        return value;
    }
}
