package com.kobe.warehouse.web.rest.errors;

public class PaymentAmountException extends BadRequestAlertException {
    private static final long serialVersionUID = 1L;
    public PaymentAmountException() {
        super(ErrorConstants.ERR_WRONG_ENTRY_AMOUNT, "Le montant  saisi n'est pas correct", "sales", "wrongEntryAmount");
    }
}
