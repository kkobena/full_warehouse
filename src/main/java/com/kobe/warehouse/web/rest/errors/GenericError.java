package com.kobe.warehouse.web.rest.errors;

public class GenericError extends BadRequestAlertException {

    private static final long serialVersionUID = 1L;

    public GenericError(String defaultMessage, String errorKey) {
        super(defaultMessage, errorKey);
    }

    public GenericError(String defaultMessage) {
        super(defaultMessage);
    }
}
