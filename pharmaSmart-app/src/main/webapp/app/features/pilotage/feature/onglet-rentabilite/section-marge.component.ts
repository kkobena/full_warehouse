import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';

import { CardComponent, DataTableComponent, KpiItemComponent, KpiStripComponent, SelectComponent } from 'app/shared/ui';
import { formatDecimal } from 'app/shared/utils/format-utils';
import { PilotageApiService } from '../../data-access/pilotage-api.service';
import { AxeAnalyse, RequetePilotage } from '../../models/pilotage.model';
import { CourbeIndicateurComponent } from '../../ui/courbe-indicateur/courbe-indicateur.component';
import { ValeurIndicateurPipe } from '../../ui/valeur-indicateur/valeur-indicateur.pipe';
import { VariationComponent } from '../../ui/variation/variation.component';
import { AideComponent } from '../../ui/aide/aide.component';
import { elargirAuxTreizeTranches, libellerHorizon } from '../../data-access/horizon-courbe';

const INDICATEURS_MARGE = ['MARGE_BRUTE', 'TAUX_MARGE', 'COEFFICIENT_MOYEN', 'MARGE_PAR_VENTE'];

/** Marge : tuiles, taux dans le temps, effet mix, marge par famille (ou laboratoire, produit…), faibles marges, ventes à perte. */
@Component({
  selector: 'app-section-marge',
  imports: [AideComponent,
    FormsModule,
    DatePipe,
    KpiStripComponent,
    KpiItemComponent,
    CardComponent,
    DataTableComponent,
    SelectComponent,
    CourbeIndicateurComponent,
    VariationComponent,
    ValeurIndicateurPipe,
  ],
  templateUrl: './section-marge.component.html',
  styleUrl: '../../ui/section-onglet.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SectionMargeComponent {
  readonly requete = input.required<RequetePilotage>();

  private readonly api = inject(PilotageApiService);

  protected readonly axe = signal<AxeAnalyse>('FAMILLE');
  protected readonly axes: { libelle: string; valeur: AxeAnalyse }[] = [
    { libelle: 'Famille', valeur: 'FAMILLE' },
    { libelle: 'Laboratoire', valeur: 'LABORATOIRE' },
    { libelle: 'Grossiste', valeur: 'FOURNISSEUR' },
    { libelle: 'Gamme', valeur: 'GAMME' },
    { libelle: 'Forme', valeur: 'FORME' },
    { libelle: 'Produit', valeur: 'PRODUIT' },
  ];

  protected readonly series = rxResource({
    params: () => this.requete(),
    stream: ({ params }) => this.api.lireSeries(params, INDICATEURS_MARGE),
  });

  protected readonly marge = rxResource({
    params: () => ({ requete: this.requete(), axe: this.axe() }),
    stream: ({ params }) => this.api.lireMarge(params.requete, params.axe),
  });

  private readonly historique = rxResource({
    params: () => elargirAuxTreizeTranches(this.requete()),
    stream: ({ params }) => this.api.lireSeries(params, ['TAUX_MARGE']),
  });

  protected readonly tauxDansLeTemps = computed(() => this.historique.value()?.series[0]);
  protected readonly horizon = computed(() => libellerHorizon(this.requete().granularite));


  /** « Le taux de marge recule de 0,6 pt : 0,4 pt dû au mix, 0,2 pt aux taux. » */
  protected readonly lectureMix = computed(() => {
    const marge = this.marge.value();
    if (!marge || marge.effetMix === null || marge.effetTaux === null) {
      return '';
    }
    const variation = marge.effetMix + marge.effetTaux;
    const sens = variation >= 0 ? 'progresse' : 'recule';
    return (
      `Le taux de marge ${sens} de ${formatDecimal(Math.abs(variation), 1)} pt : ${this.points(marge.effetMix)} dû au mix (le poids de chaque ` +
      `${marge.axe.libelle.toLowerCase()} dans le CA), ${this.points(marge.effetTaux)} au taux de chacun.`
    );
  });

  protected choisirAxe(selection: unknown): void {
    if (selection) {
      this.axe.set(selection as AxeAnalyse);
    }
  }

  private points(valeur: number): string {
    return `${valeur > 0 ? '+' : valeur < 0 ? '−' : ''}${formatDecimal(Math.abs(valeur), 1)} pt`;
  }
}
