package com.kobe.warehouse.web.rest.errors;

public class SaleNotFoundCustomerException extends BadRequestAlertException {
    private static final long serialVersionUID = 1L;
    public SaleNotFoundCustomerException() {
        super(ErrorConstants.ERR_CUSTOMER_NOT_FOUND, "Veuilez ajouter un client à la vente", "sales", "customerNotFound");
    }
}
