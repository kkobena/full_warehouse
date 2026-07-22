import { Component, input, model, output, ChangeDetectionStrategy } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgbDateStruct } from '@ng-bootstrap/ng-bootstrap';
import { PharmaDatePickerComponent } from '../../date-picker/pharma-date-picker.component';
import { ButtonComponent } from '../../ui';

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
  imports: [PharmaDatePickerComponent, FormsModule, ButtonComponent],
  changeDetection: ChangeDetectionStrategy.Eager,
  template: `
    <div class="d-flex align-items-end gap-2 flex-wrap">
      <pharma-date-picker
        label="Du"
        id="pdr-from"
        [ngModel]="dateToStruct(fromDate())"
        (ngModelChange)="fromDate.set(structToDate($event))"
      />

      <pharma-date-picker
        label="Au"
        id="pdr-to"
        [ngModel]="dateToStruct(toDate())"
        (ngModelChange)="toDate.set(structToDate($event))"
      />

      <app-button
        type="button"
        icon="pi pi-search"
        [label]="label()"
        [loading]="loading()"
        [raised]="true"
        severity="primary"
        size="small"
        (clicked)="onSearch()"
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

  protected dateToStruct(d: Date | null): NgbDateStruct | null {
    if (!d) return null;
    return { year: d.getFullYear(), month: d.getMonth() + 1, day: d.getDate() };
  }

  protected structToDate(s: NgbDateStruct | null): Date | null {
    if (!s) return null;
    return new Date(s.year, s.month - 1, s.day);
  }
}
