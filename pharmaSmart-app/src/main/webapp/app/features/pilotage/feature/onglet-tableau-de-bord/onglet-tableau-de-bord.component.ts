import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { forkJoin } from 'rxjs';

import { AbilityService } from 'app/core/auth/ability.service';
import { BlobDownloadService } from 'app/shared/services/blob-download.service';
import { ButtonComponent, CardComponent, DataTableComponent, KpiItemComponent, KpiStripComponent } from 'app/shared/ui';
import { formatDateFR, formatMontantAbrege } from 'app/shared/utils/format-utils';
import { PilotageApiService } from '../../data-access/pilotage-api.service';
import { PointSerie, ProjectionObjectif, RequetePilotage, SerieIndicateur } from '../../models/pilotage.model';
import { ValeurIndicateurPipe } from '../../ui/valeur-indicateur/valeur-indicateur.pipe';
import { AideComponent } from '../../ui/aide/aide.component';
import { BandeauAlertesComponent } from '../../ui/bandeau-alertes/bandeau-alertes.component';
import { CourbeIndicateurComponent } from '../../ui/courbe-indicateur/courbe-indicateur.component';
import { VariationComponent } from '../../ui/variation/variation.component';
import { elargirAuxTreizeTranches, libellerHorizon } from '../../data-access/horizon-courbe';

/** Tuile : un indicateur principal et, en sous-ligne, un indicateur qui l'éclaire. */
interface Tuile {
  principal: string;
  secondaire?: string;
}

const TUILES: readonly Tuile[] = [
  { principal: 'CA_TTC', secondaire: 'CA_NET' },
  { principal: 'NB_VENTES', secondaire: 'FREQUENTATION' },
  { principal: 'PANIER_MOYEN', secondaire: 'ARTICLES_PAR_VENTE' },
  { principal: 'MARGE_BRUTE', secondaire: 'TAUX_MARGE' },
  { principal: 'REMISES', secondaire: 'TAUX_REMISE' },
  { principal: 'ACHATS_TTC', secondaire: 'RATIO_VENTES_ACHATS' },
  { principal: 'PART_TIERS_PAYANT' },
  { principal: 'ENCAISSEMENTS' },
];

const COLONNES_DU_TABLEAU = ['CA_TTC', 'MARGE_BRUTE', 'TAUX_MARGE', 'REMISES', 'ACHATS_TTC', 'NB_VENTES', 'PANIER_MOYEN', 'PART_TIERS_PAYANT'];

const INDICATEURS = [...new Set([...TUILES.flatMap(t => [t.principal, t.secondaire ?? t.principal]), ...COLONNES_DU_TABLEAU])];

/** Onglet d'ouverture du pilotage : comment va l'officine, et ce qui a bougé. */
@Component({
  selector: 'app-onglet-tableau-de-bord',
  imports: [AideComponent, BandeauAlertesComponent, ButtonComponent, KpiStripComponent, KpiItemComponent, CardComponent, DataTableComponent, CourbeIndicateurComponent, VariationComponent, ValeurIndicateurPipe],
  templateUrl: './onglet-tableau-de-bord.component.html',
  styleUrl: './onglet-tableau-de-bord.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OngletTableauDeBordComponent {
  readonly requete = input.required<RequetePilotage>();

  private readonly api = inject(PilotageApiService);
  private readonly telechargement = inject(BlobDownloadService);

  protected readonly peutExporter = inject(AbilityService).canSignal('export', 'pilotage.tableau-de-bord');

  protected readonly donnees = rxResource({
    params: () => this.requete(),
    stream: ({ params }) => forkJoin({ series: this.api.lireSeries(params, INDICATEURS), ecarts: this.api.lireEcarts(params, 5) }),
  });

  /** Les alertes valent pour aujourd'hui, quelle que soit la période de la barre. */
  protected readonly alertes = rxResource({ stream: () => this.api.listerAlertes() });

  private readonly seriesParCode = computed(() => new Map((this.donnees.value()?.series.series ?? []).map(serie => [serie.indicateur.code, serie])));

  protected readonly tuiles = computed(() =>
    TUILES.filter(tuile => this.seriesParCode().has(tuile.principal)).map(tuile => ({
      principal: this.seriesParCode().get(tuile.principal)!,
      secondaire: tuile.secondaire ? this.seriesParCode().get(tuile.secondaire) : undefined,
    })),
  );

  protected readonly colonnes = computed(() => COLONNES_DU_TABLEAU.map(code => this.seriesParCode().get(code)).filter(serie => !!serie));

  /** Lignes du tableau, de la tranche la plus récente à la plus ancienne. */
  protected readonly lignes = computed(() => {
    const caTtc = this.seriesParCode().get('CA_TTC');
    return (caTtc?.points ?? []).map((point, rang) => ({ point, rang })).reverse();
  });

  protected readonly contexte = computed(() => {
    const comparaison = this.donnees.value()?.series.comparaison;
    if (!comparaison) {
      return '';
    }
    const periode = `${this.formaterPeriode(comparaison.periode)}${comparaison.aDate ? ' (à date)' : ''}`;
    if (this.contreObjectif()) {
      return `${periode} comparé aux objectifs (un mois entamé compte au prorata de ses jours). Les autres onglets restent sans référence.`;
    }
    return comparaison.reference ? `${periode} comparé à ${this.formaterPeriode(comparaison.reference)}` : periode;
  });

  protected readonly contreObjectif = computed(() => this.requete().comparaison === 'OBJECTIF');

  protected readonly ecarts = computed(() => this.donnees.value()?.ecarts);

  protected readonly effets = computed(() => {
    const ecarts = this.ecarts();
    if (ecarts?.effetFrequentation == null) {
      return [];
    }
    return [
      { libelle: 'Fréquentation (nombre de ventes)', valeur: ecarts.effetFrequentation },
      { libelle: 'Articles par vente', valeur: ecarts.effetArticles ?? 0 },
      { libelle: "Prix moyen d'un article", valeur: ecarts.effetPrix ?? 0 },
    ];
  });

  /** La courbe remonte 13 tranches : la tendance, pas la seule période des tuiles. */
  private readonly historiqueCa = rxResource({
    params: () => elargirAuxTreizeTranches(this.requete()),
    stream: ({ params }) => this.api.lireSeries(params, ['CA_TTC']),
  });

  protected readonly serieCa = computed(() => this.historiqueCa.value()?.series[0]);
  protected readonly horizon = computed(() => libellerHorizon(this.requete().granularite));


  /** Le tableau tel qu'affiché, généré par le serveur avec des valeurs complètes. */
  protected exporter(): void {
    const requete = this.requete();
    this.telechargement.downloadFromObservable(this.api.exporterSeries(requete, COLONNES_DU_TABLEAU), `pilotage-${requete.du}-${requete.au}`, 'csv');
  }

  /** La fin de mois projetée : sur le CA, et sur tout indicateur qui a un objectif ; ailleurs, elle chargerait la tuile. */
  protected afficherProjection(serie: SerieIndicateur): ProjectionObjectif | null {
    return serie.projection && (serie.indicateur.code === 'CA_TTC' || serie.objectif != null) ? serie.projection : null;
  }

  /** Hausse du CA favorable, baisse défavorable : mêmes couleurs que les variations. */
  protected classerMontant(valeur: number): string {
    return valeur > 0 ? 'montant-ecart montant-ecart--hausse' : valeur < 0 ? 'montant-ecart montant-ecart--baisse' : 'montant-ecart';
  }

  protected formaterMontant(valeur: number): string {
    return `${valeur > 0 ? '+' : ''}${formatMontantAbrege(valeur)}`;
  }

  protected point(serie: SerieIndicateur, rang: number): PointSerie | undefined {
    return serie.points[rang];
  }

  private formaterPeriode(periode: { du: string; au: string }): string {
    return `du ${formatDateFR(periode.du)} au ${formatDateFR(periode.au)}`;
  }
}
