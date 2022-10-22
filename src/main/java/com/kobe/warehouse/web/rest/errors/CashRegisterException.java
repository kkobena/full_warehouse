package com.kobe.warehouse.web.rest.errors;

public class CashRegisterException extends BadRequestAlertException {
    private static final long serialVersionUID = 1L;

    public CashRegisterException() {
        super(ErrorConstants.CASH_REGISTER_NOT_FOUND, "Votre caisse est fermée", "sales", "cashRegisterNotFound");
    }

}
