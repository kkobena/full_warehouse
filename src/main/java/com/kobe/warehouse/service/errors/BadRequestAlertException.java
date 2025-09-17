package com.kobe.warehouse.service.errors;

import java.io.Serial;

public class BadRequestAlertException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String entityName;

    private final String errorKey;
    private final Object payload;

    public BadRequestAlertException(String defaultMessage, String entityName, String errorKey) {
        super(defaultMessage
        );
        this.errorKey = errorKey;
        this.entityName = entityName;
        this.payload = null;
    }


    public BadRequestAlertException(String defaultMessage) {
        super(defaultMessage
        );
        this.errorKey = null;
        this.entityName = null;
        this.payload = null;
    }

    public BadRequestAlertException(String defaultMessage, Object payload) {
        super(defaultMessage
        );
        this.errorKey = null;
        this.entityName = null;
        this.payload = payload;
    }

    public BadRequestAlertException(String defaultMessage, String errorKey) {
        super(defaultMessage
        );
        this.entityName = null;
        this.errorKey = errorKey;
        this.payload = null;
    }


    public BadRequestAlertException(String defaultMessage, String errorKey, Object payload) {
        super(defaultMessage

        );
        this.errorKey = errorKey;
        this.entityName = null;
        this.payload = payload;
    }

    public String getEntityName() {
        return entityName;
    }

    public String getErrorKey() {
        return errorKey;
    }

    public Object getPayload() {
        return payload;
    }
}
