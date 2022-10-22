package com.kobe.warehouse.web.rest.errors;

public class CustomerAlreadyExistException extends BadRequestAlertException {
    private static final long serialVersionUID = 1L;
    public CustomerAlreadyExistException() {
        super(ErrorConstants.ERR_CUSTOMER_EXIST, "Ce client existe déjà", "customer", "customerExist");
    }
}
