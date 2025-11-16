import { Directive, ElementRef, forwardRef, HostListener } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

@Directive({
  selector: '[jhiFormatAmount]',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => FormatAmountDirective),
      multi: true,
    },
  ],
})
export class FormatAmountDirective implements ControlValueAccessor {
  constructor(private el: ElementRef<HTMLInputElement>) {}

  @HostListener('input', ['$event'])
  onInput(event: InputEvent) {
    const input = event.target as HTMLInputElement;
    const previousCursor = input.selectionStart ?? input.value.length;

    const raw = input.value; // this.parse(input.value);
    const formatted = this.format(raw);

    this.el.nativeElement.value = formatted;
    this.onChange(raw ? Number(raw) : null);
    this.onTouched();

    // Restore cursor position at end of formatted string
    const newCursor = formatted.length - (raw.length - previousCursor);
    setTimeout(() => input.setSelectionRange(newCursor, newCursor));
  }

  @HostListener('blur')
  onBlur() {
    this.onTouched();
  }

  writeValue(value: any): void {
    this.el.nativeElement.value = this.format(value);
  }

  registerOnChange(fn: any): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: any): void {
    this.onTouched = fn;
  }

  private onChange = (value: any) => {};

  private onTouched = () => {};

  private parse(value: string): string {
    return value.replace(/\D/g, '');
  }

  private format(value: string | number): string {
    if (!value) {
      return '';
    }
    const raw = value.toString().replace(/\D/g, '');
    return raw.replace(/\B(?=(\d{3})+(?!\d))/g, ' ');
  }
}
