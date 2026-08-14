package com.kobe.warehouse.license;

/**
 * Échec de vérification cryptographique ou structurelle d'un fichier de licence : signature
 * invalide, JWS malformé, JSON illisible, clé publique absente.
 *
 * <p>Distincte de {@link LicenseViolationException}, qui traduit un <em>refus d'écriture</em> à
 * l'exécution : celle-ci traduit une licence <em>inexploitable</em>.
 */
public class LicenseVerificationException extends Exception {

    private final String errorKey;

    public LicenseVerificationException(String message, String errorKey) {
        super(message);
        this.errorKey = errorKey;
    }

    public LicenseVerificationException(String message, String errorKey, Throwable cause) {
        super(message, cause);
        this.errorKey = errorKey;
    }

    public String getErrorKey() {
        return errorKey;
    }
}
