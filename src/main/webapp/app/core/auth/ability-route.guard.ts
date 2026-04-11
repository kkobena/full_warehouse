import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivateFn, Router } from '@angular/router';
import { AbilityService } from 'app/core/auth/ability.service';
import { NavAbilityAction } from 'app/shared/model/nav-item.model';

/**
 * Guard de route basé sur les abilities (ABAC).
 *
 * Usage dans les routes :
 * ```ts
 * {
 *   path: 'commande',
 *   data: { abilityAction: 'access', abilitySubject: 'commande' },
 *   canActivate: [UserRouteAccessService, AbilityRouteGuard],
 * }
 * ```
 */
export const AbilityRouteGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
  const abilityAction = route.data['abilityAction'] as NavAbilityAction | undefined;
  const abilitySubject = route.data['abilitySubject'] as string | undefined;

  // Si pas de contrainte ability → on laisse passer
  if (!abilityAction || !abilitySubject) return true;

  if (inject(AbilityService).can(abilityAction, abilitySubject)) return true;

  inject(Router).navigate(['/accessdenied']);
  return false;
};

