package com.kobe.warehouse.web.rest.errors;

@SuppressWarnings("java:S110") // Inheritance tree of classes should not be too deep
public class EmailAlreadyUsedException extends BadRequestAlertException {

    private static final long serialVersionUID = 1L;

    public EmailAlreadyUsedException() {
        super("Email is already in use!", "emailexists");
    }
}
