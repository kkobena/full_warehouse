package com.kobe.warehouse.service.errors;

import com.kobe.warehouse.license.LicenseViolationException;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Objects.nonNull;

/**
 * Controller advice to translate the server side exceptions to client-friendly json structures. The
 * error response follows RFC7807 - Problem Details for HTTP APIs
 */
@ControllerAdvice
public class ExceptionTranslator extends ResponseEntityExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ExceptionTranslator.class);


    @ExceptionHandler({OptimisticLockException.class, ObjectOptimisticLockingFailureException.class})
    public ResponseEntity<Object> handleOptimisticLock(Exception ex, NativeWebRequest request) {
        Custom pd = new Custom(HttpStatus.CONFLICT.value());
        pd.setDetail("Le stock a été modifié par une autre opération. Veuillez réessayer.");
        pd.setMessage("stock.concurrent.modification");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(pd);
    }

    /**
     * Licence expirée, absente, invalide, ou module non souscrit.
     *
     * <p>Le statut <strong>402 Payment Required</strong> est ici sémantiquement exact, et surtout il
     * n'est pas intercepté par {@code auth-expired.interceptor.ts} côté Angular : répondre 401 ou 403
     * déconnecterait l'utilisateur, qui ne pourrait plus atteindre l'écran de renouvellement.
     */
    @ExceptionHandler(LicenseViolationException.class)
    public ResponseEntity<Object> handleLicenseViolation(LicenseViolationException ex) {
        LOG.warn("Opération refusée pour cause de licence : {}", ex.getMessage());
        Custom pd = new Custom(HttpStatus.PAYMENT_REQUIRED.value());
        pd.setTitle("Licence non valide");
        pd.setDetail(ex.getMessage());
        pd.setMessage(ex.getMessage());
        pd.setErrorKey(ex.getErrorKey());
        pd.setPayload(ex.getPayload());
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(pd);
    }

    @ExceptionHandler
    public ResponseEntity<Object> handleAnyException(Throwable ex, NativeWebRequest request) {
        Custom pd = customizeProblem(ex);
        if (ex instanceof BadRequestAlertException) {
            // Règle métier refusée : le message suffit, il n'y a pas de défaut à corriger.
            LOG.warn("Erreur métier : {}", ex.getMessage());
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
        // Une erreur INATTENDUE : ni règle métier, ni saisie fautive, mais un défaut du
        // logiciel. Le client n'en apprend rien — et c'est voulu, un message technique
        // n'aiderait personne au comptoir et renseignerait un attaquant — mais le serveur
        // n'en gardait aucune trace non plus. Une pile d'appels perdue, c'est un HTTP 500
        // indiagnosticable : on ne pouvait ni nommer la cause, ni savoir si elle se
        // reproduisait. La trace est donc écrite ICI, au seul endroit par lequel toutes
        // passent.
        Custom pd = new Custom(HttpStatus.INTERNAL_SERVER_ERROR.value());
        var errMsg = "Une erreur interne est survenue";
        pd.setDetail(errMsg);
        pd.setMessage(errMsg);
        LOG.error("Erreur non gérée renvoyée en HTTP 500 : {}", err.toString(), err);
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
