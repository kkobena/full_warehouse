package com.kobe.warehouse.security.navaccess;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Exige un droit {@code nav_item_role} sur l'un des codes {@code nav_item} listés : un endpoint
 * partagé par plusieurs écrans liste tous leurs codes, et l'un d'eux suffit.
 *
 * <p>Posée sur la méthode, elle remplace celle de la classe. Cf.
 * docs/PLAN-SECURISATION-ENDPOINTS.md § 3.
 *
 * @example <pre>{@code
 * @RequiresNavAccess({"rapport-stock.stock-valuation", "comptabilite.tableau-pharmacien"})
 * @RequiresNavAccess(value = "factures", action = NavAction.EXPORT)
 * }</pre>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
public @interface RequiresNavAccess {
    /** Codes {@code nav_item} acceptés. */
    String[] value();

    /** Droit exigé ; par défaut, déduit du verbe HTTP. */
    NavAction action() default NavAction.AUTO;
}
