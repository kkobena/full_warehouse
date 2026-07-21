import { Component, forwardRef } from '@angular/core';
import { NgStyle } from '@angular/common';
import { FormsModule, NG_VALUE_ACCESSOR } from '@angular/forms';
import { NgSelectComponent } from '@ng-select/ng-select';

import { SelectBase } from './select.base';

/**
 * Liste déroulante simple, sans recherche — remplace `p-select` dans son usage courant.
 *
 * **Pourquoi pas un `<select>` natif** (ce que suggérait le plan §3) : les `<option>`
 * d'un select natif sont dessinées par le système, et leur surbrillance est la couleur
 * d'accentuation de l'OS. Impossible de l'aligner sur la charte — le survol jurait avec
 * le reste de l'application. `ng-select` rend sa liste en DOM, donc
 * `content/scss/_ng-select-pharma.scss` l'habille avec les tokens Aura
 * (`--p-list-option-focus-background`…) : le survol est exactement celui de `p-select`,
 * sans une ligne de CSS supplémentaire.
 *
 * La lib est de toute façon déjà chargée par `AppSelectSearch` et `AppMultiSelect`.
 *
 * Pour une recherche ou un chargement distant, utiliser `<app-select-search>` ;
 * pour du multiple, `<app-multi-select>`.
 *
 * @example
 * <app-select
 *   [items]="categories"
 *   bindLabel="libelle"
 *   bindValue="id"
 *   placeholder="Sélectionner une catégorie"
 *   formControlName="categorieId"
 * />
 *
 * <!-- Dans une modale, sans quoi le panneau est rogné par le scroll du corps -->
 * <app-select [items]="categories" appendTo="body" formControlName="categorieId" />
 *
 * <!-- Largeur imposée depuis l'appelant, équivalent du [style] de p-select -->
 * <app-select [items]="type" [style]="{ 'min-width': '180px' }" [(ngModel)]="typeSelected" />
 */
@Component({
  selector: 'app-select',
  imports: [NgSelectComponent, FormsModule, NgStyle],
  providers: [{ provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => SelectComponent), multi: true }],
  template: `
    <ng-select
      [items]="items()"
      [bindLabel]="bindLabel()"
      [bindValue]="bindValue()"
      [placeholder]="placeholder()"
      [ngModel]="value()"
      [ngModelOptions]="{ standalone: true }"
      [disabled]="isDisabled() || disabled()"
      [loading]="loading()"
      [clearable]="clearable()"
      [groupBy]="groupBy()"
      [notFoundText]="notFoundText()"
      [loadingText]="loadingText()"
      [appendTo]="appendTo()"
      [dropdownPosition]="dropdownPosition()"
      [labelForId]="inputId()"
      [attr.aria-label]="ariaLabel() || null"
      [class]="ngSelectClasses()"
      [ngStyle]="style()"
      [searchable]="false"
      (ngModelChange)="onSelectionChange($event)"
      (blur)="onTouched()"
    />
  `,
})
export class SelectComponent extends SelectBase<unknown> {}
