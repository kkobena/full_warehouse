import { Pipe, PipeTransform } from '@angular/core';

import { formatCurrency, formatDecimal, formatMontantAbrege, formatNumber } from 'app/shared/utils/format-utils';
import { UniteIndicateur } from '../../models/pilotage.model';

/**
 * Valeur d'un indicateur selon son unité : montants abrégés dans les tuiles et graphiques (« 19,2 M »), complets dans les
 * tableaux (`complet`), taux en %, ratios à deux décimales ; « — » si la valeur manque.
 */
@Pipe({ name: 'valeurIndicateur' })
export class ValeurIndicateurPipe implements PipeTransform {
  transform(valeur: number | null | undefined, unite: UniteIndicateur, complet = false): string {
    if (valeur === null || valeur === undefined) {
      return '—';
    }
    switch (unite) {
      case 'MONTANT':
        return complet ? formatCurrency(valeur) : formatMontantAbrege(valeur);
      case 'POURCENTAGE':
        return `${formatDecimal(valeur, 1)} %`;
      case 'RATIO':
        return formatDecimal(valeur, 2);
      default:
        return Number.isInteger(valeur) ? formatNumber(valeur) : formatDecimal(valeur, 1);
    }
  }
}
