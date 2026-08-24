import { Directive, TemplateRef, inject } from '@angular/core';

import { AppDataGridDetailTemplateContext } from './data-grid.types';

/** Template pleine largeur rendu sous une ligne maître dépliée. */
@Directive({
  selector: 'ng-template[appDataGridDetail]',
})
export class DataGridDetailDirective<T> {
  readonly template = inject<TemplateRef<AppDataGridDetailTemplateContext<T>>>(TemplateRef);

  static ngTemplateContextGuard<T>(
    _directive: DataGridDetailDirective<T>,
    _context: unknown,
  ): _context is AppDataGridDetailTemplateContext<T> {
    return true;
  }
}
