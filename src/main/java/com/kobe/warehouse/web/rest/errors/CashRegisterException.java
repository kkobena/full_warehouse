package com.kobe.warehouse.web.rest.errors;

public class CashRegisterException extends BadRequestAlertException {

    private static final long serialVersionUID = 1L;

    public CashRegisterException() {
        super("Votre caisse est fermée", "cashRegisterNotFound");
    }
}
