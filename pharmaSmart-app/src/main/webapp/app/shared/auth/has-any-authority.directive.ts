import { computed, Directive, effect, inject, input, TemplateRef, ViewContainerRef } from '@angular/core';

import { AccountService } from 'app/core/auth/account.service';

/**
 * @whatItDoes Conditionally includes an HTML element if current user has any
 * of the authorities passed as the `expression`.
 *
 * @howToUse
 * ```
 *     <some-element *jhiHasAnyAuthority="'ROLE_ADMIN'">...</some-element>
 *
 *     <some-element *jhiHasAnyAuthority="['ROLE_ADMIN', 'ROLE_USER']">...</some-element>
 * ```
 */
@Directive({
  selector: '[jhiHasAnyAuthority]',
})
export default class HasAnyAuthorityDirective {
  public authorities = input<string | string[]>([], { alias: 'jhiHasAnyAuthority' });

  private readonly templateRef = inject(TemplateRef<any>);
  private readonly viewContainerRef = inject(ViewContainerRef);
  private readonly accountService = inject(AccountService);

  constructor() {
    // const accountService = inject(AccountService);
    const currentAccount = this.accountService.trackCurrentAccount();
    const hasPermission = computed(() => currentAccount().authorities && this.accountService.hasAnyAuthority(this.authorities()));

    effect(() => {
      if (hasPermission()) {
        this.viewContainerRef.createEmbeddedView(this.templateRef);
      } else {
        this.viewContainerRef.clear();
      }
    });
  }
}
