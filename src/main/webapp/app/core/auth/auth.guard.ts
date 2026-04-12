import { inject, Injector, isDevMode } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivateFn, Router, RouterStateSnapshot } from '@angular/router';
import { of } from 'rxjs';
import { filter, map, switchMap, take } from 'rxjs/operators';
import { toObservable } from '@angular/core/rxjs-interop';

import { AccountService } from 'app/core/auth/account.service';
import { StateStorageService } from 'app/core/auth/state-storage.service';
import { AbilityService } from 'app/core/auth/ability.service';
import { NavStore } from 'app/core/store/nav.store';
import { NavAbilityAction } from 'app/shared/model/nav-item.model';
import { Authority } from 'app/config/authority.constants';

/**
 * Guard unifié — remplace UserRouteAccessService + AbilityRouteGuard.
 *
 * Quatre vérifications séquentielles :
 *  1. Authentification  → redirige /login si non connecté
 *  2. ROLE_ADMIN bypass → accès total, pas de vérification ABAC
 *  3. Attente nav tree  → résout le timing async de navStore.load()
 *  4. ABAC strict       → can(action, subject) sans fallback
 *                         Non configuré = refusé (sémantique stricte)
 *
 * Usage :
 * ```ts
 * {
 *   path: 'commande',
 *   data: { abilitySubject: 'commande' },   // optionnel — auth seule si absent
 *   canActivate: [AuthGuard],
 * }
 * ```
 *
 * - `abilitySubject` : code du nav item en base. Absent = vérification auth uniquement.
 * - `abilityAction`  : action ABAC (défaut `'access'`).
 * - Pas d'`authorities[]` dans les données — le contrôle d'accès par rôle passe
 *   entièrement par NavItemRole.canAccess (dynamique depuis la base).
 */
export const AuthGuard: CanActivateFn = (route: ActivatedRouteSnapshot, state: RouterStateSnapshot) => {
  const accountService = inject(AccountService);
  const stateStorage = inject(StateStorageService);
  const ability = inject(AbilityService);
  const navStore = inject(NavStore);
  const router = inject(Router);
  const injector = inject(Injector);

  return accountService.identity().pipe(
    switchMap(account => {
      // ── 1. Authentification ─────────────────────────────────────────────────
      if (!account) {
        stateStorage.storeUrl(state.url);
        router.navigate(['/login']);
        return of(false);
      }


      // ROLE_ADMIN a toujours accès — les contraintes NavItemRole ne s'appliquent pas.
      if (accountService.hasAnyAuthority(Authority.ADMIN)) {
        return of(true);
      }


      // navStore.load() est async (HTTP distinct de identity()). On attend que le
      // signal `loaded` passe à true avant d'évaluer les permissions.
      const loaded$ = navStore.loaded()
        ? of(true)
        : toObservable(navStore.loaded, { injector }).pipe(filter(Boolean), take(1));

      return loaded$.pipe(
        map(() => {
          // ── 4. ABAC strict ──────────────────────────────────────────────────
          // Sémantique stricte : non configuré pour ce rôle = refusé.
          // Le backend n'inclut pas dans l'arbre les nav items sans NavItemRole
          // pour les rôles de l'utilisateur → can() retourne false.
          const subject = route.data['abilitySubject'] as string | undefined;
          if (!subject) return true; // route auth-only (pas de nav item associé)

          const action = (route.data['abilityAction'] as NavAbilityAction | undefined) ?? 'access';

          if (!ability.can(action, subject)) {
            if (isDevMode()) {
              console.error(`[AuthGuard] ABAC refusé — subject: "${subject}", action: "${action}"`);
            }
            router.navigate(['/accessdenied']);
            return false;
          }

          return true;
        }),
      );
    }),
  );
};
