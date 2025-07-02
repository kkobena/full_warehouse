import {Directive, ElementRef, forwardRef, HostListener, Input, OnInit} from '@angular/core';
import { AbstractControl, ControlValueAccessor, NG_VALIDATORS, NG_VALUE_ACCESSOR, ValidationErrors, Validator } from '@angular/forms';
import dayjs from 'dayjs/esm';
import { FORMAT_ISO_DATE_TO_STRING_FR } from './util/warehouse-util';

@Directive({
  selector: '[jhiDateNaiss]',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => DateNaissDirective),
      multi: true,
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => DateNaissDirective),
      multi: true,
    },
  ],
})
export class DateNaissDirective implements ControlValueAccessor, Validator, OnInit {
  private _disabled = false;
  private _value: string | null = null;
  @Input() minDate?: string= '1930-01-01';// ISO format (YYYY-MM-DD)
  @Input() maxDate?: string= new Date().toISOString().split('T')[0]; // Default to today

  constructor(private el: ElementRef<HTMLInputElement>) {}

  ngOnInit(): void {
    // Apply initial value if already set
    if (this._value) {
      this.writeValue(this._value);
    }
  }

  @HostListener('input', ['$event'])
  onInput(event: any): void {
    const raw = event.target.value.replace(/\D/g, '').slice(0, 8);

    let formatted = '';
    if (raw.length > 0) {
      formatted += raw.substring(0, 2);
    }
    if (raw.length >= 3) {
      formatted += '/' + raw.substring(2, 4);
    }
    if (raw.length >= 5) {
      formatted += '/' + raw.substring(4, 8);
    }

    const input = this.el.nativeElement;
    input.value = formatted;
    this.onTouched();
    this.onValidatorChange();
    // Emit ISO formatted string when full date entered
    if (formatted.length === 10) {
      const iso = this.toISODate(formatted);
      this.onChange(iso);
    } else {
      this.onChange(null);
    }
  }

  registerOnValidatorChange(fn: () => void): void {
    this.onValidatorChange = fn;
  }

  @HostListener('blur')
  onBlur(): void {
    this.onTouched();
  }

  // Allow external value to populate input
  writeValue(value: string): void {
    this._value = value;
    if (!value) {
      this.el.nativeElement.value = '';
      return;
    }
    this.el.nativeElement.value = FORMAT_ISO_DATE_TO_STRING_FR(value);
  }

  registerOnChange(fn: any): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: any): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this._disabled = isDisabled;
    this.el.nativeElement.disabled = isDisabled;
  }

  // Validate correct format and valid date
  validate(control: AbstractControl): ValidationErrors | null {
    const value = this.el.nativeElement.value;

    if (!value) {
      return null; // No value means no validation error
    }

    if (!/^\d{2}\/\d{2}\/\d{4}$/.test(value)) {
      return { invalidFormat: true };
    }

    const [ddStr, mmStr, yyyyStr] = value.split('/');

    const mm = parseInt(mmStr, 10);
    const dd = parseInt(ddStr, 10);
    const yyyy = parseInt(yyyyStr, 10);
    // Check month and day ranges
    if (mm < 1 || mm > 12 || dd < 1 || dd > 31) {
      return { invalidDate: true };
    }
    if (mm === 2 && dd > 29) {
      return { invalidDate: true };
    }

    // Check actual date validity
    const date = dayjs(`${yyyyStr}-${mm}-${dd}`);

    if (!date.isValid()) {
      return { invalidDate: true };
    }

    // Optional: restrict year range
    if (this.minDate) {
      const minDate = dayjs(this.minDate);
      if (date.isBefore(minDate)) {
        return { invalidDate: true };
      }
    }

    if (this.maxDate) {
      const maxDate = dayjs(this.maxDate);
      if (date.isAfter(maxDate)) {
        return { invalidDate: true };
      }
    }
   /* if (yyyy < 1920 || yyyy > new Date().getFullYear()) {
      return { outOfRange: true };
    }*/

    return null;
  }

  private onChange = (_: any) => {};

  private onTouched = () => {};

  private onValidatorChange = () => {};

  private toISODate(dateStr: string): string {
    const [dd, mm, yyyy] = dateStr.split('/');
    return `${yyyy}-${mm}-${dd}`;
  }
}
