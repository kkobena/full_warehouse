import { inject, Pipe, PipeTransform } from '@angular/core';
import { AbilityService } from 'app/core/auth/ability.service';
import { NavAbilityAction } from 'app/shared/model/nav-item.model';

/**
 * Pipe de contrôle d'accès fin (ABAC) — évaluation synchrone.
 *
 * Usage :
 * ```html
 * @if ('commande' | hasAbility: 'create') { <p-button label="Nouvelle commande" /> }
 * @if ('ventes.journal.export' | hasAbility: 'export') { <p-button label="PDF" /> }
 * ```
 *
 * Retourne `true` si l'utilisateur a la permission `action` sur `subject`.
 * Retourne `false` si le NavItem n'est pas configuré en base (comportement restrictif).
 * Pour un fallback permissif, utiliser la directive `*appHasAbility` avec `appHasAbilityFallback`.
 */
@Pipe({
  name: 'hasAbility',
  pure: false, // réactif aux changements de signal AbilityService
})
export class HasAbilityPipe implements PipeTransform {
  private readonly ability = inject(AbilityService);

  transform(subject: string, action: NavAbilityAction): boolean {
    return this.ability.can(action, subject);
  }
}
