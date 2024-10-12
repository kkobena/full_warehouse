package com.kobe.warehouse.service.errors;

import java.io.Serial;

public class GenericError extends BadRequestAlertException {

    @Serial
    private static final long serialVersionUID = 1L;

    public GenericError(String defaultMessage, String errorKey) {
        super(defaultMessage, errorKey);
    }

    public GenericError(String defaultMessage) {
        super(defaultMessage);
    }
}
