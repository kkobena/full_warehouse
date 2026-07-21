import { Directive, computed, inject, input } from '@angular/core';

import { AppTableHost } from './table.types';

/**
 * Rend un `<th>` triable — remplace le couple `pSortableColumn` / `p-sortIcon`.
 *
 * L'icône de tri est injectée en pseudo-élément par le style de la table, donc le `<th>`
 * n'a rien à contenir de plus que son libellé.
 *
 * @example
 * <th appSortableHeader="nom">Nom du produit</th>
 */
@Directive({
  selector: '[appSortableHeader]',
  host: {
    class: 'app-sortable',
    role: 'columnheader',
    tabindex: '0',
    '[class.app-sortable--asc]': 'isSorted() && host.sortOrder() === 1',
    '[class.app-sortable--desc]': 'isSorted() && host.sortOrder() === -1',
    '[attr.aria-sort]': 'ariaSort()',
    '(click)': 'sort()',
    '(keydown.enter)': 'sort()',
    '(keydown.space)': 'sort(); $event.preventDefault()',
  },
})
export class SortableHeaderDirective {
  /** Nom du champ trié. */
  readonly field = input.required<string>({ alias: 'appSortableHeader' });

  protected readonly host = inject(AppTableHost);

  protected readonly isSorted = computed(() => this.host.sortField() === this.field());

  protected readonly ariaSort = computed(() => {
    if (!this.isSorted()) return 'none';
    return this.host.sortOrder() === 1 ? 'ascending' : 'descending';
  });

  protected sort(): void {
    this.host.toggleSort(this.field());
  }
}
