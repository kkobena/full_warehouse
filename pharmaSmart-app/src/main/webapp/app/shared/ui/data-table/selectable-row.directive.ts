import { Directive, computed, inject, input } from '@angular/core';

import { AppTableHost } from './table.types';

/**
 * Rend une ligne sélectionnable — remplace `pSelectableRow`.
 *
 * Se pose sur le `<tr>` du template `#body`. C'est le template qui possède son `<tr>`
 * (comme avec `p-table`), la table ne l'enveloppe pas : la sélection passe donc par
 * cette directive plutôt que par un gestionnaire de la table elle-même.
 *
 * @example
 * <ng-template #body let-produit>
 *   <tr [appSelectableRow]="produit">…</tr>
 * </ng-template>
 */
@Directive({
  selector: '[appSelectableRow]',
  host: {
    '[class.table-active]': 'selected()',
    '[attr.aria-selected]': 'selected()',
    '[class.app-table__row--selectable]': 'true',
    '(click)': 'select($event)',
  },
})
export class SelectableRowDirective {
  readonly row = input.required<unknown>({ alias: 'appSelectableRow' });

  private readonly host = inject(AppTableHost);

  protected readonly selected = computed(() => this.host.isSelected(this.row()));

  protected select(event: Event): void {
    this.host.selectRow(this.row(), event);
  }
}
