import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';

import { ChartComponent } from 'app/shared/chart/chart.component';
import { ChartThemeColorsService } from 'app/shared/chart/chart-theme-colors.service';
import { UniteIndicateur } from '../../models/pilotage.model';
import { ValeurIndicateurPipe } from '../valeur-indicateur/valeur-indicateur.pipe';

export interface SerieBarres {
  libelle: string;
  valeurs: (number | null)[];
}

/**
 * Barres sur des libellés communs : une série (vieillissement, échéancier) ou plusieurs, empilées (encaissements par mode).
 * Couleurs de séries du thème, contrastées.
 */
@Component({
  selector: 'app-barres',
  imports: [ChartComponent],
  template: `<app-chart [ariaLabel]="description()" [data]="configuration().data" [options]="configuration().options" height="260" type="bar" />`,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BarresComponent {
  readonly libelles = input.required<readonly string[]>();
  readonly series = input.required<readonly SerieBarres[]>();
  readonly empilees = input(false);
  readonly unite = input<UniteIndicateur>('MONTANT');
  readonly description = input.required<string>();

  private readonly couleurs = inject(ChartThemeColorsService).colors;
  private readonly valeurIndicateur = new ValeurIndicateurPipe();

  protected readonly configuration = computed(() => {
    const couleurs = this.couleurs();
    const unite = this.unite();
    const empilees = this.empilees();
    return {
      data: {
        labels: [...this.libelles()],
        datasets: this.series().map((serie, rang) => ({
          label: serie.libelle,
          data: serie.valeurs,
          backgroundColor: couleurs.series[rang % couleurs.series.length],
        })),
      },
      options: {
        maintainAspectRatio: false,
        plugins: { legend: { display: this.series().length > 1, labels: { color: couleurs.text } } },
        scales: {
          x: { stacked: empilees, ticks: { color: couleurs.textMuted }, grid: { display: false } },
          y: {
            stacked: empilees,
            ticks: { color: couleurs.textMuted, callback: (valeur: number) => this.valeurIndicateur.transform(valeur, unite) },
            grid: { color: couleurs.border },
          },
        },
      },
    };
  });
}
