import { Component, computed, input, output } from '@angular/core';

export type AppBadgeSeverity = 'primary' | 'secondary' | 'success' | 'info' | 'warn' | 'danger' | 'help' | 'contrast';

/**
 * Badge du Design System — remplace `p-tag`, `p-badge` et `p-chip`.
 *
 * `p-tag` utilise un fond **teinté** (`{couleur}.100`) avec un texte foncé (`{couleur}.700`),
 * pas un aplat comme `.text-bg-*` de Bootstrap. Les utilitaires `.bg-*-subtle` de Bootstrap
 * s'en approchent mais ne sont pas générés pour nos severities custom `help` / `contrast`.
 * On style donc directement depuis les tokens figés : rendu exact et aucune severity manquante.
 *
 * @example
 * <app-badge label="Actif" severity="success" />
 * <app-badge label="Périmé" severity="danger" icon="pi pi-exclamation-triangle" />
 * <app-badge label="Filtre" [dismissible]="true" (dismissed)="clearFilter()" />
 */
@Component({
  selector: 'app-badge',
  host: {
    '[class]': 'hostClasses()',
  },
  template: `
    @if (icon()) {
      <i [class]="icon()" aria-hidden="true"></i>
    }
    <span>{{ label() }}</span>
    @if (dismissible()) {
      <button type="button" class="app-badge__close" [attr.aria-label]="dismissAriaLabel()" (click)="dismissed.emit()">
        <i class="pi pi-times" aria-hidden="true"></i>
      </button>
    }
  `,
  styles: `
    :host {
      display: inline-flex;
      align-items: center;
      gap: 0.25rem;
      padding: 0.25rem 0.5rem;
      font-size: 0.875rem;
      font-weight: 700;
      line-height: 1.2;
      border-radius: var(--p-content-border-radius);
    }

    :host(.app-badge--rounded) {
      border-radius: var(--p-border-radius-xl);
    }

    .app-badge__close {
      display: inline-flex;
      padding: 0;
      border: 0;
      background: none;
      color: inherit;
      cursor: pointer;
      opacity: 0.7;

      &:hover {
        opacity: 1;
      }
    }

    // Fond {couleur}.100 / texte {couleur}.700 — reproduit p-tag du preset Aura.
    @each $severity, $token in (primary: primary, success: green, info: sky, warn: orange, danger: red, help: purple) {
      :host(.app-badge--#{$severity}) {
        background: var(--p-#{$token}-100);
        color: var(--p-#{$token}-700);
      }
    }

    :host(.app-badge--secondary) {
      background: var(--p-surface-100);
      color: var(--p-surface-600);
    }

    :host(.app-badge--contrast) {
      background: var(--p-surface-950);
      color: var(--p-surface-0);
    }
  `,
})
export class BadgeComponent {
  readonly label = input<string>('');

  readonly severity = input<AppBadgeSeverity>('primary');

  /** Classe d'icône affichée avant le libellé, ex. `pi pi-check`. */
  readonly icon = input<string>('');

  /** Coins arrondis façon pilule (équivalent `p-chip`). */
  readonly rounded = input<boolean>(false);

  /** Affiche une croix de fermeture (équivalent `p-chip` removable). */
  readonly dismissible = input<boolean>(false);

  readonly dismissAriaLabel = input<string>('Retirer');

  readonly dismissed = output<void>();

  protected readonly hostClasses = computed(() => {
    const classes = ['app-badge', `app-badge--${this.severity()}`];
    if (this.rounded()) classes.push('app-badge--rounded');
    return classes.join(' ');
  });
}
