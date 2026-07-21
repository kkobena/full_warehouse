import { ChangeDetectionStrategy, Component, computed, forwardRef, input } from '@angular/core';
import { NG_VALUE_ACCESSOR } from '@angular/forms';

import { ControlValueAccessorBase } from '../forms/control-value-accessor.base';

let nextId = 0;

/**
 * Sélecteur mois/année — s'appuie sur `<input type="month">` natif.
 *
 * Remplace un `p-datepicker` en `view="month"` : `pharma-date-picker` ne couvre que la
 * granularité jour, et écrire un calendrier mois-par-mois pour ce seul besoin n'aurait
 * fait que dupliquer ce que le navigateur fournit déjà. Le champ hérite du style
 * `.form-control` du Design System (bordure et focus alignés sur les tokens Aura), il
 * n'a donc besoin d'aucun CSS propre au-delà du positionnement du libellé.
 *
 * Le modèle lié est un `Date` (premier jour du mois), pour s'accorder avec le reste de
 * l'application plutôt qu'imposer le format `"YYYY-MM"` de l'attribut HTML.
 *
 * @example
 * <app-month-picker label="Mois" icon="pi pi-calendar" [(ngModel)]="selectedMonth" />
 */
@Component({
  selector: 'app-month-picker',
  providers: [{ provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => MonthPickerComponent), multi: true }],
  template: `
    <div class="d-flex flex-column gap-1">
      @if (label()) {
        <label class="small fw-semibold text-body-secondary d-flex align-items-center gap-1" [for]="inputId">
          @if (icon()) {
            <i [class]="icon()" aria-hidden="true"></i>
          }
          {{ label() }}
        </label>
      }

      <input
        type="month"
        class="form-control"
        [class.form-control-sm]="small()"
        [disabled]="isDisabled()"
        [id]="inputId"
        [value]="inputValue()"
        (blur)="onTouched()"
        (change)="onInputChange($event)"
      />
    </div>
  `,
})
export class MonthPickerComponent extends ControlValueAccessorBase<Date> {
  readonly label = input<string>('');

  readonly icon = input<string>('');

  readonly small = input<boolean>(false);

  protected readonly inputId = `app-month-picker-${nextId++}`;

  /** Valeur affichée par le champ natif, au format `YYYY-MM` qu'il impose. */
  protected readonly inputValue = computed(() => {
    const date = this.value();
    if (!date) {
      return '';
    }
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
  });

  /** Reconstruit un `Date` (premier jour du mois) depuis la valeur `YYYY-MM` du champ. */
  protected onInputChange(event: Event): void {
    const raw = (event.target as HTMLInputElement).value;
    if (!raw) {
      this.updateValue(null);
      return;
    }
    const [year, month] = raw.split('-').map(Number);
    this.updateValue(new Date(year, month - 1, 1));
  }
}
