package com.kobe.warehouse.domain.enumeration;

public enum ModePaimentCode {
    CASH(PaymentGroup.CASH, 0),
    OM(PaymentGroup.MOBILE, 1),
    MTN(PaymentGroup.MOBILE, 2),
    MOOV(PaymentGroup.MOBILE, 3),
    WAVE(PaymentGroup.MOBILE, 4),
    CB(PaymentGroup.CB, 5),
    VIREMENT(PaymentGroup.VIREMENT, 6),
    CH(PaymentGroup.CHEQUE, 7);

    private final PaymentGroup paymentGroup;
    private final int sortOrder;

    ModePaimentCode(PaymentGroup paymentGroup, int sortOrder) {
        this.paymentGroup = paymentGroup;
        this.sortOrder = sortOrder;
    }

    public static ModePaimentCode fromName(String name) {
        for (ModePaimentCode modePaimentCode : ModePaimentCode.values()) {
            if (modePaimentCode.name().equalsIgnoreCase(name)) {
                return modePaimentCode;
            }
        }
        return null;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public PaymentGroup getPaymentGroup() {
        return paymentGroup;
    }
}
