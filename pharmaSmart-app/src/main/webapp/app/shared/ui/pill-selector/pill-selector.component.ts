import {Component, forwardRef, input, output} from '@angular/core';
import {NG_VALUE_ACCESSOR} from '@angular/forms';

import {ControlValueAccessorBase} from '../forms/control-value-accessor.base';

export interface AppPillOption {
  label: string;
  value: unknown;
  icon?: string;
}

/**
 * Sélecteur à pilules du Design System .
 *
 * Reprend le style `.dashboard-periode-selector` / `.periode-pill` du tableau de bord
 * (`home-base.component.html`) : un groupe de boutons compacts sur fond neutre, l'option
 * active portant un dégradé plein. Contrairement à `p-selectbutton`, la sélection n'est
 * jamais vide une fois initialisée — comme l'original, il n'y a pas de mode `multiple`.
 *
 * @example
 * <app-pill-selector
 *   [items]="[{ label: 'Tout', value: 'TOUT' }, { label: 'Entrées', value: 'IN', icon: 'pi pi-arrow-up' }]"
 *   [(ngModel)]="typeFilter"
 * />
 *
 * Pour réagir au changement (ex. relancer une recherche), préférer `(selectionChange)` à
 * `(ngModelChange)` : combiné à `[(ngModel)]` sur le même élément, l'ordre d'écriture des
 * deux attributs déciderait lequel s'exécute en premier — `(ngModelChange)` peut alors lire
 * l'ANCIENNE valeur si un outil de formatage le replace avant `[(ngModel)]`. `(selectionChange)`
 * s'émet après la mise à jour du modèle, donc toujours à l'abri de ce piège.
 * @example
 * <app-pill-selector [items]="typeOptions" [(ngModel)]="typeFilter" (selectionChange)="onSearch()" />
 */
@Component({
  selector: 'app-pill-selector',
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => PillSelectorComponent),
    multi: true
  }],
  template: `
    <div class="app-pill-selector" role="group"
         [class.app-pill-selector--disabled]="isDisabled() || disabled()">
      @for (opt of items(); track opt.value) {
        <button
          type="button"
          class="app-pill"
          [class.active]="value() === opt.value"
          [disabled]="isDisabled() || disabled()"
          [title]="opt.label"
          (click)="select(opt.value)"
        >
          @if (opt.icon) {
            <i [class]="opt.icon" aria-hidden="true"></i>
          }
          <span class="app-pill__label">{{ opt.label }}</span>
        </button>
      }
    </div>
  `,
  styles: `
    .app-pill-selector {
      display: inline-flex;
      align-items: center;
      gap: 0.2rem;
      padding: 3px;
      background: rgba(226, 232, 240, 0.4);
      border-radius: 12px;
      flex-wrap: wrap;

      &--disabled {
        opacity: 0.6;
        pointer-events: none;
      }
    }

    .app-pill {
      display: inline-flex;
      align-items: center;
      gap: 0.3rem;
      padding: 0.28rem 0.7rem;
      border: none;
      border-radius: 9px;
      background: transparent;
      color: #888;
      font-size: 0.78rem;
      font-weight: 500;
      cursor: pointer;
      transition: all 0.2s ease;
      white-space: nowrap;

      i {
        font-size: 0.75rem;
      }

      &__label {
        font-size: 0.78rem;
      }

      &:hover:not(.active):not(:disabled) {
        background: rgba(0, 140, 186, 0.08);
        color: #008cba;
      }

      &.active {
        background: linear-gradient(135deg, #008cba 0%, #5bc0de 100%);
        color: #fff;
        font-weight: 600;
        box-shadow: 0 2px 8px rgba(0, 140, 186, 0.25);
      }
    }
  `,
})
export class PillSelectorComponent extends ControlValueAccessorBase<unknown> {
  readonly items = input.required<readonly AppPillOption[]>();

  readonly disabled = input<boolean>(false);

  /** Émis après la mise à jour du modèle — voir la note sur `(ngModelChange)` ci-dessus. */
  readonly selectionChange = output<unknown>();

  protected select(value: unknown): void {
    this.updateValue(value);
    this.selectionChange.emit(value);
  }
}
