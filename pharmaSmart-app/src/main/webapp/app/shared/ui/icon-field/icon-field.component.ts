import {Component, computed, input} from '@angular/core';

/**
 * Champ avec icône du Design System — remplace le couple `p-iconfield` / `p-inputicon`.
 *
 * **Deux rendus.** Par défaut, un `.input-group` Bootstrap : l'icône occupe une case à
 * part, à côté du champ. En `overlay`, l'icône est superposée au champ, comme le faisait
 * `p-iconfield` — c'est le rendu attendu partout où l'on migre depuis PrimeNG.
 *
 * Le mode `overlay` existe parce que sortir l'icône du flux tout en gardant `.input-group`
 * réveille une série de règles Bootstrap qui présupposent l'inverse : marge négative sur
 * le champ, coins gauches aplatis, `z-index: 5` au focus. Les combattre depuis l'appelant
 * demandait des `!important` en cascade. Ici, la classe `.input-group` n'est simplement
 * pas posée.
 *
 * @example
 * <app-icon-field icon="pi pi-search" [overlay]="true">
 *   <input class="form-control" placeholder="Rechercher un produit" />
 * </app-icon-field>
 *
 * <app-icon-field icon="pi pi-calendar" iconPos="right">
 *   <input class="form-control" />
 * </app-icon-field>
 */
@Component({
  selector: 'app-icon-field',
  host: {
    '[class.input-group]': '!overlay()',
    '[class.input-group-sm]': '!overlay() && size() === "small"',
    '[class.input-group-lg]': '!overlay() && size() === "large"',
    '[class.app-icon-field--overlay]': 'overlay()',
    '[class.app-icon-field--icon-left]': 'overlay() && iconPos() === "left"',
    '[class.app-icon-field--icon-right]': 'overlay() && iconPos() === "right"',
  },
  template: `
    @if (iconPos() === 'left') {
      <span [class]="iconClasses()"><i [class]="icon()" aria-hidden="true"></i></span>
    }
    <ng-content />
    @if (iconPos() === 'right') {
      <span [class]="iconClasses()"><i [class]="icon()" aria-hidden="true"></i></span>
    }
  `,
  styles: `
    :host(.app-icon-field--overlay) {
      position: relative;
      display: block;
    }

    :host(.app-icon-field--overlay) .app-icon-field__icon {
      position: absolute;
      top: 50%;
      transform: translateY(-50%);
      // Au-dessus du z-index 5 que Bootstrap donne au champ focalisé, sinon le fond de
      // l'input recouvre l'icône dès qu'on clique dedans.
      z-index: 6;
      display: inline-flex;
      align-items: center;
      color: var(--bs-secondary-color);
      // L'icône ne doit pas voler le clic destiné au champ.
      pointer-events: none;
    }

    :host(.app-icon-field--icon-left) .app-icon-field__icon {
      left: 0.75rem;
    }

    :host(.app-icon-field--icon-right) .app-icon-field__icon {
      right: 0.75rem;
    }

    // Le champ est projeté : il porte l'encapsulation de l'appelant, donc les styles de
    // ce composant ne l'atteignent pas sans ::ng-deep. Combiné à :host, la règle reste
    // confinée au sous-arbre de cette instance.
    :host(.app-icon-field--icon-left) ::ng-deep .form-control {
      padding-left: 2.5rem !important;
    }

    :host(.app-icon-field--icon-right) ::ng-deep .form-control {
      padding-right: 2.5rem;
    }
  `,
})
export class IconFieldComponent {
  readonly icon = input.required<string>();

  readonly iconPos = input<'left' | 'right'>('left');

  readonly size = input<'small' | 'normal' | 'large'>('normal');

  /** Superpose l'icône au champ au lieu de la placer dans une case adjacente. */
  readonly overlay = input<boolean>(false);

  protected readonly iconClasses = computed(() => (this.overlay() ? 'app-icon-field__icon' : 'input-group-text'));
}
