import { ChangeDetectionStrategy, Component } from '@angular/core';

import { TableauPharmacienComponent } from 'app/entities/mvt-caisse/tableau-pharmacien/tableau-pharmacien.component';

/** Tableau du pharmacien en mode réel. Fichier séparé : cf. `balance-reelle.component.ts`. */
@Component({
  selector: 'app-tableau-pharmacien-reel',
  imports: [TableauPharmacienComponent],
  template: ` <app-tableau-pharmacien mode="REEL" navCode="declaration-ca.tableau-pharmacien-reel" />`,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TableauPharmacienReelComponent {}
