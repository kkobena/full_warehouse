import { Component, forwardRef, input } from '@angular/core';
import { NG_VALUE_ACCESSOR } from '@angular/forms';

import { ControlValueAccessorBase } from '../forms/control-value-accessor.base';

let nextId = 0;

/**
 * Case à cocher du Design System — remplace `p-checkbox`.
 * Rend le `.form-check` natif de Bootstrap 5.
 *
 * @example
 * <app-checkbox label="Accepter les conditions" [(ngModel)]="accepted" />
 */
@Component({
  selector: 'app-checkbox',
  providers: [{ provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => CheckboxComponent), multi: true }],
  template: `
    <div class="form-check" [class.form-check-inline]="inline()">
      <input
        type="checkbox"
        class="form-check-input"
        [id]="inputId"
        [checked]="value() ?? false"
        [disabled]="isDisabled() || disabled()"
        [class.is-invalid]="invalid()"
        (change)="onCheckedChange($event)"
        (blur)="onTouched()"
      />
      @if (label()) {
        <label class="form-check-label" [for]="inputId">{{ label() }}</label>
      }
    </div>
  `,
})
export class CheckboxComponent extends ControlValueAccessorBase<boolean> {
  readonly label = input<string>('');

  /** Aligne plusieurs cases sur une même ligne (`.form-check-inline`). */
  readonly inline = input<boolean>(false);

  /** Désactivation statique. `setDisabledState` du formulaire reste prioritaire. */
  readonly disabled = input<boolean>(false);

  readonly invalid = input<boolean>(false);

  /** L'id lie `<input>` et `<label>` : indispensable pour que le clic sur le libellé coche. */
  protected readonly inputId = `app-checkbox-${nextId++}`;

  protected onCheckedChange(event: Event): void {
    this.updateValue((event.target as HTMLInputElement).checked);
  }
}
