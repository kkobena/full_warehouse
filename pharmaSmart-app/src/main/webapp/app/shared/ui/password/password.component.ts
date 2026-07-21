import { Component, computed, forwardRef, input, signal } from '@angular/core';
import { NG_VALUE_ACCESSOR } from '@angular/forms';

import { ControlValueAccessorBase } from '../forms/control-value-accessor.base';

/**
 * Champ mot de passe du Design System — remplace `p-password`.
 * Un `<input class="form-control">` dans un `.input-group`, avec bouton afficher/masquer.
 *
 * @example
 * <app-password placeholder="Mot de passe" [(ngModel)]="motDePasse" />
 */
@Component({
  selector: 'app-password',
  providers: [{ provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => PasswordComponent), multi: true }],
  template: `
    <div class="input-group" [class.input-group-sm]="size() === 'small'" [class.input-group-lg]="size() === 'large'">
      <input
        [type]="revealed() ? 'text' : 'password'"
        [class]="inputClasses()"
        [value]="value() ?? ''"
        [placeholder]="placeholder()"
        [disabled]="isDisabled() || disabled()"
        [required]="required()"
        [attr.autocomplete]="autocomplete()"
        [attr.aria-label]="ariaLabel() || null"
        (input)="onInput($event)"
        (blur)="onTouched()"
      />
      @if (toggleMask()) {
        <button
          type="button"
          class="btn btn-outline-secondary"
          [disabled]="isDisabled() || disabled()"
          [attr.aria-label]="revealed() ? 'Masquer le mot de passe' : 'Afficher le mot de passe'"
          [attr.aria-pressed]="revealed()"
          (click)="toggleReveal()"
        >
          <i [class]="revealed() ? 'pi pi-eye-slash' : 'pi pi-eye'" aria-hidden="true"></i>
        </button>
      }
    </div>
  `,
})
export class PasswordComponent extends ControlValueAccessorBase<string> {
  readonly placeholder = input<string>('');

  readonly size = input<'small' | 'normal' | 'large'>('normal');

  readonly disabled = input<boolean>(false);

  readonly required = input<boolean>(false);

  readonly invalid = input<boolean>(false);

  /** Affiche le bouton œil permettant de révéler la saisie. */
  readonly toggleMask = input<boolean>(true);

  /** `current-password` à la connexion, `new-password` à l'inscription. */
  readonly autocomplete = input<string>('current-password');

  readonly ariaLabel = input<string>('');

  private readonly _revealed = signal(false);
  protected readonly revealed = this._revealed.asReadonly();

  protected readonly inputClasses = computed(() => (this.invalid() ? 'form-control is-invalid' : 'form-control'));

  protected toggleReveal(): void {
    this._revealed.update(revealed => !revealed);
  }

  protected onInput(event: Event): void {
    this.updateValue((event.target as HTMLInputElement).value);
  }
}
