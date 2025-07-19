import {Component, forwardRef, inject, input, output, signal} from '@angular/core';
import {DatePickerModule} from "primeng/datepicker";
import {FloatLabelModule} from "primeng/floatlabel";
import {ControlValueAccessor, FormsModule, NG_VALUE_ACCESSOR} from "@angular/forms";
import {PrimeNG} from "primeng/config";
import {TranslatePipe, TranslateService} from "@ngx-translate/core";
import {DATE_FORMAT_ISO_DATE} from "../util/warehouse-util";

@Component({
  selector: 'jhi-date-picker',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => DatePickerComponent),
      multi: true,
    },
  ],
  imports: [
    DatePickerModule,
    FloatLabelModule,
    FormsModule,
    TranslatePipe
  ],
  template: `
    <p-floatlabel variant="on">
      <p-datePicker
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
      <label for="{{id()}}">{{ label() | translate }}</label>
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
  private readonly primeNGConfig = inject(PrimeNG);
  private readonly translate = inject(TranslateService);
  private _value = signal<Date | null>(null);
  onSelect = output<Date>();
  onInput = output<void>();
  onDateInput = output<void>();

  constructor() {
    this.translate.use('fr');
    this.translate.stream('primeng').subscribe(data => {
      this.primeNGConfig.setTranslation(data);
    });
  }

  private onChange: (_: any) => void = () => {
  };
  private onTouched: () => void = () => {
  };

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

  // Getter / Setter pour ngModel
  get value(): Date | null {
    return this._value();
  }

  set value(value: Date | null) {
    this._value.set(value);
    this.onChange(value);

  }

  handleInput(evt: any): void {
    console.log(evt.target.value, ' input');
    this.onDateInput.emit();
  }

  handleOnInput(evt: any): void {

    this.onInput.emit();
  }

  get submitValue(): string | null {
    return DATE_FORMAT_ISO_DATE(this.value);
  }

  onSelectDate(date: Date): void {
    this.onSelect.emit(date);
  }
}
