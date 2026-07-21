import { Component, computed, forwardRef, input } from '@angular/core';
import { NG_VALUE_ACCESSOR } from '@angular/forms';

import { ControlValueAccessorBase } from '../forms/control-value-accessor.base';

/**
 * Champ texte du Design System — remplace `p-inputtext` / `pInputText`.
 * Rend un `<input class="form-control">` natif de Bootstrap 5.
 *
 * @example
 * <app-input placeholder="Nom du produit" [(ngModel)]="nom" />
 * <app-input type="email" size="small" [invalid]="emailInvalide()" [(ngModel)]="email" />
 */
@Component({
  selector: 'app-input',
  providers: [{ provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => InputComponent), multi: true }],
  template: `
    <input
      [type]="type()"
      [class]="inputClasses()"
      [value]="value() ?? ''"
      [placeholder]="placeholder()"
      [disabled]="isDisabled() || disabled()"
      [readOnly]="readonly()"
      [required]="required()"
      [attr.maxlength]="maxlength()"
      [attr.autocomplete]="autocomplete() || null"
      [attr.aria-label]="ariaLabel() || null"
      (input)="onInput($event)"
      (blur)="onTouched()"
    />
  `,
})
export class InputComponent extends ControlValueAccessorBase<string> {
  readonly type = input<'text' | 'email' | 'tel' | 'url' | 'search'>('text');

  readonly placeholder = input<string>('');

  readonly size = input<'small' | 'normal' | 'large'>('normal');

  readonly disabled = input<boolean>(false);

  readonly readonly = input<boolean>(false);

  readonly required = input<boolean>(false);

  /** Affiche l'état d'erreur Bootstrap (`.is-invalid`). */
  readonly invalid = input<boolean>(false);

  readonly maxlength = input<number | undefined>(undefined);

  readonly autocomplete = input<string>('');

  readonly ariaLabel = input<string>('');

  /** Classes additionnelles posées sur l'`<input>`. */
  readonly inputClass = input<string>('');

  protected readonly inputClasses = computed(() => {
    const classes = ['form-control'];
    if (this.size() === 'small') classes.push('form-control-sm');
    if (this.size() === 'large') classes.push('form-control-lg');
    if (this.invalid()) classes.push('is-invalid');
    if (this.inputClass()) classes.push(this.inputClass());
    return classes.join(' ');
  });

  protected onInput(event: Event): void {
    this.updateValue((event.target as HTMLInputElement).value);
  }
}
