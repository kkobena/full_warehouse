import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivateFn, Router, UrlTree } from '@angular/router';
import { map, Observable, of } from 'rxjs';

import { LicenseService } from 'app/core/license/license.service';

/**
 * Réserve une route au module de licence déclaré dans `data.feature`.
 *
 * <p>Le masquage des menus (§3.6) suffit à l'usage courant, mais une route reste atteignable par URL
 * directe — un favori, un lien collé, un retour d'historique. Cette garde ferme ce chemin et amène
 * l'utilisateur sur une page qui explique quoi faire, plutôt que sur un écran vide ou une erreur.
 *
 * <p>Elle ne protège rien par elle-même : le refus fait autorité côté serveur, où
 * `@RequiresFeature` répond 402 y compris sur les lectures.
 *
 * @example
 * { path: 'comptabilite', data: { feature: 'COMPTABILITE' }, canActivate: [AuthGuard, licenseFeatureGuard] }
 *
 *
 */
export const licenseFeatureGuard: CanActivateFn = (route: ActivatedRouteSnapshot): Observable<boolean | UrlTree> => {
  const licenseService = inject(LicenseService);
  const router = inject(Router);

  const feature = route.data['feature'] as string | undefined;
  if (!feature) {
    return of(true);
  }

  const decide = (): boolean | UrlTree =>
    licenseService.hasFeature(feature) || router.createUrlTree(['/module-non-souscrit'], { queryParams: { module: feature } });

  // Statut non chargé (accès direct par URL sur une session restaurée) : on le récupère avant de
  // trancher. `hasFeature` répondrait « oui » sur un statut absent, ce qui laisserait passer.
  return licenseService.license() === null ? licenseService.load().pipe(map(decide)) : of(decide());
};
