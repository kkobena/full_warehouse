import { Directive, signal } from '@angular/core';
import { ControlValueAccessor } from '@angular/forms';

/**
 * Socle `ControlValueAccessor` commun aux composants de formulaire du Design System
 * (`AppInput`, `AppInputNumber`, `AppCheckbox`, `AppRadio`, `AppSwitch`, `AppPassword`).
 *
 * Se substitue au boilerplate répété dans chaque composant — le pattern est celui de
 * `pharma-date-picker.component.ts` (cf. plan de migration §5.5), transposé en signals.
 *
 * Chaque composant concret doit déclarer le provider :
 * ```ts
 * providers: [{ provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => MonComposant), multi: true }]
 * ```
 */
@Directive()
export abstract class ControlValueAccessorBase<T> implements ControlValueAccessor {
  /** Valeur courante, alimentée par le formulaire ou par la saisie utilisateur. */
  protected readonly value = signal<T | null>(null);

  /** Piloté par Angular via `setDisabledState` (ex. `FormControl.disable()`). */
  protected readonly isDisabled = signal(false);

  private onChange: (value: T | null) => void = () => undefined;

  /** À appeler au blur, pour que l'état `touched` du contrôle soit correct. */
  protected onTouched: () => void = () => undefined;

  writeValue(value: T | null): void {
    this.value.set(value);
  }

  registerOnChange(fn: (value: T | null) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.isDisabled.set(isDisabled);
  }

  /** Met à jour la valeur ET notifie le formulaire. À utiliser depuis le template. */
  protected updateValue(value: T | null): void {
    this.value.set(value);
    this.onChange(value);
  }
}
