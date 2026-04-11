import { computed, Directive, effect, inject, input, TemplateRef, ViewContainerRef } from '@angular/core';
import { AbilityService } from 'app/core/auth/ability.service';
import { NavAbilityAction } from 'app/shared/model/nav-item.model';

export interface AbilityCheck {
  action: NavAbilityAction;
  subject: string;
}

/**
 * Directive structurelle de contrôle d'accès fin (ABAC).
 *
 * Usage :
 * ```html
 * <p-button *appHasAbility="{ action: 'create', subject: 'commande' }" label="Nouveau" />
 * <th *appHasAbility="{ action: 'delete', subject: 'facturation' }">Actions</th>
 * ```
 */
@Directive({
  selector: '[appHasAbility]',
})
export class HasAbilityDirective {
  public check = input<AbilityCheck>({ action: 'access', subject: '' }, { alias: 'appHasAbility' });

  private readonly templateRef = inject(TemplateRef<any>);
  private readonly viewContainerRef = inject(ViewContainerRef);
  private readonly abilityService = inject(AbilityService);

  constructor() {
    const hasPermission = computed(() => {
      const { action, subject } = this.check();
      if (!subject) return false;
      return this.abilityService.can(action, subject);
    });

    effect(() => {
      if (hasPermission()) {
        this.viewContainerRef.createEmbeddedView(this.templateRef);
      } else {
        this.viewContainerRef.clear();
      }
    });
  }
}

