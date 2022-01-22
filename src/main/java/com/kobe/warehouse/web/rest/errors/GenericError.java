package com.kobe.warehouse.web.rest.errors;


public class GenericError extends BadRequestAlertException {
    private static final long serialVersionUID = 1L;

    public GenericError(String defaultMessage, String entityName, String errorKey) {
        super(ErrorConstants.DEFAULT_TYPE, defaultMessage, entityName, errorKey);

    }
}
