package com.kobe.warehouse.web.rest.errors;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.ProblemDetail;

public class ProblemDetailWithCause extends ProblemDetail {

    private ProblemDetailWithCause cause;

    ProblemDetailWithCause(int rawStatus) {
        super(rawStatus);
    }

    ProblemDetailWithCause(int rawStatus, ProblemDetailWithCause cause) {
        super(rawStatus);
        this.cause = cause;
    }

    public ProblemDetailWithCause getCause() {
        return this.cause;
    }

    public void setCause(ProblemDetailWithCause cause) {
        this.cause = cause;
    }

    public static class ProblemDetailWithCauseBuilder {

        private static final URI BLANK_TYPE = URI.create("about:blank");
        private URI type;
        private String title;
        private int status;
        private String detail;
        private URI instance;
        private Map<String, Object> properties;
        private ProblemDetailWithCause cause;

        public ProblemDetailWithCauseBuilder() {
            this.type = BLANK_TYPE;
            this.properties = new HashMap<>();
        }

        public static ProblemDetailWithCause.ProblemDetailWithCauseBuilder instance() {
            return new ProblemDetailWithCause.ProblemDetailWithCauseBuilder();
        }

        public ProblemDetailWithCause.ProblemDetailWithCauseBuilder withType(URI type) {
            this.type = type;
            return this;
        }

        public ProblemDetailWithCause.ProblemDetailWithCauseBuilder withTitle(String title) {
            this.title = title;
            return this;
        }

        public ProblemDetailWithCause.ProblemDetailWithCauseBuilder withStatus(int status) {
            this.status = status;
            return this;
        }

        public ProblemDetailWithCause.ProblemDetailWithCauseBuilder withDetail(String detail) {
            this.detail = detail;
            return this;
        }

        public ProblemDetailWithCause.ProblemDetailWithCauseBuilder withInstance(URI instance) {
            this.instance = instance;
            return this;
        }

        public ProblemDetailWithCause.ProblemDetailWithCauseBuilder withCause(ProblemDetailWithCause cause) {
            this.cause = cause;
            return this;
        }

        public ProblemDetailWithCause.ProblemDetailWithCauseBuilder withProperties(Map<String, Object> properties) {
            this.properties = properties;
            return this;
        }

        public ProblemDetailWithCause.ProblemDetailWithCauseBuilder withProperty(String key, Object value) {
            this.properties.put(key, value);
            return this;
        }

        public ProblemDetailWithCause build() {
            ProblemDetailWithCause cause = new ProblemDetailWithCause(this.status);
            cause.setType(this.type);
            cause.setTitle(this.title);
            cause.setDetail(this.detail);
            cause.setInstance(this.instance);
            Map<String, Object> var10000 = this.properties;
            Objects.requireNonNull(cause);
            var10000.forEach(cause::setProperty);
            cause.setCause(this.cause);
            return cause;
        }
    }
}
