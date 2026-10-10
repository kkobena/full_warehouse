import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';

import { ChartComponent } from 'app/shared/chart/chart.component';
import { ChartThemeColors, ChartThemeColorsService } from 'app/shared/chart/chart-theme-colors.service';
import { ComparaisonAnnees, UniteIndicateur } from '../../models/pilotage.model';
import { ValeurIndicateurPipe } from '../valeur-indicateur/valeur-indicateur.pipe';

export const MOIS_COURTS = ['Janv.', 'Févr.', 'Mars', 'Avr.', 'Mai', 'Juin', 'Juil.', 'Août', 'Sept.', 'Oct.', 'Nov.', 'Déc.'];
const FORMES = ['circle', 'rect', 'triangle', 'rectRot', 'star', 'crossRot'];

export type GraphiqueAnnees = 'COURBES' | 'TRIMESTRES' | 'SAISONNALITE';

/**
 * Graphiques de « Comparer les années » : une courbe par année (l'année en cours s'arrête au mois en cours, en trait plein ;
 * les autres en pointillés), barres groupées par trimestre, saisonnalité moyenne. Couleurs du thème, l'année en cours à la
 * première couleur de série (l'accent).
 */
@Component({
  selector: 'app-graphique-annees',
  imports: [ChartComponent],
  template: `<app-chart [ariaLabel]="description()" [data]="configuration().data" [options]="configuration().options" [type]="configuration().type" height="300" />`,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GraphiqueAnneesComponent {
  readonly comparaison = input.required<ComparaisonAnnees>();
  readonly graphique = input.required<GraphiqueAnnees>();

  private readonly couleurs = inject(ChartThemeColorsService).colors;
  private readonly valeurIndicateur = new ValeurIndicateurPipe();

  protected readonly description = computed(() => {
    const libelle = this.comparaison().indicateur.libelle;
    return {
      COURBES: `${libelle} par mois, une courbe par année`,
      TRIMESTRES: `${libelle} par trimestre et par année`,
      SAISONNALITE: 'Poids moyen de chaque mois dans l’année',
    }[this.graphique()];
  });

  protected readonly configuration = computed(() => {
    const comparaison = this.comparaison();
    const couleurs = this.couleurs();
    switch (this.graphique()) {
      case 'COURBES':
        return {
          type: 'line' as const,
          data: { labels: MOIS_COURTS, datasets: this.lireSeries(comparaison, couleurs, annee => annee.mois.map(cellule => cellule.valeur)) },
          options: this.options(couleurs, comparaison.indicateur.unite),
        };
      case 'TRIMESTRES':
        return {
          type: 'bar' as const,
          data: { labels: ['T1', 'T2', 'T3', 'T4'], datasets: this.lireSeries(comparaison, couleurs, annee => annee.trimestres.map(cellule => cellule.valeur)) },
          options: this.options(couleurs, comparaison.indicateur.unite),
        };
      case 'SAISONNALITE':
        return {
          type: 'bar' as const,
          data: {
            labels: MOIS_COURTS,
            datasets: [{ label: 'Poids du mois', data: comparaison.saisonnalite, backgroundColor: couleurs.accent }],
          },
          options: this.options(couleurs, 'POURCENTAGE'),
        };
    }
  });

  /** L'année la plus récente d'abord dans la légende, à la couleur d'accent. */
  private lireSeries(comparaison: ComparaisonAnnees, couleurs: ChartThemeColors, valeurs: (annee: ComparaisonAnnees['annees'][number]) => (number | null)[]) {
    return [...comparaison.annees].reverse().map((annee, rang) => ({
      label: annee.complete ? String(annee.annee) : `${annee.annee} (en cours)`,
      data: valeurs(annee),
      borderColor: couleurs.series[rang % couleurs.series.length],
      backgroundColor: couleurs.series[rang % couleurs.series.length],
      borderDash: rang === 0 ? [] : [6, 3],
      pointStyle: FORMES[rang % FORMES.length],
      pointRadius: 4,
      tension: 0.3,
    }));
  }

  private options(couleurs: ChartThemeColors, unite: UniteIndicateur) {
    return {
      maintainAspectRatio: false,
      plugins: { legend: { labels: { color: couleurs.text, usePointStyle: true } } },
      scales: {
        x: { ticks: { color: couleurs.textMuted }, grid: { display: false } },
        y: { ticks: { color: couleurs.textMuted, callback: (valeur: number) => this.valeurIndicateur.transform(valeur, unite) }, grid: { color: couleurs.border } },
      },
    };
  }
}
