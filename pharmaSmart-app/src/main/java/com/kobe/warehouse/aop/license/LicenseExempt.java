package com.kobe.warehouse.aop.license;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Exempte une méthode — ou un contrôleur entier — du blocage en écriture appliqué lorsque la licence
 * n'est plus valide (exigence B4).
 *
 * <p><strong>La liste blanche est ce qui rend le dispositif réversible.</strong> Sans elle, une
 * licence expirée empêcherait de se connecter, donc d'atteindre l'écran d'activation, donc de
 * renouveler : le client serait définitivement bloqué et l'incident deviendrait majeur (cf.
 * PLAN-GESTION-LICENCE §9). Sont exemptés :
 *
 * <ul>
 *   <li>l'authentification (login, rafraîchissement de jeton, déconnexion) ;
 *   <li>l'activation de licence elle-même ;
 *   <li>le changement et la réinitialisation de mot de passe ;
 *   <li>les endpoints d'impression, de duplicata et d'export exposés en {@code POST} — la
 *       traçabilité pharmaceutique impose que la consultation et la réimpression de l'historique
 *       restent possibles.
 * </ul>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
public @interface LicenseExempt {
    /** Justification de l'exemption, à des fins de revue. */
    String value() default "";
}
