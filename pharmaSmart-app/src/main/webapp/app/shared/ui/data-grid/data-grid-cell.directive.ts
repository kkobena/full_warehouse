import { Directive, TemplateRef, inject, input } from '@angular/core';

import { AppDataGridCellTemplateContext } from './data-grid.types';

/** Template de rendu riche associé à l'identifiant d'une colonne. */
@Directive({
  selector: 'ng-template[appDataGridCell]',
})
export class DataGridCellDirective<T> {
  readonly columnId = input.required<string>({ alias: 'appDataGridCell' });
  readonly template = inject<TemplateRef<AppDataGridCellTemplateContext<T>>>(TemplateRef);

  static ngTemplateContextGuard<T>(
    _directive: DataGridCellDirective<T>,
    _context: unknown,
  ): _context is AppDataGridCellTemplateContext<T> {
    return true;
  }
}
