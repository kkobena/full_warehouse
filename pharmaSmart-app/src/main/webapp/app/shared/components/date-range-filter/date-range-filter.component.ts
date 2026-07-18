import { Component, input, model, output, ChangeDetectionStrategy } from '@angular/core';
import { DatePicker } from 'primeng/datepicker';
import { FloatLabel } from 'primeng/floatlabel';
import { FormsModule } from '@angular/forms';
import { Button } from 'primeng/button';

/**
 * Filtre date-range réutilisable : deux date-pickers (Du / Au) + bouton Rechercher.
 *
 * Utilisation :
 * ```html
 * <pharma-date-range-filter
 *   [(fromDate)]="fromDate"
 *   [(toDate)]="toDate"
 *   [loading]="isLoading()"
 *   (search)="loadData()"
 * />
 * ```
 * `fromDate` et `toDate` sont des `model()` Angular — compatibles signals et two-way binding.
 */
@Component({
  selector: 'pharma-date-range-filter',
  imports: [DatePicker, FloatLabel, FormsModule, Button],
  changeDetection: ChangeDetectionStrategy.Eager,
  template: `
    <div class="d-flex align-items-end gap-2 flex-wrap">
      <p-floatlabel variant="on">
        <p-datepicker
          [ngModel]="fromDate()"
          (ngModelChange)="fromDate.set($event)"
          [iconDisplay]="'input'"
          [selectOtherMonths]="true"
          [showButtonBar]="true"
          [showIcon]="true"
          dateFormat="dd/mm/yy"
          id="pdr-from"
          inputId="pdr-from"
          appendTo="body"
          [style]="{ minWidth: '130px' }"
        />
        <label for="pdr-from">Du</label>
      </p-floatlabel>

      <p-floatlabel variant="on">
        <p-datepicker
          [ngModel]="toDate()"
          (ngModelChange)="toDate.set($event)"
          [iconDisplay]="'input'"
          [selectOtherMonths]="true"
          [showButtonBar]="true"
          [showIcon]="true"
          dateFormat="dd/mm/yy"
          id="pdr-to"
          inputId="pdr-to"
          appendTo="body"
          [style]="{ minWidth: '130px' }"
        />
        <label for="pdr-to">Au</label>
      </p-floatlabel>

      <p-button
        type="button"
        icon="pi pi-search"
        [label]="label()"
        [loading]="loading()"
        [raised]="true"
        severity="primary"
        size="small"
        (onClick)="onSearch()"
      />
    </div>
  `,
})
export class DateRangeFilterComponent {
  /** Date de début — two-way bindable via [(fromDate)] */
  fromDate = model<Date | null>(new Date());
  /** Date de fin — two-way bindable via [(toDate)] */
  toDate   = model<Date | null>(new Date());
  /** Désactive le bouton et affiche un spinner pendant le chargement */
  loading  = input<boolean>(false);
  /** Label du bouton (défaut : "Rechercher") */
  label    = input<string>('Rechercher');
  /** Émis quand l'utilisateur clique sur le bouton */
  search   = output<void>();

  protected onSearch(): void {
    this.search.emit();
  }
}
