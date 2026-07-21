import { Directive, inject, input } from '@angular/core';

import { AppTableHost } from './table.types';

/**
 * Bascule l'affichage de la ligne dépliée — remplace `pRowToggler`.
 *
 * Se pose sur l'élément déclencheur (typiquement un `<app-button>` en début de ligne).
 * La ligne dépliée est rendue par le template `#expandedrow` de la table.
 *
 * @example
 * <app-button [appRowToggler]="client" [icon]="expanded ? 'pi pi-chevron-down' : 'pi pi-chevron-right'" [text]="true" />
 */
@Directive({
  selector: '[appRowToggler]',
  host: {
    '(click)': 'toggle($event)',
  },
})
export class RowTogglerDirective {
  /** Ligne concernée. */
  readonly row = input.required<unknown>({ alias: 'appRowToggler' });

  private readonly host = inject(AppTableHost);

  protected toggle(event: Event): void {
    // Sans ça, le clic remonterait jusqu'à la ligne et déclencherait aussi sa sélection.
    event.stopPropagation();
    this.host.toggleRow(this.row());
  }
}
