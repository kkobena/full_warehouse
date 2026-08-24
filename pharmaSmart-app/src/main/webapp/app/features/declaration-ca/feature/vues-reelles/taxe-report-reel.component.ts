import {ChangeDetectionStrategy, Component} from '@angular/core';

import {TaxeReportComponent} from 'app/entities/mvt-caisse/taxe-report/taxe-report.component';

/** Rapport TVA en mode réel. Fichier séparé : cf. `balance-reelle.component.ts`. */
@Component({
  selector: 'app-taxe-report-reel',
  imports: [TaxeReportComponent],
  template: `
    <app-taxe-report mode="REEL" navCode="declaration-ca.taxe-report-reel" />`,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TaxeReportReelComponent {
}
