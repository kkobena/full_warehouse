import { inject, Injectable } from '@angular/core';
import { ChartColorsUtilsService } from './chart-colors-utils.service';

export interface ChartConfig {
  data: any;
  options: any;
}

/**
 * Service factory pour créer des configurations Chart.js standardisées.
 * Délègue les couleurs à `ChartColorsUtilsService` (CSS variables PrimeNG).
 *
 * Utilisation :
 * ```typescript
 * private readonly chartBuilder = inject(ChartBuilderService);
 *
 * this.barChartConfig = this.chartBuilder.barConfig(
 *   items.map(i => i.label),
 *   items.map(i => i.value),
 *   'Montant HT'
 * );
 * ```
 * Puis dans le template :
 * ```html
 * <p-chart type="bar" [data]="barChartConfig.data" [options]="barChartConfig.options" />
 * ```
 */
@Injectable({ providedIn: 'root' })
export class ChartBuilderService {
  private readonly colors = inject(ChartColorsUtilsService);

  // ─────────────────────────────────────────────────────────────────────────
  // Diagrammes en barres
  // ─────────────────────────────────────────────────────────────────────────

  /** Barre simple : un seul dataset. `color` est optionnel (défaut : bleu). */
  barConfig(labels: string[], data: number[], label: string, color?: string): ChartConfig {
    return {
      data: {
        labels,
        datasets: [{
          type: 'bar',
          label,
          backgroundColor: color ?? this.colors.colors()[0],
          data,
        }],
      },
      options: this.scaleOptions(),
    };
  }

  /** Barres empilées ou multiples datasets. */
  multiBarConfig(labels: string[], datasets: Array<{ label: string; data: number[]; color?: string }>): ChartConfig {
    const palette = this.colors.colors();
    return {
      data: {
        labels,
        datasets: datasets.map((d, i) => ({
          type: 'bar',
          label: d.label,
          backgroundColor: d.color ?? palette[i % palette.length],
          data: d.data,
        })),
      },
      options: this.scaleOptions(),
    };
  }

  // ─────────────────────────────────────────────────────────────────────────
  // Diagrammes circulaires
  // ─────────────────────────────────────────────────────────────────────────

  pieConfig(labels: string[], data: number[]): ChartConfig {
    return {
      data: {
        labels,
        datasets: [{
          data,
          backgroundColor: this.colors.colors(),
          hoverBackgroundColor: this.colors.hoverColors(),
        }],
      },
      options: this.pieOptions(),
    };
  }

  doughnutConfig(labels: string[], data: number[]): ChartConfig {
    return this.pieConfig(labels, data);
  }

  // ─────────────────────────────────────────────────────────────────────────
  // Pareto (barres + ligne de % cumulé) — analyse ABC / 20-80
  // ─────────────────────────────────────────────────────────────────────────

  /**
   * @param labels      Libellés (noms de produits, familles…)
   * @param quantities  Valeurs brutes (quantités ou montants)
   * @param cumPct      Pourcentages cumulés pour la ligne
   * @param barLabel    Label du dataset barre (ex: "Quantité")
   * @param lineLabel   Label du dataset ligne (ex: "% cumulé")
   */
  paretoConfig(
    labels: string[],
    quantities: number[],
    cumPct: number[],
    barLabel = 'Quantité',
    lineLabel = '% cumulé',
  ): ChartConfig {
    const palette = this.colors.colors();
    return {
      data: {
        labels,
        datasets: [
          {
            type: 'bar',
            label: barLabel,
            backgroundColor: palette[4] ?? palette[0],
            data: quantities,
          },
          {
            type: 'line',
            label: lineLabel,
            borderColor: palette[0],
            backgroundColor: palette[0].replace(')', ', 0.08)').replace('rgb', 'rgba'),
            tension: 0.4,
            data: cumPct,
          },
        ],
      },
      options: this.scaleOptions(),
    };
  }

  // ─────────────────────────────────────────────────────────────────────────
  // Options partagées
  // ─────────────────────────────────────────────────────────────────────────

  private scaleOptions(): any {
    return {
      maintainAspectRatio: false,
      aspectRatio: 0.8,
      plugins: {
        legend: { labels: { color: this.colors.textColor() } },
      },
      scales: {
        y: {
          ticks: { color: this.colors.textColorSecondary() },
          grid:  { color: this.colors.surfaceBorder() },
        },
        x: {
          ticks: { color: this.colors.textColorSecondary() },
          grid:  { color: this.colors.surfaceBorder() },
        },
      },
    };
  }

  private pieOptions(): any {
    return {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: {
          position: 'bottom',
          labels: { color: this.colors.textColor(), usePointStyle: true },
        },
      },
    };
  }
}
