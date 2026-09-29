package com.kobe.warehouse.security.navaccess;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Endpoint accessible à tout utilisateur authentifié (ou public, selon la chaîne de filtres), sans
 * droit {@code nav_item}. Posée sur la méthode, elle l'emporte sur un {@link RequiresNavAccess} de
 * la classe.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
public @interface NavAccessExempt {
    /** Justification, relue en revue. */
    String value();
}
