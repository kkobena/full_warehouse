import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';

import { CardComponent, DataTableComponent } from 'app/shared/ui';
import { PilotageApiService } from '../../data-access/pilotage-api.service';
import { RequetePilotage } from '../../models/pilotage.model';
import { BarresComponent, SerieBarres } from '../../ui/barres/barres.component';
import { TuileCellule, TuilesCellulesComponent } from '../../ui/tuiles-cellules/tuiles-cellules.component';
import { ValeurIndicateurPipe } from '../../ui/valeur-indicateur/valeur-indicateur.pipe';
import { VariationComponent } from '../../ui/variation/variation.component';
import { AideComponent } from '../../ui/aide/aide.component';
import { elargirAuxTreizeTranches, libellerHorizon } from '../../data-access/horizon-courbe';

/** Encaissements & caisse : par mode de paiement, dans le temps (empilés), par caissier. */
@Component({
  selector: 'app-section-encaissements',
  imports: [AideComponent, CardComponent, DataTableComponent, TuilesCellulesComponent, BarresComponent, VariationComponent, ValeurIndicateurPipe],
  templateUrl: './section-encaissements.component.html',
  styleUrl: '../../ui/section-onglet.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SectionEncaissementsComponent {
  readonly requete = input.required<RequetePilotage>();

  private readonly api = inject(PilotageApiService);

  protected readonly encaissements = rxResource({ params: () => this.requete(), stream: ({ params }) => this.api.lireEncaissements(params) });

  /** Le total, puis les trois premiers modes. */
  protected readonly tuiles = computed<TuileCellule[]>(() => {
    const encaissements = this.encaissements.value();
    if (!encaissements) {
      return [];
    }
    return [
      { libelle: 'Encaissé', definition: 'Règlements de ventes, de différés et de factures tiers payant.', unite: 'MONTANT', sens: 'HAUSSE', cellule: encaissements.total },
      ...encaissements.modes.slice(0, 3).map(mode => ({
        libelle: `dont ${mode.libelle}`,
        definition: `Encaissé par ${mode.libelle}.`,
        unite: 'MONTANT' as const,
        sens: 'NEUTRE' as const,
        cellule: mode.montant,
      })),
    ];
  });

  private readonly historique = rxResource({
    params: () => elargirAuxTreizeTranches(this.requete()),
    stream: ({ params }) => this.api.lireEncaissements(params),
  });

  protected readonly libelles = computed(() => this.historique.value()?.tranches ?? []);
  protected readonly series = computed<SerieBarres[]>(() => this.historique.value()?.series ?? []);
  protected readonly horizon = computed(() => libellerHorizon(this.requete().granularite));

}
