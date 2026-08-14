package com.kobe.warehouse.aop.license;

import com.kobe.warehouse.license.Feature;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Réserve une méthode — ou un contrôleur entier — aux installations ayant souscrit le module indiqué.
 *
 * <p>Contrairement au blocage d'expiration (B4), qui n'atteint que les écritures, celui-ci
 * s'applique <strong>aussi aux lectures</strong> : un module non souscrit doit être invisible, pas
 * seulement en lecture seule. Le masquage des menus (§3.6) suffit à l'usage courant ; cette
 * annotation ferme l'accès direct par URL ou par appel d'API.
 *
 * <p>Cf. docs/PLAN-GESTION-LICENCE.md §3.6.
 *
 * @example <pre>{@code
 * @RestController
 * @RequestMapping("/api/comptabilite")
 * @RequiresFeature(Feature.COMPTABILITE)
 * public class ComptabiliteResource { … }
 * }</pre>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
public @interface RequiresFeature {
    Feature value();
}
