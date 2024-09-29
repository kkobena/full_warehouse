package com.kobe.warehouse.service.errors;

public class PaymentAmountException extends BadRequestAlertException {

    private static final long serialVersionUID = 1L;

    public PaymentAmountException() {
        super("Le montant  saisi n'est pas correct", "wrongEntryAmount");
    }
}
