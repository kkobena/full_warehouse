package com.kobe.warehouse.license;

import java.util.Map;

/**
 * Refus d'une opération pour cause de licence : expiration (B4) ou module non souscrit (§3.6).
 *
 * <p>Traduite par {@code ExceptionTranslator} en <strong>HTTP 402 Payment Required</strong> —
 * sémantiquement exact et, contrairement à 401/403, non intercepté par
 * {@code auth-expired.interceptor.ts} côté Angular : l'utilisateur n'est donc pas déconnecté.
 */
public class LicenseViolationException extends RuntimeException {

    /** Licence absente, invalide ou expirée : toute écriture est refusée. */
    public static final String ERROR_KEY_EXPIRED = "license.expired";
    /** Module absent des {@code features} de la licence. */
    public static final String ERROR_KEY_FEATURE_NOT_INCLUDED = "license.feature.notIncluded";
    /** Quota de la version de démonstration atteint. */
    public static final String ERROR_KEY_DEMO_QUOTA = "license.demo.quotaReached";

    private final String errorKey;
    private final transient Map<String, Object> payload;

    public LicenseViolationException(String message, String errorKey) {
        this(message, errorKey, Map.of());
    }

    public LicenseViolationException(String message, String errorKey, Map<String, Object> payload) {
        super(message);
        this.errorKey = errorKey;
        this.payload = payload == null ? Map.of() : Map.copyOf(payload);
    }

    public String getErrorKey() {
        return errorKey;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }
}
