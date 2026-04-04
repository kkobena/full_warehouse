import { ChangeDetectionStrategy, Component, forwardRef, input, viewChild, ViewEncapsulation } from '@angular/core';
import { ControlValueAccessor, FormsModule, NG_VALUE_ACCESSOR } from '@angular/forms';
import { NgbDateParserFormatter, NgbDatepickerModule, NgbDateStruct, NgbInputDatepicker } from '@ng-bootstrap/ng-bootstrap';
import { FrenchDateParserFormatter } from '../../config/french-date-parser-formatter';

/**
 * Composant date picker PharmaSmart réutilisable basé sur ng-bootstrap.
 *
 * Utilisation :
 * ```html
 * <pharma-date-picker
 *   id="myDate"
 *   label="Date de début"
 *   [(ngModel)]="myDateStruct"
 * />
 * ```
 * Le modèle lié est de type `NgbDateStruct | null`.
 */
@Component({
  selector: 'pharma-date-picker',
  encapsulation: ViewEncapsulation.None,
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    { provide: NgbDateParserFormatter, useClass: FrenchDateParserFormatter },
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => PharmaDatePickerComponent),
      multi: true,
    },
  ],
  imports: [NgbDatepickerModule, FormsModule],
  template: `
    <div class="pharma-dp-wrapper">
      @if (label()) {
        <label class="pharma-dp-label" [for]="id()">
          @if (icon()) {
            <i [class]="icon()"></i>
          }
          {{ label() }}
        </label>
      }
      <div class="input-group input-group-sm">
        <input
          class="form-control pharma-dp-input"
          [placeholder]="placeholder()"
          ngbDatepicker
          #dp="ngbDatepicker"
          [(ngModel)]="value"
          [container]="$any(container())"
          [footerTemplate]="dpFooter"
          [id]="id()"
          [minDate]="minDate()"
          [maxDate]="maxDate()"
          [disabled]="isDisabled"
          [readonly]="readOnly()"
        />
        <button
          class="btn pharma-dp-btn"
          type="button"
          (click)="dp.toggle()"
          [disabled]="isDisabled"
          [attr.aria-label]="'Ouvrir le calendrier pour ' + label()"
        >
          <i class="pi pi-calendar"></i>
        </button>
      </div>
    </div>

    <ng-template #dpFooter>
      <div class="pharma-dp-footer">
        <button class="btn btn-sm pharma-dp-footer-today" type="button" (click)="selectToday()">
          <i class="pi pi-calendar-clock"></i> Aujourd'hui
        </button>
        <button class="btn btn-sm pharma-dp-footer-close" type="button" (click)="closeCalendar()">
          <i class="pi pi-times"></i> Fermer
        </button>
      </div>
    </ng-template>
  `,
  styleUrl: './pharma-date-picker.component.scss',
})
export class PharmaDatePickerComponent implements ControlValueAccessor {
  /** Texte du label affiché au-dessus de l'input */
  readonly label = input<string>('');
  /** Icône PrimeIcons affichée dans le label (ex: "pi pi-calendar") */
  readonly icon = input<string>('pi pi-calendar');
  /** Id de l'input (pour le label `for`) */
  readonly id = input<string>('pharma-dp');
  /** Texte placeholder */
  readonly placeholder = input<string>('jj/mm/aaaa');
  /** Date minimale sélectionnable */
  readonly minDate = input<NgbDateStruct | null>(null);
  /** Date maximale sélectionnable */
  readonly maxDate = input<NgbDateStruct | null>(null);
  /** Conteneur du popup : 'body' (défaut) pour éviter le clipping, ou null pour positionner dans le DOM */
  readonly container = input<'body' | null>('body');
  /** Rendre l'input en lecture seule (clavier désactivé, sélection par calendrier uniquement) */
  readonly readOnly = input<boolean>(true);

  private readonly dpRef = viewChild.required<NgbInputDatepicker>('dp');

  protected isDisabled = false;
  private _value: NgbDateStruct | null = null;

  get value(): NgbDateStruct | null {
    return this._value;
  }

  set value(v: NgbDateStruct | null) {
    this._value = v;
    this.onChange(v);
    this.onTouched();
  }

  writeValue(obj: NgbDateStruct | null): void {
    this._value = obj ?? null;
  }

  registerOnChange(fn: (_: NgbDateStruct | null) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.isDisabled = isDisabled;
  }

  selectToday(): void {
    const d = new Date();
    this.value = { year: d.getFullYear(), month: d.getMonth() + 1, day: d.getDate() };
    this.dpRef().close();
  }

  closeCalendar(): void {
    this.dpRef().close();
  }

  private onChange: (_: NgbDateStruct | null) => void = () => {};
  private onTouched: () => void = () => {};
}

