import { Component, forwardRef, inject, input, output, signal, viewChild } from '@angular/core';
import { DatePicker, DatePickerModule } from 'primeng/datepicker';
import { FloatLabelModule } from 'primeng/floatlabel';
import { ControlValueAccessor, FormsModule, NG_VALUE_ACCESSOR } from '@angular/forms';
import { PrimeNG } from 'primeng/config';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { DATE_FORMAT_ISO_DATE } from '../util/warehouse-util';

@Component({
  selector: 'jhi-date-picker',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => DatePickerComponent),
      multi: true,
    },
  ],
  imports: [DatePickerModule, FloatLabelModule, FormsModule, TranslatePipe],
  template: `
    <p-floatlabel variant="on">
      <p-datePicker
        #datePicker
        dateFormat="dd/mm/yy"
        [iconDisplay]="'input'"
        [disabled]="disabled()"
        [id]="id()"
        [minDate]="min()"
        [maxDate]="max()"
        [(ngModel)]="value"
        (ngModelChange)="onNgModelChange($event)"
        [showIcon]="true"
        [inputId]="id()"
        [selectOtherMonths]="true"
        [showButtonBar]="true"
        [style]="style()"
        class="mr-1"
        [defaultDate]="defaultDate()"
        (onSelect)="onSelectDate($event)"
        (onInput)="handleOnInput($event)"
      />
      <label for="{{ id() }}">{{ label() | translate }}</label>
    </p-floatlabel>
  `,
})
export class DatePickerComponent implements ControlValueAccessor {
  style = input<{}>();
  label = input.required<string>();
  id = input.required<string>();
  class = input<string>();
  min = input<Date | null>(null);
  max = input<Date | null>(null);
  defaultDate = input<Date | null>(null);
  disabled = input<boolean>(false);
  onSelect = output<Date>();
  onInput = output<void>();
  onDateInput = output<void>();
  protected datePicker = viewChild.required<DatePicker>('datePicker');
  private readonly primeNGConfig = inject(PrimeNG);
  private readonly translate = inject(TranslateService);

  constructor() {
    this.translate.use('fr');
    this.translate.stream('primeng').subscribe(data => {
      this.primeNGConfig.setTranslation(data);
    });
  }

  private _value = signal<Date | null>(null);

  // Getter / Setter pour ngModel
  get value(): Date | null {
    return this._value();
  }

  set value(value: Date | null) {
    this._value.set(value);
    this.onChange(value);
  }

  get submitValue(): string | null {
    return DATE_FORMAT_ISO_DATE(this.value);
  }

  writeValue(value: any): void {
    this.value = value;
  }

  registerOnChange(fn: any): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: any): void {
    this.onTouched = fn;
  }

  onNgModelChange(value: Date | null): void {
    this.value = value;
  }

  setDisabledState?(isDisabled: boolean): void {
    // Implémenter si besoin
  }

  handleInput(evt: any): void {
    console.log(evt.target.value, ' input');
    this.onDateInput.emit();
  }

  handleOnInput(evt: any): void {
    this.onInput.emit();
  }

  onSelectDate(date: Date): void {
    this.onSelect.emit(date);
  }

  getFocus(): void {
    setTimeout(() => {
      this.datePicker().el.nativeElement.focus();
      //  console.log(el, 'el');
      //  el.focus();
    }, 100);
  }

  private onChange: (_: any) => void = () => {};

  private onTouched: () => void = () => {};
}
