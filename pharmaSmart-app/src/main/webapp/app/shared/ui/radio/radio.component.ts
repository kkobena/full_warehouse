import { Component, computed, forwardRef, input } from '@angular/core';
import { NG_VALUE_ACCESSOR } from '@angular/forms';

import { ControlValueAccessorBase } from '../forms/control-value-accessor.base';

let nextId = 0;

/**
 * Bouton radio du Design System — remplace `p-radiobutton`.
 * Rend le `.form-check` natif de Bootstrap 5 avec `type="radio"`.
 *
 * Comme `p-radiobutton`, chaque instance représente **une** option : plusieurs radios
 * partagent le même `name` et le même modèle, et `value` désigne l'option portée.
 *
 * @example
 * <app-radio name="mode" [value]="'especes'" label="Espèces" [(ngModel)]="mode" />
 * <app-radio name="mode" [value]="'carte'"   label="Carte"   [(ngModel)]="mode" />
 */
@Component({
  selector: 'app-radio',
  providers: [{ provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => RadioComponent), multi: true }],
  template: `
    <div class="form-check" [class.form-check-inline]="inline()">
      <input
        type="radio"
        class="form-check-input"
        [id]="inputId"
        [name]="name()"
        [checked]="checked()"
        [disabled]="isDisabled() || disabled()"
        [class.is-invalid]="invalid()"
        (change)="onSelected()"
        (blur)="onTouched()"
      />
      @if (label()) {
        <label class="form-check-label" [for]="inputId">{{ label() }}</label>
      }
    </div>
  `,
})
export class RadioComponent extends ControlValueAccessorBase<unknown> {
  /** Valeur portée par cette option ; devient la valeur du modèle une fois sélectionnée. */
  readonly optionValue = input.required<unknown>({ alias: 'value' });

  /** Regroupe les radios entre eux — obligatoire pour l'exclusivité native du navigateur. */
  readonly name = input.required<string>();

  readonly label = input<string>('');

  readonly inline = input<boolean>(false);

  readonly disabled = input<boolean>(false);

  readonly invalid = input<boolean>(false);

  protected readonly inputId = `app-radio-${nextId++}`;

  protected readonly checked = computed(() => this.value() === this.optionValue());

  protected onSelected(): void {
    this.updateValue(this.optionValue());
  }
}
