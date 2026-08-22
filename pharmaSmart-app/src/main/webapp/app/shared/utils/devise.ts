import {Directive, ElementRef, inject, Pipe, PipeTransform} from '@angular/core';

import {currencySymbol, formatCurrencyWithUnit} from './format-utils';

/**
 * Écrit la devise de l'officine dans l'élément qui la porte.
 *
 * @example <span class="balance-item-currency" appDevise></span>
 */
@Directive({selector: '[appDevise]'})
export class DeviseDirective {
  constructor() {
    inject(ElementRef).nativeElement.textContent = currencySymbol();
  }
}

/**
 * La devise, ou un montant suivi de la devise, pour les contextes où une directive n'a pas sa
 * place : liaison d'attribut (`[suffix]`), interpolation dans un texte composé.
 *
 * @example {{ '' | devise }}        → « FCFA »
 * @example {{ montant | devise }}   → « 1 234 FCFA »
 */
@Pipe({name: 'devise'})
export class DevisePipe implements PipeTransform {
  transform(value?: number | string | null): string {
    if (value === null || value === undefined || value === '') {
      return currencySymbol();
    }
    return typeof value === 'number' ? formatCurrencyWithUnit(value) : `${value} ${currencySymbol()}`;
  }
}
