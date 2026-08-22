import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivateFn, Router, UrlTree } from '@angular/router';
import { map, Observable, of } from 'rxjs';

import { LicenseService } from 'app/core/license/license.service';

/**
 * Réserve une route aux modules de licence déclarés dans `data.feature` ou `data.features`.
 *
 * <p>Le masquage des menus (§3.6) suffit à l'usage courant, mais une route reste atteignable par URL
 * directe — un favori, un lien collé, un retour d'historique. Cette garde ferme ce chemin et amène
 * l'utilisateur sur une page qui explique quoi faire, plutôt que sur un écran vide ou une erreur.
 *
 * <p>Elle ne protège rien par elle-même : le refus fait autorité côté serveur, où
 * `@RequiresFeature` répond 402 y compris sur les lectures.
 *
 * <p>`data.features` couvre les modules vendus par morceaux : la route s'ouvre dès qu'**un** des
 * modules listés est souscrit, chaque écran restant filtré par son propre `required_feature`.
 * Exiger le lot complet fermerait la porte à une officine qui n'en a acheté qu'une partie.
 *
 * @example
 * { path: 'comptabilite', data: { feature: 'COMPTABILITE' }, canActivate: [AuthGuard, licenseFeatureGuard] }
 * { path: 'declaration-ca', data: { features: ['EXCLUSION_RAYON', 'CALLEBASSE'] }, canActivate: [AuthGuard, licenseFeatureGuard] }
 */
export const licenseFeatureGuard: CanActivateFn = (route: ActivatedRouteSnapshot): Observable<boolean | UrlTree> => {
  const licenseService = inject(LicenseService);
  const router = inject(Router);

  const declares = route.data['features'] as string[] | undefined;
  const feature = route.data['feature'] as string | undefined;
  const requis = declares?.length ? declares : feature ? [feature] : [];
  if (requis.length === 0) {
    return of(true);
  }

  const decide = (): boolean | UrlTree =>
    requis.some(module => licenseService.hasFeature(module)) ||
    // Un seul module est nommé sur la page de refus : en lister quatre noierait le message.
    router.createUrlTree(['/module-non-souscrit'], { queryParams: { module: requis[0] } });

  // Statut non chargé (accès direct par URL sur une session restaurée) : on le récupère avant de
  // trancher. `hasFeature` répondrait « oui » sur un statut absent, ce qui laisserait passer.
  return licenseService.license() === null ? licenseService.load().pipe(map(decide)) : of(decide());
};
