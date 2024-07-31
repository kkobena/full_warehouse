package com.kobe.warehouse.web.rest.errors;

public class LoginAlreadyUsedException extends BadRequestAlertException {

    private static final long serialVersionUID = 1L;

    public LoginAlreadyUsedException() {
        super("Ce Login existe déjà", "userexists");
    }
}
