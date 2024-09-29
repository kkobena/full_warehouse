package com.kobe.warehouse.service.errors;

public class CashRegisterException extends BadRequestAlertException {

    private static final long serialVersionUID = 1L;

    public CashRegisterException() {
        super("Votre caisse est fermée", "cashRegisterNotFound");
    }
}
