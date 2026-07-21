import { Component, forwardRef, input, TemplateRef } from '@angular/core';
import { NgStyle, NgTemplateOutlet } from '@angular/common';
import { FormsModule, NG_VALUE_ACCESSOR } from '@angular/forms';
import { NgOptgroupTemplateDirective, NgOptionTemplateDirective, NgSelectComponent } from '@ng-select/ng-select';

import { SelectBase } from './select.base';

/**
 * Liste déroulante à sélection multiple — remplace `p-multiselect`.
 *
 * Les options retenues s'affichent en chips, calées sur la teinte `p-multiselect`
 * du preset Aura (cf. `content/scss/_ng-select-pharma.scss`).
 *
 * @example
 * <app-multi-select
 *   [items]="rayons()"
 *   bindLabel="nom"
 *   bindValue="id"
 *   placeholder="Filtrer par rayon"
 *   [(ngModel)]="rayonIds"
 * />
 */
@Component({
  selector: 'app-multi-select',
  imports: [NgSelectComponent, FormsModule, NgStyle, NgTemplateOutlet, NgOptionTemplateDirective, NgOptgroupTemplateDirective],
  providers: [{ provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => MultiSelectComponent), multi: true }],
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
      [searchable]="searchable()"
      [virtualScroll]="virtualScroll()"
      [groupBy]="groupBy()"
      [groupValue]="groupValueFn()"
      [notFoundText]="notFoundText()"
      [loadingText]="loadingText()"
      [typeToSearchText]="typeToSearchText()"
      [appendTo]="appendTo()"
      [dropdownPosition]="dropdownPosition()"
      [typeahead]="typeahead()"
      [minTermLength]="minSearchLength()"
      [labelForId]="inputId()"
      [attr.aria-label]="ariaLabel() || null"
      [class]="ngSelectClasses()"
      [ngStyle]="style()"
      [multiple]="true"
      [closeOnSelect]="closeOnSelect()"
      [hideSelected]="hideSelected()"
      [maxSelectedItems]="maxSelectedItems()"
      (ngModelChange)="onSelectionChange($event)"
      (search)="onSearched($event.term)"
      (scrollToEnd)="scrolledToEnd.emit()"
      (blur)="onTouched()"
    >
      @if (optionTemplate()) {
        <ng-template ng-option-tmp let-item="item" let-index="index" let-search="searchTerm">
          <ng-container
            [ngTemplateOutlet]="optionTemplate()!"
            [ngTemplateOutletContext]="{ $implicit: item, item, index, search }"
          />
        </ng-template>
      }
      @if (groupTemplate()) {
        <ng-template ng-optgroup-tmp let-item="item">
          <ng-container
            [ngTemplateOutlet]="groupTemplate()!"
            [ngTemplateOutletContext]="{ $implicit: item, item }"
          />
        </ng-template>
      }
    </ng-select>
  `,
})
export class MultiSelectComponent extends SelectBase<unknown[]> {
  /** Referme le panneau après chaque choix. `false` par défaut : on enchaîne les sélections. */
  readonly closeOnSelect = input<boolean>(false);

  /** Retire de la liste les options déjà choisies. */
  readonly hideSelected = input<boolean>(false);

  readonly maxSelectedItems = input<number | undefined>(undefined);

  /** Gabarit de rendu d'une option. Voir `SelectSearchComponent.optionTemplate`. */
  readonly optionTemplate = input<TemplateRef<unknown> | undefined>(undefined);

  /** Gabarit de rendu d'un en-tête de groupe. Voir `SelectSearchComponent.groupTemplate`. */
  readonly groupTemplate = input<TemplateRef<unknown> | undefined>(undefined);

  /** Voir `SelectSearchComponent.groupValueFn`. */
  readonly groupValueFn = input<((key: unknown, children: unknown[]) => unknown) | undefined>(undefined);
}
