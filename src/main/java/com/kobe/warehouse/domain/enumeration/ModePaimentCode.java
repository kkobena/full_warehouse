package com.kobe.warehouse.domain.enumeration;

public enum ModePaimentCode {
    CASH,
    OM,
    MTN,
    MOOV,
    WAVE,
    CB,
    VIREMENT,
    CH;

    public static ModePaimentCode fromName(String name) {
        for (ModePaimentCode modePaimentCode : ModePaimentCode.values()) {
            if (modePaimentCode.name().equalsIgnoreCase(name)) {
                return modePaimentCode;
            }
        }
        return null;
    }
}
