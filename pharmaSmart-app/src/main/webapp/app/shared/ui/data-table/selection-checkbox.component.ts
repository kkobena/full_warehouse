import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';

import { AppTableHost } from './table.types';

/**
 * Case à cocher d'une ligne — remplace `p-tablecheckbox`.
 *
 * Rend la case Bootstrap directement plutôt que de réutiliser `app-checkbox` : celui-ci
 * est un `ControlValueAccessor` destiné aux formulaires, alors qu'il s'agit ici d'un
 * widget de sélection dont l'état appartient à la table. Le passer par un CVA créerait
 * une seconde source de vérité.
 *
 * @example
 * <ng-template #body let-facture>
 *   <tr>
 *     <td><app-row-checkbox [row]="facture" /></td>
 *   </tr>
 * </ng-template>
 */
@Component({
  selector: 'app-row-checkbox',
  template: `
    <div class="form-check m-0 d-flex justify-content-center">
      <input
        type="checkbox"
        class="form-check-input m-0"
        [attr.aria-label]="ariaLabel()"
        [checked]="checked()"
        [disabled]="disabled()"
        (click)="onClick($event)"
      />
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RowCheckboxComponent {
  /** Ligne concernée. */
  readonly row = input.required<unknown>();

  readonly disabled = input<boolean>(false);

  readonly ariaLabel = input<string>('Sélectionner la ligne');

  private readonly host = inject(AppTableHost);

  protected checked(): boolean {
    return this.host.isSelected(this.row());
  }

  protected onClick(event: Event): void {
    // Sans ça, le clic remonterait au <tr> : si la ligne porte `[appSelectableRow]`, la
    // sélection serait basculée deux fois, donc annulée.
    event.stopPropagation();
    this.host.toggleSelection(this.row());
  }
}

/**
 * Case « tout sélectionner » de l'en-tête — remplace `p-tableheadercheckbox`.
 *
 * Porte l'état indéterminé quand une partie seulement des lignes est cochée, et n'agit
 * que sur les lignes **affichées** : en mode `lazy` la table ne connaît que la page
 * courante. C'est aussi le comportement de `p-tableheadercheckbox`.
 *
 * @example
 * <ng-template #header>
 *   <tr>
 *     <th><app-header-checkbox /></th>
 *   </tr>
 * </ng-template>
 */
@Component({
  selector: 'app-header-checkbox',
  template: `
    <div class="form-check m-0 d-flex justify-content-center">
      <input
        type="checkbox"
        class="form-check-input m-0"
        [attr.aria-label]="ariaLabel()"
        [checked]="checked()"
        [indeterminate]="indeterminate()"
        (click)="onClick($event)"
      />
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HeaderCheckboxComponent {
  readonly ariaLabel = input<string>('Tout sélectionner');

  private readonly host = inject(AppTableHost);

  protected checked(): boolean {
    return this.host.isAllSelected();
  }

  protected indeterminate(): boolean {
    return this.host.isPartiallySelected();
  }

  protected onClick(event: Event): void {
    event.stopPropagation();
    this.host.toggleAll();
  }
}
