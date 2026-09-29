package com.kobe.warehouse.security.navaccess;

import java.lang.reflect.Method;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Règle d'accès déclarée sur un handler : partagée par l'intercepteur et par le test de
 * complétude, pour qu'ils ne divergent jamais.
 */
public final class NavAccessRules {

    public sealed interface Rule {}

    /** {@link NavAccessExempt} : authentification seule. */
    public record Exempt(String justification) implements Rule {}

    /** {@link RequiresNavAccess} : droit nav_item exigé. */
    public record Requires(String[] codes, NavAction action) implements Rule {}

    /** {@code @PreAuthorize} ou {@code @Secured} : contrôle déjà fait par Spring Security. */
    public record SpringSecured() implements Rule {}

    /** Rien de déclaré. */
    public record Unclassified() implements Rule {}

    private NavAccessRules() {}

    /** La déclaration de la méthode l'emporte sur celle de la classe. */
    public static Rule resolve(Method method, Class<?> type) {
        Rule onMethod = declaredOn(
            AnnotatedElementUtils.findMergedAnnotation(method, NavAccessExempt.class),
            AnnotatedElementUtils.findMergedAnnotation(method, RequiresNavAccess.class),
            AnnotatedElementUtils.hasAnnotation(method, PreAuthorize.class) || AnnotatedElementUtils.hasAnnotation(method, Secured.class)
        );
        if (!(onMethod instanceof Unclassified)) {
            return onMethod;
        }
        return declaredOn(
            AnnotatedElementUtils.findMergedAnnotation(type, NavAccessExempt.class),
            AnnotatedElementUtils.findMergedAnnotation(type, RequiresNavAccess.class),
            AnnotatedElementUtils.hasAnnotation(type, PreAuthorize.class) || AnnotatedElementUtils.hasAnnotation(type, Secured.class)
        );
    }

    private static Rule declaredOn(NavAccessExempt exempt, RequiresNavAccess requires, boolean springSecured) {
        if (exempt != null) {
            return new Exempt(exempt.value());
        }
        if (requires != null) {
            return new Requires(requires.value(), requires.action());
        }
        return springSecured ? new SpringSecured() : new Unclassified();
    }
}
