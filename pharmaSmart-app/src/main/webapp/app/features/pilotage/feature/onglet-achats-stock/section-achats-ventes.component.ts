import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import dayjs from 'dayjs/esm';

import { CardComponent, DataTableComponent } from 'app/shared/ui';
import { PilotageApiService } from '../../data-access/pilotage-api.service';
import { RequetePilotage } from '../../models/pilotage.model';
import { TuileCellule, TuilesCellulesComponent, celluleSeule } from '../../ui/tuiles-cellules/tuiles-cellules.component';
import { ValeurIndicateurPipe } from '../../ui/valeur-indicateur/valeur-indicateur.pipe';
import { CourbeSeriesComponent, SerieCourbe } from '../../ui/courbe-series/courbe-series.component';
import { AideComponent } from '../../ui/aide/aide.component';
import { elargirAuxTreizeTranches, libellerHorizon } from '../../data-access/horizon-courbe';

/** Ce qui entre face à ce qui sort : ratio ventes / achats (période et 12 mois glissants), achats et coût des ventes. */
@Component({
  selector: 'app-section-achats-ventes',
  imports: [AideComponent, CardComponent, DataTableComponent, TuilesCellulesComponent, CourbeSeriesComponent, ValeurIndicateurPipe],
  templateUrl: './section-achats-ventes.component.html',
  styleUrl: '../../ui/section-onglet.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SectionAchatsVentesComponent {
  readonly requete = input.required<RequetePilotage>();

  private readonly api = inject(PilotageApiService);

  protected readonly achatsVentes = rxResource({ params: () => this.requete(), stream: ({ params }) => this.api.lireAchatsVentes(params) });
  protected readonly ratio = rxResource({ params: () => this.requete(), stream: ({ params }) => this.api.lireSeries(params, ['RATIO_VENTES_ACHATS']) });
  /** Les 12 mois qui se terminent à la fin de la période, sans comparaison. */
  protected readonly ratioDouzeMois = rxResource({
    params: () => ({ ...this.requete(), du: douzeMoisAvant(this.requete().au), comparaison: 'AUCUNE' as const, aDate: false }),
    stream: ({ params }) => this.api.lireSeries(params, ['RATIO_VENTES_ACHATS']),
  });

  protected readonly tuiles = computed<TuileCellule[]>(() => {
    const periode = this.ratio.value()?.series[0];
    const glissant = this.ratioDouzeMois.value()?.series[0];
    const tuiles: TuileCellule[] = [];
    if (periode) {
      tuiles.push({
        libelle: 'Ventes / achats',
        definition: periode.indicateur.definition,
        unite: 'RATIO',
        sens: 'NEUTRE',
        cellule: { valeur: periode.valeur, valeurReference: periode.valeurReference, ecart: periode.ecart, ecartPct: periode.ecartPct },
      });
    }
    if (glissant) {
      tuiles.push({ libelle: 'Ventes / achats sur 12 mois', definition: 'Le même ratio sur les 12 mois glissants.', unite: 'RATIO', sens: 'NEUTRE', cellule: celluleSeule(glissant.valeur) });
    }
    return tuiles;
  });

  private readonly historique = rxResource({
    params: () => elargirAuxTreizeTranches(this.requete()),
    stream: ({ params }) => this.api.lireAchatsVentes(params),
  });

  protected readonly libelles = computed(() => (this.historique.value()?.points ?? []).map(point => point.libelle));
  protected readonly horizon = computed(() => libellerHorizon(this.requete().granularite));

  protected readonly series = computed<SerieCourbe[]>(() => {
    const points = this.historique.value()?.points ?? [];
    return [
      { libelle: 'Achats HT', valeurs: points.map(point => point.achatsHt) },
      { libelle: 'Coût des ventes HT', valeurs: points.map(point => point.coutVentesHt) },
    ];
  });
}

function douzeMoisAvant(au: string): string {
  return dayjs(au).subtract(1, 'year').add(1, 'day').format('YYYY-MM-DD');
}
