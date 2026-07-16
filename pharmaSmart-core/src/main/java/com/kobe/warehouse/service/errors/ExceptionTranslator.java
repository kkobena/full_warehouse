package com.kobe.warehouse.service.errors;

import jakarta.persistence.OptimisticLockException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.logging.Logger;

import static java.util.Objects.nonNull;

/**
 * Controller advice to translate the server side exceptions to client-friendly json structures. The
 * error response follows RFC7807 - Problem Details for HTTP APIs
 */
@ControllerAdvice
public class ExceptionTranslator extends ResponseEntityExceptionHandler {
    private static final Logger LOG = Logger.getLogger(ExceptionTranslator.class.getName());


    @ExceptionHandler({OptimisticLockException.class, ObjectOptimisticLockingFailureException.class})
    public ResponseEntity<Object> handleOptimisticLock(Exception ex, NativeWebRequest request) {
        Custom pd = new Custom(HttpStatus.CONFLICT.value());
        pd.setDetail("Le stock a été modifié par une autre opération. Veuillez réessayer.");
        pd.setMessage("stock.concurrent.modification");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(pd);
    }

    @ExceptionHandler
    public ResponseEntity<Object> handleAnyException(Throwable ex, NativeWebRequest request) {
        Custom pd = customizeProblem(ex);
        if (ex instanceof BadRequestAlertException) {
            LOG.warning("Erreur métier: " + ex.getMessage());
        } else {
            LOG.severe("Erreur interne non gérée: " + ex.getMessage());
        }

        return ResponseEntity.status(pd.getStatus()).body(pd);

    }


    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
        Exception ex,
        Object body,
        HttpHeaders headers,
        HttpStatusCode statusCode,
        WebRequest request
    ) {
        body = body == null ? wrapAndCustomizeProblem(ex, (NativeWebRequest) request) : body;
        return super.handleExceptionInternal(ex, body, headers, statusCode, request);
    }

    protected ProblemDetail wrapAndCustomizeProblem(Throwable ex, NativeWebRequest request) {
        return customizeProblem(ex);
    }

    protected Custom customizeProblem(Throwable err) {
        if (err instanceof BadRequestAlertException cust) {
            Custom pd = new Custom(HttpStatus.BAD_REQUEST.value());
            pd.setDetail(cust.getMessage());
            pd.setMessage(cust.getMessage());

            if (nonNull(cust.getErrorKey())) {
                pd.setErrorKey(cust.getErrorKey());
            }
            if (nonNull(cust.getPayload())) {
                pd.setPayload(cust.getPayload());
            }
            return pd;
        }
        Custom pd = new Custom(HttpStatus.INTERNAL_SERVER_ERROR.value());
        var errMsg = "Une erreur interne est survenue";
        pd.setDetail(errMsg);
        pd.setMessage(errMsg);
        return pd;
    }

    private class Custom extends ProblemDetail {

        private String errorKey;
        private Object payload;
        private String message;

        public Custom(int rawStatusCode) {
            super(rawStatusCode);
        }

        public String getErrorKey() {
            return errorKey;
        }

        public void setErrorKey(String errorKey) {
            this.errorKey = errorKey;
        }

        public Object getPayload() {
            return payload;
        }

        public void setPayload(Object payload) {
            this.payload = payload;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
