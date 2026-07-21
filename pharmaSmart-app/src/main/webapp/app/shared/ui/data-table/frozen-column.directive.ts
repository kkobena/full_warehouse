import { Directive, input } from '@angular/core';

/**
 * Fige une colonne pendant le défilement horizontal — remplace `pFrozenColumn` / `alignFrozen`.
 *
 * À poser sur le `<th>` et le `<td>` correspondants (l'en-tête et le corps se figent
 * indépendamment, comme avec `p-table`). Suppose la table en défilement horizontal, donc
 * combinée à `[scrollable]="true"`.
 *
 * @example
 * <th appFrozenColumn style="width: 8rem"></th>
 * …
 * <td appFrozenColumn="right">…</td>
 */
@Directive({
  selector: '[appFrozenColumn]',
  host: {
    class: 'app-frozen-column',
    '[class.app-frozen-column--right]': "align() === 'right'",
  },
})
export class FrozenColumnDirective {
  /**
   * Bord auquel la colonne se fige : `'left'` (défaut) ou `'right'`.
   *
   * Typé `string`, pas en union littérale : posée sans valeur (`appFrozenColumn` nu, comme
   * `pFrozenColumn`), l'attribut vaut `''`, que le vérificateur de template refuserait pour
   * une entrée `'left' | 'right'`.
   */
  readonly align = input<string>('left', { alias: 'appFrozenColumn' });
}
