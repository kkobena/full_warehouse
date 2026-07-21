import { Component, forwardRef, input } from '@angular/core';
import { NG_VALUE_ACCESSOR } from '@angular/forms';

import { ControlValueAccessorBase } from '../forms/control-value-accessor.base';

let nextId = 0;

/**
 * Interrupteur du Design System — remplace `p-toggleswitch`.
 * Rend le `.form-switch` natif de Bootstrap 5 (une case à cocher avec `role="switch"`).
 *
 * @example
 * <app-switch label="Notifications" [(ngModel)]="enabled" />
 */
@Component({
  selector: 'app-switch',
  providers: [{ provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => SwitchComponent), multi: true }],
  template: `
    <div class="form-check form-switch" [class.form-check-inline]="inline()">
      <input
        type="checkbox"
        role="switch"
        class="form-check-input"
        [id]="resolvedId()"
        [checked]="value() ?? false"
        [disabled]="isDisabled() || disabled()"
        [class.is-invalid]="invalid()"
        (change)="onCheckedChange($event)"
        (blur)="onTouched()"
      />
      @if (label()) {
        <label class="form-check-label" [for]="resolvedId()">{{ label() }}</label>
      }
    </div>
  `,
})
export class SwitchComponent extends ControlValueAccessorBase<boolean> {
  readonly label = input<string>('');

  readonly inline = input<boolean>(false);

  readonly disabled = input<boolean>(false);

  readonly invalid = input<boolean>(false);

  /**
   * `id` posé sur la case, pour qu'un `<label for="…">` extérieur au composant puisse s'y
   * associer. Sans valeur, un identifiant unique est généré.
   */
  readonly inputId = input<string>('');

  private readonly fallbackId = `app-switch-${nextId++}`;

  protected resolvedId(): string {
    return this.inputId() || this.fallbackId;
  }

  protected onCheckedChange(event: Event): void {
    this.updateValue((event.target as HTMLInputElement).checked);
  }
}
