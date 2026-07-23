import {Component, forwardRef, input, TemplateRef, viewChild} from '@angular/core';
import {NgStyle, NgTemplateOutlet} from '@angular/common';
import {
  NgOptgroupTemplateDirective,
  NgOptionTemplateDirective,
  NgSelectComponent
} from '@ng-select/ng-select';
import {FormsModule, NG_VALUE_ACCESSOR} from '@angular/forms';

import {SelectBase} from './select.base';

/**
 * Liste déroulante à sélection unique avec recherche — remplace `p-select` (cas riches)
 * et `p-autocomplete`.
 *
 * Pour un `p-select` simple (moins de 20 options, sans recherche), préférer un
 * `<select class="form-select">` natif : inutile d'embarquer ng-select (cf. plan §3).
 *
 * @example
 * <app-select-search
 *   [items]="produits()"
 *   bindLabel="libelle"
 *   bindValue="id"
 *   placeholder="Choisir un produit"
 *   [(ngModel)]="produitId"
 * />
 */
@Component({
  selector: 'app-select-search',
  imports: [NgSelectComponent, FormsModule, NgStyle, NgTemplateOutlet, NgOptionTemplateDirective, NgOptgroupTemplateDirective],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => SelectSearchComponent),
    multi: true
  }],
  template: `
    <ng-select
      #ngSelect
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
      (ngModelChange)="onSelectionChange($event)"
      (search)="onSearched($event.term)"
      (scrollToEnd)="scrolledToEnd.emit()"
      (open)="onOpened()"
      (blur)="onTouched()"
    >
      @if (optionTemplate()) {
        <!--
          Le ng-template est déclaré ici et non projeté : ng-select récupère son
          gabarit d'option par @ContentChild, qui ne voit pas un contenu venu de
          l'appelant. On relaie donc un TemplateRef reçu en entrée.
        -->
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
export class SelectSearchComponent extends SelectBase<unknown> {
  /**
   * Gabarit de rendu d'une option — équivalent du `<ng-template #item>` de
   * `p-autocomplete`. L'élément est exposé en `$implicit` et sous le nom `item`.
   *
   * @example
   * <app-select-search [optionTemplate]="tplFacture" [items]="factures" />
   * <ng-template #tplFacture let-f>{{ f.numFacture }}</ng-template>
   */
  readonly optionTemplate = input<TemplateRef<unknown> | undefined>(undefined);

  /**
   * Gabarit de rendu d'un en-tête de groupe — équivalent du `<ng-template #group>` de
   * `p-select`. Actif uniquement avec `[groupBy]` renseigné.
   *
   * @example
   * <app-select-search [groupBy]="'groupLabel'" [groupTemplate]="tplGroup" [items]="items" />
   * <ng-template #tplGroup let-g><i [class]="g.icon"></i>{{ g.groupLabel }}</ng-template>
   */
  readonly groupTemplate = input<TemplateRef<unknown> | undefined>(undefined);

  /**
   * Calcule la valeur transmise à `groupTemplate` — équivalent du `groupValue` de ng-select.
   * Reçoit la clé de regroupement et les éléments du groupe ; par défaut, ng-select renvoie
   * simplement la clé telle quelle.
   *
   * @example
   * <app-select-search [groupBy]="'groupLabel'" [groupValueFn]="fn" [groupTemplate]="tpl" [items]="items" />
   */
  readonly groupValueFn = input<((key: unknown, children: unknown[]) => unknown) | undefined>(undefined);

  /**
   * Empêche l'ouverture du panneau tant qu'il n'y a rien à y montrer — au focus/clic, avec
   * `[typeahead]` branché et aucun terme saisi, ng-select ouvre quand même un panneau vide
   * ne contenant que `[typeToSearchText]`. Mettre à `false` referme ce panneau
   * immédiatement ; il ne réapparaît qu'une fois un terme valide saisi (ou des résultats
   * disponibles).
   *
   * @example
   * <app-select-search [items]="items" [typeahead]="search$" [openWhenEmpty]="false" />
   */
  readonly openWhenEmpty = input<boolean>(false);

  private readonly ngSelectRef = viewChild.required('ngSelect', {read: NgSelectComponent});

  /** Ferme le panneau d'options — équivalent de `AutoComplete.hide()` de PrimeNG. */
  close(): void {
    this.ngSelectRef().close();
  }

  /** Panneau d'options actuellement ouvert. */
  isOpen(): boolean {
    return this.ngSelectRef().isOpen();
  }

  /** Reboucle l'ouverture : referme aussitôt si `openWhenEmpty` est désactivé et le panneau n'a rien à montrer. */
  protected onOpened(): void {
    if (!this.openWhenEmpty() && this.ngSelectRef().showTypeToSearch()) {
      this.ngSelectRef().close();
    }
  }
}
