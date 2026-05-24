import { inject, Pipe, PipeTransform } from '@angular/core';
import { AbilityService } from 'app/core/auth/ability.service';
import { NavAbilityAction } from 'app/shared/model/nav-item.model';

/**
 * Pipe de contrôle d'accès fin.
 *
 * Usage :
 * ```html
 * @if ('commande' | hasAbility: 'create') { ... }
 * <p-button [disabled]="!('facturation' | hasAbility: 'export')" />
 * ```
 */
@Pipe({
  name: 'hasAbility',
  pure: false,
})
export class HasAbilityPipe implements PipeTransform {
  private readonly abilityService = inject(AbilityService);

  transform(subject: string, action: NavAbilityAction): boolean {
    return this.abilityService.can(action, subject);
  }
}

