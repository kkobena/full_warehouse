import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';

import { NavStore } from 'app/core/store/nav.store';

/**
 * Le contenu d'un lien de sous-menu — icône, libellé, chevron — lu dans la base de navigation.
 *
 * <p>Le libellé et l'icône d'un onglet existent déjà dans `nav_item` : c'est cette ligne que
 * l'administrateur modifie depuis l'écran de gestion des menus. Recopiés dans le gabarit, ils s'en
 * écartaient au premier renommage — le pharmacien changeait « Balance caisse » et ne voyait rien
 * bouger. Ce composant supprime la copie : le gabarit ne connaît plus que le **code**, la seule
 * information qui ne change pas, puisque c'est elle qui porte les permissions.
 *
 * <p><strong>Le repli n'est pas optionnel.</strong> Si le code est absent de l'arbre — item
 * désactivé, base d'une installation plus ancienne, arbre pas encore chargé au premier rendu — le
 * lien afficherait un libellé vide, donc un menu inutilisable. `fallbackLabel` garantit qu'il
 * reste au pire périmé, jamais muet.
 *
 * <p>`display: contents` : `.pharma-nav-vertical-link` est un conteneur flex avec `gap`, et son
 * chevron se cale par `margin-left: auto`. Un élément hôte visible s'interposerait entre le lien et
 * ses trois enfants, et la mise en page tomberait.
 *
 * @example
 * <a class="pharma-nav-vertical-link" ngbNavLink>
 *   <app-nav-section-link code="comptabilite.balance"
 *                         fallbackIcon="pi pi-calculator" fallbackLabel="Balance caisse" />
 * </a>
 */
@Component({
  selector: 'app-nav-section-link',
  template: `
    @if (icon()) {
      <i [class]="icon()" aria-hidden="true"></i>
    }
    <span>{{ label() }}</span>
    @if (arrow()) {
      <span class="link-arrow" aria-hidden="true">›</span>
    }
  `,
  styles: `
    :host {
      display: contents;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NavSectionLinkComponent {
  /** Code de la `SECTION` en base, ex. `comptabilite.balance`. */
  readonly code = input.required<string>();

  /** Libellé servi tant que le code n'est pas trouvé dans l'arbre. */
  readonly fallbackLabel = input<string>('');

  /** Icône servie tant que le code n'est pas trouvé, ex. `pi pi-calculator`. */
  readonly fallbackIcon = input<string>('');

  /**
   * Rend le chevron de fin de ligne.
   *
   * <p>À désactiver quand le gabarit intercale un élément entre le libellé et le chevron — un badge
   * d'alerte, par exemple : le chevron doit alors rester après lui, et c'est l'appelant qui le place.
   */
  readonly arrow = input<boolean>(true);

  private readonly navStore = inject(NavStore);

  protected readonly label = computed(() => this.navStore.node(this.code())?.libelle || this.fallbackLabel());

  protected readonly icon = computed(() => this.navStore.node(this.code())?.icon || this.fallbackIcon());
}
