import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';

import { ChartComponent } from 'app/shared/chart/chart.component';
import { ChartThemeColorsService } from 'app/shared/chart/chart-theme-colors.service';
import { UniteIndicateur } from '../../models/pilotage.model';
import { ValeurIndicateurPipe } from '../valeur-indicateur/valeur-indicateur.pipe';

export interface SerieCourbe {
  libelle: string;
  valeurs: (number | null)[];
  /** Série de comparaison (référence, N-1) : en pointillés, couleur discrète. */
  reference?: boolean;
}

const FORMES = ['circle', 'rect', 'triangle', 'rectRot'];

/** Quelques séries sur des libellés communs (achats et coût des ventes, stock et stock N-1…) ; couleurs du thème. */
@Component({
  selector: 'app-courbe-series',
  imports: [ChartComponent],
  template: `<app-chart [ariaLabel]="description()" [data]="configuration().data" [options]="configuration().options" height="260" type="line" />`,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CourbeSeriesComponent {
  readonly libelles = input.required<readonly string[]>();
  readonly series = input.required<readonly SerieCourbe[]>();
  readonly unite = input<UniteIndicateur>('MONTANT');
  readonly description = input.required<string>();

  private readonly couleurs = inject(ChartThemeColorsService).colors;
  private readonly valeurIndicateur = new ValeurIndicateurPipe();

  protected readonly configuration = computed(() => {
    const couleurs = this.couleurs();
    const unite = this.unite();
    let rangCouleur = 0;
    return {
      data: {
        labels: [...this.libelles()],
        datasets: this.series().map((serie, rang) => {
          const couleur = serie.reference ? couleurs.textMuted : couleurs.series[rangCouleur++ % couleurs.series.length];
          return {
            label: serie.libelle,
            data: serie.valeurs,
            borderColor: couleur,
            backgroundColor: couleur,
            borderDash: serie.reference ? [6, 4] : [],
            pointStyle: FORMES[rang % FORMES.length],
            tension: 0.3,
          };
        }),
      },
      options: {
        maintainAspectRatio: false,
        plugins: { legend: { labels: { color: couleurs.text, usePointStyle: true } } },
        scales: {
          x: { ticks: { color: couleurs.textMuted }, grid: { color: couleurs.border } },
          y: { ticks: { color: couleurs.textMuted, callback: (valeur: number) => this.valeurIndicateur.transform(valeur, unite) }, grid: { color: couleurs.border } },
        },
      },
    };
  });
}
