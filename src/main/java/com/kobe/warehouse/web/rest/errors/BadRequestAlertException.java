package com.kobe.warehouse.web.rest.errors;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.ErrorResponseException;
import tech.jhipster.web.rest.errors.ProblemDetailWithCause;
import tech.jhipster.web.rest.errors.ProblemDetailWithCause.ProblemDetailWithCauseBuilder;

public class BadRequestAlertException extends ErrorResponseException {

    private static final long serialVersionUID = 1L;

    private final String entityName;

    private final String errorKey;

    public BadRequestAlertException(String defaultMessage, String entityName, String errorKey) {
        super(
            HttpStatus.BAD_REQUEST,
            ProblemDetailWithCauseBuilder.instance()
                .withStatus(HttpStatus.BAD_REQUEST.value())
                .withProperty("message", defaultMessage)
                .withProperty("errorKey", errorKey)
                .build(),
            null
        );
        this.errorKey = errorKey;
        this.entityName = entityName;
    }

    public BadRequestAlertException(HttpStatusCode status, String defaultMessage, String errorKey) {
        super(
            status,
            ProblemDetailWithCauseBuilder.instance()
                .withStatus(status.value())
                .withProperty("message", defaultMessage)
                .withProperty("errorKey", errorKey)
                .build(),
            null
        );
        this.errorKey = errorKey;
        this.entityName = null;
    }

    public BadRequestAlertException(HttpStatusCode status, String defaultMessage) {
        super(
            status,
            ProblemDetailWithCauseBuilder.instance().withStatus(status.value()).withProperty("message", defaultMessage).build(),
            null
        );
        this.errorKey = null;
        this.entityName = null;
    }

    public BadRequestAlertException(HttpStatusCode status, String defaultMessage, Object payload) {
        super(
            status,
            ProblemDetailWithCauseBuilder.instance()
                .withStatus(status.value())
                .withProperty("message", defaultMessage)
                .withProperty("payload", payload)
                .build(),
            null
        );
        this.errorKey = null;
        this.entityName = null;
    }

    public BadRequestAlertException(String defaultMessage, String errorKey) {
        super(
            HttpStatus.BAD_REQUEST,
            ProblemDetailWithCauseBuilder.instance()
                .withStatus(HttpStatus.BAD_REQUEST.value())
                .withTitle(defaultMessage)
                .withProperty("message", defaultMessage)
                .withProperty("errorKey", errorKey)
                .build(),
            null
        );
        this.entityName = null;
        this.errorKey = errorKey;
    }

    public BadRequestAlertException(String defaultMessage) {
        super(
            HttpStatus.BAD_REQUEST,
            ProblemDetailWithCauseBuilder.instance()
                .withStatus(HttpStatus.BAD_REQUEST.value())
                .withTitle(defaultMessage)
                .withProperty("message", defaultMessage)
                .build(),
            null
        );
        this.entityName = null;
        this.errorKey = null;
    }

    public String getEntityName() {
        return entityName;
    }

    public String getErrorKey() {
        return errorKey;
    }

    public ProblemDetailWithCause getProblemDetailWithCause() {
        return (ProblemDetailWithCause) this.getBody();
    }
}
