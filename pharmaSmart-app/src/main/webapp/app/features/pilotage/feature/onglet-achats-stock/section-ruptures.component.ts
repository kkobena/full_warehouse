import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';

import { CardComponent, DataTableComponent } from 'app/shared/ui';
import { PilotageApiService } from '../../data-access/pilotage-api.service';
import { RequetePilotage } from '../../models/pilotage.model';
import { TuileCellule, TuilesCellulesComponent, celluleSeule } from '../../ui/tuiles-cellules/tuiles-cellules.component';
import { ValeurIndicateurPipe } from '../../ui/valeur-indicateur/valeur-indicateur.pipe';

const MOIS = ['janvier', 'février', 'mars', 'avril', 'mai', 'juin', 'juillet', 'août', 'septembre', 'octobre', 'novembre', 'décembre'];

/**
 * Ruptures & péremptions. Deux ruptures distinctes, jamais additionnées : fournisseurs (commandé, non livré) et comptoir
 * (demandé par le client, non servi : les ventes manquées).
 */
@Component({
  selector: 'app-section-ruptures',
  imports: [CardComponent, DataTableComponent, TuilesCellulesComponent, ValeurIndicateurPipe],
  templateUrl: './section-ruptures.component.html',
  styleUrl: '../../ui/section-onglet.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SectionRupturesComponent {
  readonly requete = input.required<RequetePilotage>();

  private readonly api = inject(PilotageApiService);

  protected readonly ruptures = rxResource({ params: () => this.requete(), stream: ({ params }) => this.api.lireRuptures(params) });

  protected readonly tuiles = computed<TuileCellule[]>(() => {
    const ruptures = this.ruptures.value();
    if (!ruptures) {
      return [];
    }
    return [
      { libelle: 'Ruptures fournisseurs', definition: 'Lignes commandées non livrées / lignes commandées.', unite: 'POURCENTAGE', sens: 'BAISSE', cellule: ruptures.tauxRupture },
      { libelle: 'Ventes manquées', definition: 'Valeur des quantités demandées au comptoir et non servies (avoirs clients).', unite: 'MONTANT', sens: 'BAISSE', cellule: ruptures.ventesManquees },
      { libelle: 'Taux de ventes manquées', definition: 'Quantités en avoir / quantités demandées.', unite: 'POURCENTAGE', sens: 'BAISSE', cellule: ruptures.tauxVentesManquees },
      { libelle: 'Périmé sur la période', definition: 'Lots arrivés à péremption et encore en stock, au prix d’achat.', unite: 'MONTANT', sens: 'BAISSE', cellule: ruptures.valeurPerimee },
      { libelle: 'À périmer sous 3 mois', definition: 'Lots en stock qui périment dans les 3 mois.', unite: 'MONTANT', sens: 'BAISSE', cellule: celluleSeule(ruptures.valeurAPerimerTroisMois) },
    ];
  });

  protected libellerMois(peremption: { annee: number; mois: number }): string {
    return `${MOIS[peremption.mois - 1]} ${peremption.annee}`;
  }
}
