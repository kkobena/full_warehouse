package com.kobe.warehouse.domain.enumeration;

/**
 * The PaymentGroup enumeration.
 */
public enum PaymentGroup {
    CASH("Espèce"),
    CREDIT("Crédit"),
    MOBILE("Mobile"),
    CB("Carte bancaire"),
    CHEQUE("Chèque");

    private final String value;


    PaymentGroup(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
