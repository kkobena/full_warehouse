import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { KpiItemComponent, KpiStripComponent } from 'app/shared/ui';
import { CelluleAnalyse, SensFavorable, UniteIndicateur } from '../../models/pilotage.model';
import { ValeurIndicateurPipe } from '../valeur-indicateur/valeur-indicateur.pipe';
import { VariationComponent } from '../variation/variation.component';

/** Une tuile : une valeur (et sa référence, s'il y en a une) d'unité et de sens favorable connus. */
export interface TuileCellule {
  libelle: string;
  definition: string;
  unite: UniteIndicateur;
  sens: SensFavorable;
  cellule: CelluleAnalyse;
}

/** Rangée de tuiles pour des mesures hors dictionnaire (délai de livraison, taux de rupture…), variation sous chaque valeur. */
@Component({
  selector: 'app-tuiles-cellules',
  imports: [KpiStripComponent, KpiItemComponent, VariationComponent, ValeurIndicateurPipe],
  template: `
    <app-kpi-strip [loading]="loading()" [skeletonCount]="tuiles().length || 4">
      @for (tuile of tuiles(); track tuile.libelle) {
        <app-kpi-item [label]="tuile.libelle" [info]="tuile.definition" [value]="tuile.cellule.valeur | valeurIndicateur: tuile.unite">
          <ng-container ngProjectAs="[kpiSub]">
            @if (tuile.cellule.valeurReference !== null) {
              <app-variation [ecartPct]="tuile.cellule.ecartPct" [ecart]="tuile.cellule.ecart" [sensFavorable]="tuile.sens" [unite]="tuile.unite" />
            }
          </ng-container>
        </app-kpi-item>
      }
    </app-kpi-strip>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TuilesCellulesComponent {
  readonly tuiles = input.required<readonly TuileCellule[]>();
  readonly loading = input(false);
}

/** Cellule d'une valeur seule, sans référence. */
export function celluleSeule(valeur: number | null): CelluleAnalyse {
  return { valeur, valeurReference: null, ecart: null, ecartPct: null };
}
