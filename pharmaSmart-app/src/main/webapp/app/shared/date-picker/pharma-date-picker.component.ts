import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  forwardRef,
  input,
  output,
  viewChild,
  ViewEncapsulation
} from '@angular/core';
import {ControlValueAccessor, FormsModule, NG_VALUE_ACCESSOR} from '@angular/forms';
import {
  NgbDateParserFormatter,
  NgbDatepickerModule,
  NgbDateStruct,
  NgbInputDatepicker
} from '@ng-bootstrap/ng-bootstrap';
import {FrenchDateParserFormatter} from '../../config/french-date-parser-formatter';

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
 *
 * Saisie clavier : l'utilisateur peut taper directement "jj/mm/aaaa" ou "jjmmaaaa" (8 chiffres,
 * sans séparateur) — la valeur est reformatée automatiquement en "jj/mm/aaaa" dès qu'elle forme
 * une date valide (jour/mois/année cohérents), sans attendre de perdre le focus.
 *
 * Pour réagir au changement (ex. relancer une recherche), préférer `(selectionChange)` à
 * `(ngModelChange)` : combiné à `[(ngModel)]` sur le même élément, l'ordre d'écriture des deux
 * attributs déciderait lequel s'exécute en premier — un outil de formatage qui replace
 * `(ngModelChange)` avant `[(ngModel)]` le ferait lire l'ANCIENNE valeur. `(selectionChange)`
 * s'émet après la mise à jour du modèle, donc toujours à l'abri de ce piège.
 * @example
 * <pharma-date-picker id="du" label="Du" [(ngModel)]="fromDate" (selectionChange)="onSearch()" />
 */
@Component({
  selector: 'pharma-date-picker',
  encapsulation: ViewEncapsulation.None,
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {provide: NgbDateParserFormatter, useClass: FrenchDateParserFormatter},
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => PharmaDatePickerComponent),
      multi: true,
    },
  ],
  imports: [NgbDatepickerModule, FormsModule],
  template: `
    <div class="pharma-dp-wrapper" [class.pharma-dp-wrapper--inline]="labelPosition() === 'inline'">
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
          #dpInput
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
          [disabled]="isDisabled || disabled()"
          [readonly]="readOnly()"
          (input)="onManualInput($event)"
        />
        <button
          class="btn pharma-dp-btn"
          type="button"
          (click)="dp.toggle()"
          [disabled]="isDisabled || disabled()"
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
  readonly readOnly = input<boolean>(false);
  /** Désactivation statique, en plus de celle pilotée par `setDisabledState` (`FormControl.disable()`). */
  readonly disabled = input<boolean>(false);
  /** Position du label : au-dessus du champ (défaut) ou en ligne, à sa gauche. */
  readonly labelPosition = input<'top' | 'inline'>('inline');

  /** Émis après la mise à jour du modèle — voir la note sur `(ngModelChange)` ci-dessus. */
  readonly selectionChange = output<NgbDateStruct | null>();
  protected isDisabled = false;
  private readonly dpRef = viewChild.required<NgbInputDatepicker>('dp');
  private readonly dpInputRef = viewChild.required('dpInput', {read: ElementRef<HTMLInputElement>});

  private _value: NgbDateStruct | null = null;

  get value(): NgbDateStruct | null {
    return this._value;
  }

  set value(v: NgbDateStruct | null) {
    this._value = v;
    this.onChange(v);
    this.onTouched();
    this.selectionChange.emit(v);
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
    this.value = {year: d.getFullYear(), month: d.getMonth() + 1, day: d.getDate()};
    this.dpRef().close();
  }

  closeCalendar(): void {
    this.dpRef().close();
  }

  getFocus(): void {
    setTimeout(() => {
      this.dpInputRef().nativeElement.focus();
    }, 100);
  }

  /**
   * Reformate immédiatement "jjmmaaaa" → "jj/mm/aaaa" dès que la saisie manuelle (8 chiffres,
   * sans séparateur) forme une date valide, sans attendre le `blur`.
   *
   * `ngbDatepicker` ne reformate le texte affiché que sur l'événement natif `change` (blur) —
   * on déclenche donc ce même événement dès que le motif est complet ; `FrenchDateParserFormatter`
   * se charge de la validation (jour/mois/année cohérents) et du parsing réel.
   */
  protected onManualInput(event: Event): void {
    const inputEl = event.target as HTMLInputElement;
    if (/^\d{8}$/.test(inputEl.value)) {
      inputEl.dispatchEvent(new Event('change'));
    }
  }

  private onChange: (_: NgbDateStruct | null) => void = () => {
  };
  private onTouched: () => void = () => {
  };
}

