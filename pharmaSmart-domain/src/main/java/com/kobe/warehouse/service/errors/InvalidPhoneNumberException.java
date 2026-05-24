package com.kobe.warehouse.service.errors;

public class InvalidPhoneNumberException extends BadRequestAlertException {

    public InvalidPhoneNumberException() {
        super("Le numéro de téléphone saisi n'est pas correct", "invalidPhoneNumber");
    }
}
