import {ChangeDetectionStrategy, Component, forwardRef, input, TemplateRef} from '@angular/core';
import { NgStyle, NgTemplateOutlet } from '@angular/common';
import { FormsModule, NG_VALUE_ACCESSOR } from '@angular/forms';
import { NgMultiLabelTemplateDirective, NgOptgroupTemplateDirective, NgOptionTemplateDirective, NgSelectComponent } from '@ng-select/ng-select';

import { SelectBase } from './select.base';

/**
 * Liste déroulante à sélection multiple — remplace `p-multiselect`.
 *
 * Les options retenues s'affichent en chips, calées sur la teinte `p-multiselect`
 * du preset Aura (cf. `content/scss/_ng-select-pharma.scss`). Le champ reste sur une ligne :
 * au-delà de {@link maxLabels} chips, une pastille « +N » (liste complète en infobulle). Sans cela,
 * chaque choix pouvait ajouter une ligne et faire sauter la barre qui contient le champ.
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
  imports: [
    NgSelectComponent,
    FormsModule,
    NgStyle,
    NgTemplateOutlet,
    NgOptionTemplateDirective,
    NgOptgroupTemplateDirective,
    NgMultiLabelTemplateDirective,
  ],
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
      <ng-template ng-multi-label-tmp let-items="items" let-clear="clear">
        @for (item of items.slice(0, maxLabels()); track $index) {
          <div class="ng-value" [title]="libelle(item)">
            <span class="ng-value-icon left" role="button" tabindex="0" [attr.aria-label]="'Retirer ' + libelle(item)"
                  (click)="clear(item)" (keydown.enter)="clear(item)">×</span>
            <span class="ng-value-label">{{ libelle(item) }}</span>
          </div>
        }
        @if (items.length > maxLabels()) {
          <div class="ng-value ng-value-plus" [title]="libelles(items.slice(maxLabels()))">+{{ items.length - maxLabels() }}</div>
        }
      </ng-template>
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
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MultiSelectComponent extends SelectBase<unknown[]> {
  /** Referme le panneau après chaque choix. `false` par défaut : on enchaîne les sélections. */
  readonly closeOnSelect = input<boolean>(false);

  /** Retire de la liste les options déjà choisies. */
  readonly hideSelected = input<boolean>(false);

  readonly maxSelectedItems = input<number | undefined>(undefined);

  /** Chips affichées avant la pastille « +N ». */
  readonly maxLabels = input<number>(2);

  /** Gabarit de rendu d'une option. Voir `SelectSearchComponent.optionTemplate`. */
  readonly optionTemplate = input<TemplateRef<unknown> | undefined>(undefined);

  /** Gabarit de rendu d'un en-tête de groupe. Voir `SelectSearchComponent.groupTemplate`. */
  readonly groupTemplate = input<TemplateRef<unknown> | undefined>(undefined);

  /** Voir `SelectSearchComponent.groupValueFn`. */
  readonly groupValueFn = input<((key: unknown, children: unknown[]) => unknown) | undefined>(undefined);

  /** Libellé d'un élément choisi : `bindLabel` (chemin pointé admis) sur un objet, la valeur elle-même sinon. */
  protected libelle(item: unknown): string {
    const chemin = this.bindLabel();
    if (!chemin || item === null || typeof item !== 'object') {
      return String(item ?? '');
    }
    const valeur = chemin.split('.').reduce<unknown>((obj, cle) => (obj as Record<string, unknown> | null)?.[cle], item);
    return String(valeur ?? '');
  }

  protected libelles(items: unknown[]): string {
    return items.map(item => this.libelle(item)).join(', ');
  }
}
