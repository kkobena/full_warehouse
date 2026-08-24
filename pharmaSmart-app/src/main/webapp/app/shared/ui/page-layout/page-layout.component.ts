import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

export type PageLayoutDensity = 'compact' | 'default';

/**
 * Structure verticale d'une vue/page applicative.
 *
 * Le composant orchestre uniquement quatre zones projetées : en-tête, aide,
 * contexte temporaire et contenu métier. Il ne crée pas de toolbar, n'ajoute
 * pas de surface et ne gère aucun état métier.
 *
 * @example
 * <app-page-layout density="compact" ariaLabel="Catalogue produits">
 *   <app-toolbar ngProjectAs="[pageHeader]" title="Produits" />
 *   <app-hint ngProjectAs="[pageGuidance]">Conseil</app-hint>
 *   <app-bulk-action-bar ngProjectAs="[pageContext]" [count]="2" />
 *   <app-data-table [value]="items()" />
 * </app-page-layout>
 */
@Component({
  selector: 'app-page-layout',
  templateUrl: './page-layout.component.html',
  styleUrl: './page-layout.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PageLayoutComponent {
  /** Rythme vertical entre les zones de la page. */
  readonly density = input<PageLayoutDensity>('default');

  /** Identifiant du heading qui nomme la région. */
  readonly labelledBy = input('');

  /** Nom accessible de repli lorsqu'aucun heading référençable n'existe. */
  readonly ariaLabel = input('');

  protected readonly layoutClasses = computed(() => `app-page-layout app-page-layout--${this.density()}`);
  protected readonly regionRole = computed(() => (this.labelledBy() || this.ariaLabel() ? 'region' : null));
}
