import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';

import { ChartComponent } from 'app/shared/chart/chart.component';
import { ChartThemeColorsService } from 'app/shared/chart/chart-theme-colors.service';
import { libellerReference } from '../../data-access/libelle-reference';
import { RequetePilotage, SerieIndicateur } from '../../models/pilotage.model';
import { ValeurIndicateurPipe } from '../valeur-indicateur/valeur-indicateur.pipe';

/** Un indicateur par tranche de la période, sa référence en pointillés ; couleurs du thème, relues à chaque changement. */
@Component({
  selector: 'app-courbe-indicateur',
  imports: [ChartComponent],
  template: `<app-chart [ariaLabel]="description()" [data]="configuration().data" [options]="configuration().options" [height]="hauteur()" type="line" />`,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CourbeIndicateurComponent {
  readonly serie = input.required<SerieIndicateur>();
  readonly hauteur = input('260');
  /** La référence est l'objectif (comparaison « Objectifs ») : elle en prend le nom, sans seconde ligne d'objectif. */
  readonly contreObjectif = input(false);
  /** La requête de la barre : la légende de la référence en est tirée (« Même période, année précédente (N-1) »…). */
  readonly requete = input<RequetePilotage | null>(null);

  private readonly libelleReference = computed(() => {
    const requete = this.requete();
    return requete ? libellerReference(requete) : 'Référence';
  });

  private readonly couleurs = inject(ChartThemeColorsService).colors;
  private readonly valeurIndicateur = new ValeurIndicateurPipe();

  protected readonly description = computed(() => `${this.serie().indicateur.libelle} par période, comparé à ${this.contreObjectif() ? "l'objectif" : this.libelleReference().toLowerCase()}`);

  protected readonly configuration = computed(() => {
    const serie = this.serie();
    const couleurs = this.couleurs();
    const unite = serie.indicateur.unite;
    return {
      data: {
        labels: serie.points.map(point => point.libelle),
        datasets: [
          { label: 'Réalisé', data: serie.points.map(point => point.valeur), borderColor: couleurs.accent, backgroundColor: couleurs.accent, tension: 0.3 },
          {
            label: this.contreObjectif() ? 'Objectif' : this.libelleReference(),
            data: serie.points.map(point => point.valeurReference),
            borderColor: couleurs.textMuted,
            backgroundColor: couleurs.textMuted,
            borderDash: [6, 4],
            tension: 0.3,
          },
          ...(this.contreObjectif() || !serie.points.some(point => point.objectif != null)
            ? []
            : [
                {
                  label: 'Objectif',
                  data: serie.points.map(point => point.objectif),
                  borderColor: couleurs.series[1],
                  backgroundColor: couleurs.series[1],
                  borderDash: [2, 3],
                  stepped: 'middle' as const,
                  pointRadius: 0,
                },
              ]),
        ],
      },
      options: {
        maintainAspectRatio: false,
        // Pastilles plutôt que rectangles : un rectangle en pointillés se lisait mal.
        plugins: { legend: { labels: { color: couleurs.text, usePointStyle: true, pointStyleWidth: 14 } } },
        scales: {
          x: { ticks: { color: couleurs.textMuted }, grid: { color: couleurs.border } },
          y: { ticks: { color: couleurs.textMuted, callback: (valeur: number) => this.valeurIndicateur.transform(valeur, unite) }, grid: { color: couleurs.border } },
        },
      },
    };
  });
}
