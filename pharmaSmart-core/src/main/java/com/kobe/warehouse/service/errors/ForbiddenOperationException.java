package com.kobe.warehouse.service.errors;

import java.io.Serial;
import org.springframework.security.access.AccessDeniedException;

/** Refus pour droit manquant, avec un message lisible et une clé métier ; traduit en 403. */
public class ForbiddenOperationException extends AccessDeniedException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String errorKey;

    public ForbiddenOperationException(String message, String errorKey) {
        super(message);
        this.errorKey = errorKey;
    }

    public String getErrorKey() {
        return errorKey;
    }
}
