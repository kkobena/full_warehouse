import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';

import { BadgeComponent, ButtonComponent, CardComponent, DataTableComponent } from 'app/shared/ui';
import { PilotageApiService } from '../../data-access/pilotage-api.service';
import { RequetePilotage } from '../../models/pilotage.model';
import { CourbeIndicateurComponent } from '../../ui/courbe-indicateur/courbe-indicateur.component';
import { TableauRemisesComponent } from '../../ui/tableau-remises/tableau-remises.component';
import { ValeurIndicateurPipe } from '../../ui/valeur-indicateur/valeur-indicateur.pipe';
import { TuileCellule, TuilesCellulesComponent } from '../../ui/tuiles-cellules/tuiles-cellules.component';
import { versParametresUrl, REGLAGE_PAR_DEFAUT } from '../onglet-analyser/reglage-analyse';
import { AideComponent } from '../../ui/aide/aide.component';
import { elargirAuxTreizeTranches, libellerHorizon } from '../../data-access/horizon-courbe';

/** Remises : tuiles, remises dans le temps, mode d'octroi, tranches, vendeurs, plus fortes remises. */
@Component({
  selector: 'app-section-remises',
  imports: [AideComponent, 
    DatePipe,
    BadgeComponent,
    CardComponent,
    DataTableComponent,
    ButtonComponent,
    CourbeIndicateurComponent,
    TableauRemisesComponent,
    TuilesCellulesComponent,
    ValeurIndicateurPipe,
  ],
  templateUrl: './section-remises.component.html',
  styleUrl: '../../ui/section-onglet.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SectionRemisesComponent {
  readonly requete = input.required<RequetePilotage>();

  private readonly api = inject(PilotageApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  /** Taux moyen des seules ventes remisées (remises / CA des modes d'octroi) : le repère des plus fortes remises. */
  protected readonly tauxRemisees = computed(() => {
    let remises = 0;
    let ca = 0;
    for (const octroi of this.remises.value()?.octrois ?? []) {
      remises += octroi.remises.valeur ?? 0;
      ca += octroi.caTtc.valeur ?? 0;
    }
    return ca > 0 ? (remises * 100) / ca : null;
  });

  /** Taux d'une vente au-delà du multiple d'alerte du taux moyen des ventes remisées. */
  protected estExcessif(taux: number | null, tauxMoyen: number | null, multiple: number): boolean {
    return taux != null && tauxMoyen != null && tauxMoyen > 0 && taux > multiple * tauxMoyen;
  }

  protected readonly remises = rxResource({
    params: () => this.requete(),
    stream: ({ params }) => this.api.lireRemises(params),
  });

  protected readonly serieRemises = rxResource({
    params: () => elargirAuxTreizeTranches(this.requete()),
    stream: ({ params }) => this.api.lireSeries(params, ['REMISES']),
  });

  protected readonly tuiles = computed<TuileCellule[]>(() => {
    const remises = this.remises.value();
    if (!remises) {
      return [];
    }
    return [
      { libelle: 'Remises accordées', definition: 'Total des remises au comptoir.', unite: 'MONTANT', sens: 'BAISSE', cellule: remises.remises },
      { libelle: 'Taux de remise', definition: 'Remises divisées par le CA TTC.', unite: 'POURCENTAGE', sens: 'BAISSE', cellule: remises.tauxRemise },
      {
        libelle: 'Ventes remisées',
        definition: 'Part des ventes qui portent une remise.',
        unite: 'POURCENTAGE',
        sens: 'BAISSE',
        cellule: remises.partVentesRemisees,
      },
      {
        libelle: 'Remise moyenne',
        definition: 'Remise moyenne d’une vente remisée.',
        unite: 'MONTANT',
        sens: 'BAISSE',
        cellule: remises.remiseMoyenne,
      },
      {
        libelle: 'Poids dans la marge',
        definition: 'Remises divisées par la marge qu’on aurait faite sans elles.',
        unite: 'POURCENTAGE',
        sens: 'BAISSE',
        cellule: remises.poidsRemisesMarge,
      },
    ];
  });

  protected readonly serie = computed(() => this.serieRemises.value()?.series[0]);
  protected readonly horizon = computed(() => libellerHorizon(this.requete().granularite));


  /** Les autres ventilations (famille, produit, code remise…) sont celles de l'onglet Analyser, réglé sur les remises. */
  protected analyserLesRemises(): void {
    const reglage = { ...REGLAGE_PAR_DEFAUT, indicateurs: ['REMISES', 'TAUX_REMISE', 'CA_TTC'], axe: 'FAMILLE' as const, affichage: 'TABLEAU' as const };
    void this.router.navigate([], { relativeTo: this.route, queryParams: { ...versParametresUrl(reglage), onglet: 'analyser', vue: null }, queryParamsHandling: 'merge' });
  }
}
