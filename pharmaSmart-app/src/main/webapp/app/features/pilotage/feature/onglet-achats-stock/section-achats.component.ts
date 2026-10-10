import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';

import { CardComponent, DataTableComponent } from 'app/shared/ui';
import { PilotageApiService } from '../../data-access/pilotage-api.service';
import { RequetePilotage } from '../../models/pilotage.model';
import { TuileCellule, TuilesCellulesComponent, celluleSeule } from '../../ui/tuiles-cellules/tuiles-cellules.component';
import { ValeurIndicateurPipe } from '../../ui/valeur-indicateur/valeur-indicateur.pipe';
import { VariationComponent } from '../../ui/variation/variation.component';
import { CourbeIndicateurComponent } from '../../ui/courbe-indicateur/courbe-indicateur.component';
import { AideComponent } from '../../ui/aide/aide.component';
import { elargirAuxTreizeTranches, libellerHorizon } from '../../data-access/horizon-courbe';

/** Achats reçus : tuiles, achats dans le temps, par fournisseur (délai, conformité), par famille. */
@Component({
  selector: 'app-section-achats',
  imports: [AideComponent, CardComponent, DataTableComponent, TuilesCellulesComponent, CourbeIndicateurComponent, VariationComponent, ValeurIndicateurPipe],
  templateUrl: './section-achats.component.html',
  styleUrl: '../../ui/section-onglet.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SectionAchatsComponent {
  readonly requete = input.required<RequetePilotage>();

  private readonly api = inject(PilotageApiService);

  protected readonly achats = rxResource({ params: () => this.requete(), stream: ({ params }) => this.api.lireAchats(params) });
  protected readonly serie = rxResource({
    params: () => elargirAuxTreizeTranches(this.requete()),
    stream: ({ params }) => this.api.lireSeries(params, ['ACHATS_TTC']),
  });

  protected readonly tuiles = computed<TuileCellule[]>(() => {
    const achats = this.achats.value();
    if (!achats) {
      return [];
    }
    return [
      { libelle: 'Achats TTC', definition: 'Commandes reçues, datées à la réception.', unite: 'MONTANT', sens: 'NEUTRE', cellule: achats.achatsTtc },
      { libelle: 'Achats HT', definition: 'Achats TTC hors taxe.', unite: 'MONTANT', sens: 'NEUTRE', cellule: achats.achatsHt },
      { libelle: 'Bons reçus', definition: 'Nombre de commandes réceptionnées.', unite: 'NOMBRE', sens: 'NEUTRE', cellule: achats.nbBons },
      { libelle: 'Délai de livraison', definition: 'Jours moyens entre la commande et la réception.', unite: 'JOURS', sens: 'BAISSE', cellule: achats.delaiMoyen },
      { libelle: 'Conformité', definition: 'Quantités reçues / quantités commandées.', unite: 'POURCENTAGE', sens: 'HAUSSE', cellule: achats.conformite },
    ];
  });

  protected readonly serieAchats = computed(() => this.serie.value()?.series[0]);
  protected readonly horizon = computed(() => libellerHorizon(this.requete().granularite));

}
