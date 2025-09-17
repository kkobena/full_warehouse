package com.kobe.warehouse.service.errors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import static java.util.Objects.nonNull;

/**
 * Controller advice to translate the server side exceptions to client-friendly json structures. The
 * error response follows RFC7807 - Problem Details for HTTP APIs
 * (https://tools.ietf.org/html/rfc7807).
 */
@ControllerAdvice
public class ExceptionTranslator extends ResponseEntityExceptionHandler {


    private final Environment env;

    @Value("${pharma-smart.clientApp.name}")
    private String applicationName;

    public ExceptionTranslator(Environment env) {
        this.env = env;
    }

    @ExceptionHandler
    public ResponseEntity<Object> handleAnyException(Throwable ex, NativeWebRequest request) {

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(wrapAndCustomizeProblem(ex, request));
    }

    @Nullable
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
        Exception ex,
        @Nullable Object body,
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
