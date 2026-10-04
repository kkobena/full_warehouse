import { ChangeDetectionStrategy, Component, ElementRef, inject, input, model } from '@angular/core';
import { NgbTooltip } from '@ng-bootstrap/ng-bootstrap';

export type AppSubtabBadgeSeverity = 'primary' | 'success' | 'warn' | 'danger' | 'info';

export interface AppSubtab {
  /** Identifiant renvoyé par `(activeChange)`. */
  id: string;
  label: string;
  /** Classe d'icône PrimeIcons, ex. `pi pi-inbox`. */
  icon?: string;
  /** Info-bulle de l'onglet (placée sous la barre). */
  tooltip?: string;
  /** Compteur affiché dans une pastille ; `null` ou absent = pas de pastille. Un `0` s'affiche. */
  badge?: number | null;
  badgeSeverity?: AppSubtabBadgeSeverity;
}

/**
 * Barre d'onglets légère du Design System : navigation entre les sections d'un même écran, placée sous un en-tête
 * (`pharma-toolbar-header`). Remplace les blocs `.su-source-tabs-bar` / `.su-tab` / `.su-tab-badge` recopiés d'écran en écran.
 *
 * Suit le thème de couleur (jetons `--pharma-chrome-tab*` et `--pharma-chrome-badge-*`, `_pharma-chrome-themes.scss`).
 *
 * Accessibilité (patron ARIA « Tabs » de l'APG, activation automatique) : `role="tablist"` / `role="tab"`, `aria-selected`,
 * tabulation itinérante (un seul onglet dans l'ordre de tabulation), flèches gauche et droite, Début et Fin.
 * Le contenu affiché reste à la charge de l'appelant ; lui donner `role="tabpanel"` et `aria-labelledby="<idPrefix><id>"` s'il le souhaite.
 *
 * @example
 * <app-subtab-bar
 *   ariaLabel="Sources d'approvisionnement"
 *   [tabs]="onglets()"
 *   [active]="activeSource()"
 *   (activeChange)="setSource($event)"
 * />
 */
@Component({
  selector: 'app-subtab-bar',
  imports: [NgbTooltip],
  template: `
    <div class="app-subtab-bar" role="tablist" [attr.aria-label]="ariaLabel()" (keydown)="onKeydown($event)">
      @for (tab of tabs(); track tab.id; let i = $index) {
        <button
          type="button"
          role="tab"
          class="app-subtab"
          [class.app-subtab--active]="estActif(tab.id)"
          [attr.id]="idPrefix() + tab.id"
          [attr.aria-selected]="estActif(tab.id)"
          [attr.tabindex]="estFocalisable(tab.id, i) ? 0 : -1"
          [ngbTooltip]="tab.tooltip ?? null"
          placement="bottom"
          (click)="select(tab.id)"
        >
          @if (tab.icon) {
            <i [class]="tab.icon" aria-hidden="true"></i>
          }
          <span class="app-subtab__label">{{ tab.label }}</span>
          @if (tab.badge !== null && tab.badge !== undefined) {
            <span class="app-subtab__badge app-subtab__badge--{{ tab.badgeSeverity ?? 'primary' }}">{{ tab.badge }}</span>
          }
        </button>
      }
    </div>
  `,
  styleUrl: './subtab-bar.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SubtabBarComponent {
  private readonly hote = inject<ElementRef<HTMLElement>>(ElementRef);

  readonly tabs = input.required<readonly AppSubtab[]>();

  /** Identifiant de l'onglet actif. */
  readonly active = model<string>('');

  /** Nom accessible de la liste d'onglets. */
  readonly ariaLabel = input<string>('Sections');

  /** Préfixe des `id` des onglets (`<idPrefix><id>`), à changer si deux barres cohabitent sur un écran. */
  readonly idPrefix = input<string>('subtab-');

  protected estActif(id: string): boolean {
    return this.active() === id;
  }

  /** Un seul onglet est dans l'ordre de tabulation : l'actif, ou le premier tant qu'aucun n'est actif. */
  protected estFocalisable(id: string, index: number): boolean {
    const aUnActif = this.tabs().some(t => t.id === this.active());
    return aUnActif ? this.estActif(id) : index === 0;
  }

  protected select(id: string): void {
    this.active.set(id);
  }

  protected onKeydown(event: KeyboardEvent): void {
    const onglets = this.tabs();
    if (onglets.length === 0) {
      return;
    }
    const courant = Math.max(0, onglets.findIndex(t => t.id === this.active()));
    let cible = -1;
    switch (event.key) {
      case 'ArrowRight':
        cible = (courant + 1) % onglets.length;
        break;
      case 'ArrowLeft':
        cible = (courant - 1 + onglets.length) % onglets.length;
        break;
      case 'Home':
        cible = 0;
        break;
      case 'End':
        cible = onglets.length - 1;
        break;
      default:
        return;
    }
    event.preventDefault();
    this.select(onglets[cible].id);
    this.hote.nativeElement.querySelectorAll<HTMLElement>('[role="tab"]')[cible]?.focus();
  }
}
