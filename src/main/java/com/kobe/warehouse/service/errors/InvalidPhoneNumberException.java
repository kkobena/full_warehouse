package com.kobe.warehouse.service.errors;

import org.springframework.http.HttpStatus;

public class InvalidPhoneNumberException extends BadRequestAlertException {

    public InvalidPhoneNumberException() {
        super("Le numéro de téléphone saisi n'est pas correct", "invalidPhoneNumber");
    }
}
