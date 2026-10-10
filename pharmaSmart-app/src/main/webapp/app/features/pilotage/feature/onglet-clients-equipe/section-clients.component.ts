import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';

import { CardComponent, DataTableComponent } from 'app/shared/ui';
import { PilotageApiService } from '../../data-access/pilotage-api.service';
import { ReglageAnalyse, RequetePilotage } from '../../models/pilotage.model';
import { TuileCellule, TuilesCellulesComponent, celluleSeule } from '../../ui/tuiles-cellules/tuiles-cellules.component';
import { ValeurIndicateurPipe } from '../../ui/valeur-indicateur/valeur-indicateur.pipe';
import { AideComponent } from '../../ui/aide/aide.component';

/** Clients identifiés : actifs, nouveaux, revenus, perdus ; part du CA avec un client identifié ; clients en baisse. */
@Component({
  selector: 'app-section-clients',
  imports: [AideComponent, CardComponent, DataTableComponent, TuilesCellulesComponent, ValeurIndicateurPipe],
  templateUrl: './section-clients.component.html',
  styleUrl: '../../ui/section-onglet.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SectionClientsComponent {
  readonly requete = input.required<RequetePilotage>();

  private readonly api = inject(PilotageApiService);

  protected readonly clients = rxResource({ params: () => this.requete(), stream: ({ params }) => this.api.lireClients(params) });

  protected readonly tuiles = computed<TuileCellule[]>(() => {
    const clients = this.clients.value();
    if (!clients) {
      return [];
    }
    const tuiles: TuileCellule[] = [
      { libelle: 'Clients actifs', definition: 'Clients identifiés ayant acheté sur la période.', unite: 'NOMBRE', sens: 'HAUSSE', cellule: clients.actifs },
      { libelle: 'Nouveaux', definition: 'Première vente sur la période.', unite: 'NOMBRE', sens: 'HAUSSE', cellule: clients.nouveaux },
    ];
    if (clients.revenus !== null && clients.perdus !== null) {
      tuiles.push(
        { libelle: 'Revenus', definition: 'Absents de la référence, mais clients avant elle.', unite: 'NOMBRE', sens: 'HAUSSE', cellule: celluleSeule(clients.revenus) },
        { libelle: 'Perdus', definition: 'Actifs sur la référence, plus sur la période.', unite: 'NOMBRE', sens: 'BAISSE', cellule: celluleSeule(clients.perdus) },
      );
    }
    tuiles.push(
      { libelle: 'CA avec un client identifié', definition: 'Part du CA réalisée avec un client identifié.', unite: 'POURCENTAGE', sens: 'HAUSSE', cellule: clients.partCaIdentifie },
      { libelle: 'CA moyen par client', definition: 'CA avec un client identifié / clients actifs.', unite: 'MONTANT', sens: 'HAUSSE', cellule: clients.caMoyenParClient },
    );
    return tuiles;
  });
}
