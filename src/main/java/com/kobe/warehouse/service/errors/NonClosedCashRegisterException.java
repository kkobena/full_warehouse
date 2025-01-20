package com.kobe.warehouse.service.errors;

import java.io.Serial;

public class NonClosedCashRegisterException extends BadRequestAlertException {

    @Serial
    private static final long serialVersionUID = 1L;

    public NonClosedCashRegisterException() {
        super("Vous avez une caisse en cours. Vous devez la fermer caisse avant", "nonClosedCashRegister");
    }
}
