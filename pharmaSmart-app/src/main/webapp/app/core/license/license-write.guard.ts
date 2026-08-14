import { inject } from '@angular/core';
import { CanActivateFn, Router, UrlTree } from '@angular/router';
import { map, Observable, of } from 'rxjs';

import { AccountService } from 'app/core/auth/account.service';
import { AbilityService } from 'app/core/auth/ability.service';
import { Authority } from 'app/config/authority.constants';
import { LicenseService } from 'app/core/license/license.service';
import { NotificationService } from 'app/shared/services/notification.service';

/**
 * Interdit l'accès aux écrans de saisie tant que la licence n'autorise pas les modifications, et
 * redirige vers l'écran d'activation — où l'utilisateur trouve les coordonnées du revendeur et peut
 * déposer un nouveau fichier.
 *
 * <p>Ouvrir un point de vente ou un formulaire de création pour se voir refuser l'enregistrement au
 * dernier clic est la pire des expériences : la garde arrête l'utilisateur avant qu'il ne saisisse.
 *
 * <p><strong>Ne jamais poser cette garde sur `licence`</strong> : ce serait fermer la seule
 * porte de sortie d'une officine bloquée.
 *
 * <p>Statut inconnu ⇒ <strong>accès autorisé</strong>. Un statut non chargé (backend momentanément
 * injoignable, session restaurée) ne doit pas se traduire par un blocage : le serveur reste seul
 * juge et refusera l'écriture par un 402 si nécessaire.
 *
 * Cf. docs/PLAN-GESTION-LICENCE.md §5.4.
 */
export const licenseWriteGuard: CanActivateFn = (): Observable<boolean | UrlTree> => {
  const licenseService = inject(LicenseService);
  const accountService = inject(AccountService);
  const ability = inject(AbilityService);
  const notification = inject(NotificationService);
  const router = inject(Router);

  const decide = (): boolean | UrlTree => {
    const info = licenseService.license();
    if (!info?.readOnly) {
      return true;
    }
    notification.error(info.message, 'Licence non valide');


    const canReachLicenseScreen =
      accountService.hasAnyAuthority(Authority.ADMIN) || ability.can('access', 'gestion-licence');
    return canReachLicenseScreen ? router.createUrlTree(['/licence']) : router.createUrlTree(['/']);
  };

  // Le statut est chargé au login et au démarrage du layout ; ce repli couvre l'accès direct par
  // URL sur une session déjà ouverte, sans imposer un aller-retour réseau à chaque navigation.
  return licenseService.license() === null ? licenseService.load().pipe(map(decide)) : of(decide());
};
