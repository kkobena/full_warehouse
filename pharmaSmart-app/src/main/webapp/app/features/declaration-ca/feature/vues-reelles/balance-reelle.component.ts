import { ChangeDetectionStrategy, Component } from '@angular/core';

import { BalanceMvtCaisseComponent } from 'app/entities/mvt-caisse/balance-mvt-caisse/balance-mvt-caisse.component';

/**
 * Balance de caisse en mode réel — l'écran de comptabilité, forcé sur le CA non retraité.
 *
 * <p>Un fichier par vue, et surtout pas un fichier commun aux trois : ces composants ne sont
 * atteints que par un bloc `@defer`, et Angular découpe le code par module. Réunis, ils formaient un
 * seul chunk tirant Chart.js et AG Grid d'un bloc — ouvrir la balance chargeait aussi le rapport TVA
 * et le tableau pharmacien, soit près de 3 Mo pour un écran qui n'en demandait qu'un tiers.
 */
@Component({
  selector: 'app-balance-reelle',
  imports: [BalanceMvtCaisseComponent],
  template: ` <app-balance-mvt-caisse mode="REEL" />`,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BalanceReelleComponent {}
