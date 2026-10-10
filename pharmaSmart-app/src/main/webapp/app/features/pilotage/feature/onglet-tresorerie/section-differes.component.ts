import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';

import { CardComponent, DataTableComponent } from 'app/shared/ui';
import { PilotageApiService } from '../../data-access/pilotage-api.service';
import { RequetePilotage } from '../../models/pilotage.model';
import { BarresComponent, SerieBarres } from '../../ui/barres/barres.component';
import { TuileCellule, TuilesCellulesComponent, celluleSeule } from '../../ui/tuiles-cellules/tuiles-cellules.component';
import { ValeurIndicateurPipe } from '../../ui/valeur-indicateur/valeur-indicateur.pipe';

/** Différés & crédit : reste dû des ventes différées (à date), par client et par ancienneté ; avoirs clients. */
@Component({
  selector: 'app-section-differes',
  imports: [CardComponent, DataTableComponent, TuilesCellulesComponent, BarresComponent, ValeurIndicateurPipe],
  templateUrl: './section-differes.component.html',
  styleUrl: '../../ui/section-onglet.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SectionDifferesComponent {
  readonly requete = input.required<RequetePilotage>();

  private readonly api = inject(PilotageApiService);

  protected readonly differes = rxResource({ params: () => this.requete(), stream: ({ params }) => this.api.lireDifferes(params) });

  protected readonly tuiles = computed<TuileCellule[]>(() => {
    const differes = this.differes.value();
    if (!differes) {
      return [];
    }
    return [
      { libelle: 'Différés à recouvrer', definition: 'Reste dû des ventes différées, à date.', unite: 'MONTANT', sens: 'BAISSE', cellule: celluleSeule(differes.encours) },
      { libelle: 'Avoirs émis', definition: 'Avoirs clients créés sur la période.', unite: 'MONTANT', sens: 'BAISSE', cellule: differes.avoirsEmis },
      { libelle: 'Avoirs remboursés', definition: 'Avoirs clôturés par remboursement (espèces ou carte).', unite: 'MONTANT', sens: 'NEUTRE', cellule: differes.avoirsRembourses },
    ];
  });

  protected readonly libelles = computed(() => (this.differes.value()?.vieillissement ?? []).map(t => t.libelle));
  protected readonly series = computed<SerieBarres[]>(() => [{ libelle: 'Reste dû', valeurs: (this.differes.value()?.vieillissement ?? []).map(t => t.montant) }]);
}
