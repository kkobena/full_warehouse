package com.kobe.warehouse.service.errors;

public class CustomerAlreadyExistException extends BadRequestAlertException {

    private static final long serialVersionUID = 1L;

    public CustomerAlreadyExistException() {
        super("Ce client existe déjà", "customerExist");
    }

    /** Message précis : nomme le client en cause, pour que le caissier sache quoi changer. */
    public CustomerAlreadyExistException(String message) {
        super(message, "customerExist");
    }
}
