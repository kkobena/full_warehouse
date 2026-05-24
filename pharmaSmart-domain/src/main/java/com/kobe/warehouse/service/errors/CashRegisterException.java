package com.kobe.warehouse.service.errors;

import java.io.Serial;

public class CashRegisterException extends BadRequestAlertException {

    @Serial
    private static final long serialVersionUID = 1L;

    public CashRegisterException() {
        super("Votre caisse est fermée", "cashRegisterNotFound");
    }
}
