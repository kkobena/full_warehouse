package com.kobe.warehouse.service.errors;

public class SaleAlreadyCloseException extends BadRequestAlertException {

    private static final long serialVersionUID = 1L;

    public SaleAlreadyCloseException() {
        super("Cette Vente est déjà cloturée", "saleAlreadyClosed");
    }
}
