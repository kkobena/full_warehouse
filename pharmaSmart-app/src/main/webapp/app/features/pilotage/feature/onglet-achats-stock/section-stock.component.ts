import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';

import { CardComponent, DataTableComponent } from 'app/shared/ui';
import { PilotageApiService } from '../../data-access/pilotage-api.service';
import { RequetePilotage } from '../../models/pilotage.model';
import { TuileCellule, TuilesCellulesComponent, celluleSeule } from '../../ui/tuiles-cellules/tuiles-cellules.component';
import { ValeurIndicateurPipe } from '../../ui/valeur-indicateur/valeur-indicateur.pipe';
import { CourbeSeriesComponent, SerieCourbe } from '../../ui/courbe-series/courbe-series.component';
import { AideComponent } from '../../ui/aide/aide.component';

/** Stock : photographie de fin de mois face à N-1, rotation, couverture, dormants ; par famille. */
@Component({
  selector: 'app-section-stock',
  imports: [AideComponent, DatePipe, CardComponent, DataTableComponent, TuilesCellulesComponent, CourbeSeriesComponent, ValeurIndicateurPipe],
  templateUrl: './section-stock.component.html',
  styleUrl: '../../ui/section-onglet.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SectionStockComponent {
  readonly requete = input.required<RequetePilotage>();

  private readonly api = inject(PilotageApiService);
  private readonly datePipe = new DatePipe('fr');

  protected readonly stock = rxResource({ params: () => this.requete(), stream: ({ params }) => this.api.lireStock(params) });

  protected readonly tuiles = computed<TuileCellule[]>(() => {
    const stock = this.stock.value();
    if (!stock) {
      return [];
    }
    return [
      { libelle: 'Valeur du stock', definition: 'Photographie de fin de mois, au prix d’achat, face au même mois N-1.', unite: 'MONTANT', sens: 'NEUTRE', cellule: stock.valeur },
      { libelle: 'Rotation', definition: 'Coût des ventes des 12 derniers mois / valeur du stock (fois par an).', unite: 'RATIO', sens: 'HAUSSE', cellule: celluleSeule(stock.rotation) },
      { libelle: 'Couverture', definition: 'Jours de ventes que couvre le stock.', unite: 'JOURS', sens: 'BAISSE', cellule: celluleSeule(stock.couvertureJours) },
      {
        libelle: 'Stock dormant',
        definition: `Produits en stock sans vente depuis ${stock.seuilDormant} jours (${stock.nbDormants} produits).`,
        unite: 'MONTANT',
        sens: 'BAISSE',
        cellule: celluleSeule(stock.valeurDormante),
      },
    ];
  });

  protected readonly libelles = computed(() => (this.stock.value()?.courbe ?? []).map(point => this.datePipe.transform(point.mois, 'MMM yyyy') ?? point.mois));
  protected readonly series = computed<SerieCourbe[]>(() => {
    const courbe = this.stock.value()?.courbe ?? [];
    return [
      { libelle: 'Stock', valeurs: courbe.map(point => point.valeur) },
      { libelle: 'Stock N-1', valeurs: courbe.map(point => point.valeurN1), reference: true },
    ];
  });
}
