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
 *
 * Sémantique stricte : masqué si le sujet n'est pas configuré en base.
 */
@Directive({
  selector: '[appHasAbility]',
})
export class HasAbilityDirective {
  public check = input<AbilityCheck>({ action: 'access', subject: '' }, { alias: 'appHasAbility' });

  private readonly templateRef      = inject(TemplateRef<any>);
  private readonly viewContainerRef = inject(ViewContainerRef);
  private readonly abilityService   = inject(AbilityService);

  constructor() {
    const hasPermission = computed(() => {
      const { action, subject } = this.check();
      return !!subject && this.abilityService.can(action, subject);
    });

    effect(() => {
      this.viewContainerRef.clear();
      if (hasPermission()) {
        this.viewContainerRef.createEmbeddedView(this.templateRef);
      }
    });
  }
}

