import { Component, computed, forwardRef, input, signal } from '@angular/core';
import { NG_VALUE_ACCESSOR } from '@angular/forms';

import { ControlValueAccessorBase } from '../forms/control-value-accessor.base';

/**
 * Champ numérique du Design System — remplace `p-inputnumber`.
 *
 * Formatage français : espace insécable fine comme séparateur de milliers, virgule
 * décimale. Le champ affiche la valeur formatée au repos et la valeur brute pendant
 * la saisie — sinon le curseur saute à chaque frappe.
 *
 * À ne pas confondre avec la directive existante `[jhiFormatAmount]`, limitée aux
 * entiers ; ce composant gère les décimales, les négatifs et les bornes.
 *
 * @example
 * <app-input-number [(ngModel)]="quantite" [min]="0" />
 * <app-input-number [(ngModel)]="prix" [maxFractionDigits]="2" suffix=" F CFA" />
 * <app-input-number [(ngModel)]="qty" [showButtons]="true" [step]="1" />
 */
@Component({
  selector: 'app-input-number',
  providers: [{ provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => InputNumberComponent), multi: true }],
  template: `
    @if (showButtons()) {
      <div class="app-input-number-group" [class.app-input-number-group--sm]="size() === 'small'" [class.app-input-number-group--lg]="size() === 'large'">
        <button
          class="app-input-number-btn app-input-number-btn--start"
          type="button"
          [disabled]="isDisabled() || disabled() || readonly()"
          (click)="decrement()"
        >
          <i [class]="decrementButtonIcon()"></i>
        </button>
        <input
          type="text"
          [attr.inputmode]="inputMode()"
          [attr.id]="inputId() || null"
          [class]="inputClasses()"
          [value]="displayValue()"
          [placeholder]="placeholder()"
          [disabled]="isDisabled() || disabled()"
          [readOnly]="readonly()"
          [attr.aria-label]="ariaLabel() || null"
          (focus)="onFocus()"
          (input)="onInput($event)"
          (blur)="onBlur()"
        />
        <button
          class="app-input-number-btn app-input-number-btn--end"
          type="button"
          [disabled]="isDisabled() || disabled() || readonly()"
          (click)="increment()"
        >
          <i [class]="incrementButtonIcon()"></i>
        </button>
      </div>
    } @else {
      <input
        type="text"
        [attr.inputmode]="inputMode()"
        [attr.id]="inputId() || null"
        [class]="inputClasses()"
        [value]="displayValue()"
        [placeholder]="placeholder()"
        [disabled]="isDisabled() || disabled()"
        [readOnly]="readonly()"
        [attr.aria-label]="ariaLabel() || null"
        (focus)="onFocus()"
        (input)="onInput($event)"
        (blur)="onBlur()"
      />
    }
  `,
  styles: `
    :host {
      display: block;
    }

    // Groupe dédié plutôt que \`.input-group\` Bootstrap : le radius des boutons
    // d'extrémité est posé explicitement ci-dessous, sans dépendre des sélecteurs
    // \`:first-child\`/\`:last-child\` de Bootstrap (fragiles dès qu'un wrapper s'intercale).
    .app-input-number-group {
      display: flex;
      align-items: stretch;
    }

    .app-input-number-group .form-control {
      border-radius: 0;
      border-left: none;
      border-right: none;
      text-align: center;
      position: relative;
      z-index: 1;

      &:focus {
        z-index: 2;
      }
    }

    .app-input-number-btn {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      min-width: 2.25rem;
      padding-inline: 0.5rem;
      border: 1px solid var(--bs-border-color);
      background-color: var(--bs-tertiary-bg, #f1f3f5);
      color: var(--bs-secondary-color, #6c757d);
      transition: background-color 0.15s ease-in-out, color 0.15s ease-in-out, border-color 0.15s ease-in-out;

      &--start {
        border-top-left-radius: var(--bs-border-radius);
        border-bottom-left-radius: var(--bs-border-radius);
      }

      &--end {
        border-top-right-radius: var(--bs-border-radius);
        border-bottom-right-radius: var(--bs-border-radius);
      }

      &:hover:not(:disabled) {
        // Couleur primaire de la charte (émeraude Aura, cf. \`_pharma-bootstrap-palette.scss\`)
        // plutôt que \`--bs-secondary\`, un ton trop clair pour porter une icône blanche.
        background-color: var(--bs-primary);
        border-color: var(--bs-primary);
        color: #fff;
      }

      &:focus-visible {
        z-index: 2;
        outline: 2px solid var(--bs-primary);
        outline-offset: -1px;
      }

      &:disabled {
        opacity: 0.5;
        cursor: not-allowed;
      }

      i {
        font-size: 0.85rem;
      }
    }

    .app-input-number-group--sm .app-input-number-btn {
      min-width: 1.9rem;
      padding-inline: 0.4rem;

      i {
        font-size: 0.75rem;
      }
    }

    .app-input-number-group--lg .app-input-number-btn {
      min-width: 2.6rem;
      padding-inline: 0.65rem;
    }
  `,
})
export class InputNumberComponent extends ControlValueAccessorBase<number> {
  readonly placeholder = input<string>('');

  readonly size = input<'small' | 'normal' | 'large'>('normal');

  readonly disabled = input<boolean>(false);

  readonly readonly = input<boolean>(false);

  readonly invalid = input<boolean>(false);

  readonly min = input<number | undefined>(undefined);

  readonly max = input<number | undefined>(undefined);

  readonly minFractionDigits = input<number>(0);

  readonly maxFractionDigits = input<number>(0);

  /** Texte ajouté après la valeur au repos, ex. `" F CFA"` ou `" %"`. */
  readonly suffix = input<string>('');

  /** Texte ajouté avant la valeur au repos. */
  readonly prefix = input<string>('');

  readonly ariaLabel = input<string>('');

  /** `id` posé sur le champ, pour l'associer à un `<label for="…">` extérieur. */
  readonly inputId = input<string>('');

  readonly inputClass = input<string>('');

  /** Affiche les boutons +/- de part et d'autre du champ (remplace `[showButtons]` de `p-inputnumber`). */
  readonly showButtons = input<boolean>(false);

  /** Pas d'incrémentation/décrémentation des boutons +/-. */
  readonly step = input<number>(1);

  readonly incrementButtonIcon = input<string>('pi pi-plus');

  readonly decrementButtonIcon = input<string>('pi pi-minus');

  /** Pendant la saisie on affiche la frappe telle quelle ; le formatage n'a lieu qu'au blur. */
  private readonly editing = signal(false);
  private readonly rawInput = signal('');

  /** `numeric` par défaut (le projet manipule surtout des entiers) ; `decimal` dès que des décimales sont autorisées. */
  protected readonly inputMode = computed(() => (this.maxFractionDigits() > 0 ? 'decimal' : 'numeric'));

  protected readonly inputClasses = computed(() => {
    const classes = ['form-control'];
    if (this.size() === 'small') classes.push('form-control-sm');
    if (this.size() === 'large') classes.push('form-control-lg');
    if (this.invalid()) classes.push('is-invalid');
    if (this.inputClass()) classes.push(this.inputClass());
    return classes.join(' ');
  });

  protected readonly displayValue = computed(() => {
    if (this.editing()) {
      return this.rawInput();
    }
    const value = this.value();
    if (value === null || value === undefined || Number.isNaN(value)) {
      return '';
    }
    const formatted = new Intl.NumberFormat('fr-FR', {
      minimumFractionDigits: this.minFractionDigits(),
      maximumFractionDigits: Math.max(this.maxFractionDigits(), this.minFractionDigits()),
    }).format(value);
    return `${this.prefix()}${formatted}${this.suffix()}`;
  });

  protected onFocus(): void {
    const value = this.value();
    // Virgule décimale : c'est ce que l'utilisateur francophone tape naturellement.
    this.rawInput.set(value === null || value === undefined ? '' : String(value).replace('.', ','));
    this.editing.set(true);
  }

  protected onInput(event: Event): void {
    const raw = (event.target as HTMLInputElement).value;
    this.rawInput.set(raw);
    this.updateValue(this.parse(raw));
  }

  protected increment(): void {
    this.updateValue(this.clamp((this.value() ?? 0) + this.step()));
  }

  protected decrement(): void {
    this.updateValue(this.clamp((this.value() ?? 0) - this.step()));
  }

  protected onBlur(): void {
    this.editing.set(false);
    const clamped = this.clamp(this.value());
    if (clamped !== this.value()) {
      this.updateValue(clamped);
    }
    this.onTouched();
  }

  /** Accepte virgule ou point, ignore les séparateurs de milliers, refuse le reste. */
  private parse(raw: string): number | null {
    const normalized = raw.replace(/\s/g, '').replace(',', '.');
    if (normalized === '' || normalized === '-') {
      return null;
    }
    const parsed = Number(normalized);
    return Number.isNaN(parsed) ? null : parsed;
  }

  private clamp(value: number | null): number | null {
    if (value === null) {
      return null;
    }
    const min = this.min();
    const max = this.max();
    if (min !== undefined && value < min) return min;
    if (max !== undefined && value > max) return max;
    return value;
  }
}
