package com.kobe.warehouse.web.rest.errors;

public class SaleNotFoundCustomerException extends BadRequestAlertException {

    private static final long serialVersionUID = 1L;

    public SaleNotFoundCustomerException() {
        super("Veuillez ajouter un client à la vente", "customerNotFound");
    }
}
