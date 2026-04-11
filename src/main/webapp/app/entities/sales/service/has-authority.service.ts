import { inject, Injectable } from '@angular/core';
import { AccountService } from '../../../core/auth/account.service';
import { AbilityService } from '../../../core/auth/ability.service';

/**
 * Service de vérification des permissions.
 * Bridge compatible avec l'ancien RBAC + délégation vers AbilityService (ABAC).
 *
 * Migration progressive (Annexe C.6) :
 *   Étape 2 actuelle : tente via AbilityService (PR_FORCE_STOCK → pr-force-stock),
 *   puis fallback RBAC si l'ability n'est pas trouvée.
 */
@Injectable({
  providedIn: 'root',
})
export class HasAuthorityService {
  private readonly accountService = inject(AccountService);
  private readonly abilityService = inject(AbilityService);

  hasAuthorities(authorities: string | string[]): boolean {
    const list = Array.isArray(authorities) ? authorities : [authorities];

    // Nouveau : cherche via AbilityService (PR_FORCE_STOCK → pr-force-stock)
    const viaAbility = list.some(a =>
      this.abilityService.can('execute', a.toLowerCase().replace(/_/g, '-'))
    );
    if (viaAbility) return true;

    // Fallback : RBAC existant (pendant la transition)
    return this.accountService.hasAnyAuthority(list);
  }
}
