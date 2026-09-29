import { formatDecimal, formatNumber } from 'app/shared/utils/format-utils';
import { SeriesData, VizType } from './dashboard.model';

/**
 * Palette catégorielle de référence (skill dataviz, validée pour les daltonismes), dans un ordre
 * fixe : une couleur suit la série, jamais son rang. Au-delà de huit parts, le reste est regroupé
 * en « Autres » plutôt que de générer une teinte.
 */
const SERIES_LIGHT = ['#2a78d6', '#eb6834', '#1baf7a', '#eda100', '#e87ba4', '#008300', '#4a3aa7', '#e34948'];
const SERIES_DARK = ['#3987e5', '#d95926', '#199e70', '#c98500', '#d55181', '#008300', '#9085e9', '#e66767'];
const MAX_SLICES = 8;

/** Série de comparaison : grise et en pointillés, elle s'efface derrière la série principale. */
export const PREVIOUS_SERIES_PREFIX = 'Période précédente';

interface Theme {
  series: string[];
  muted: string;
  grid: string;
  text: string;
  surface: string;
}

function theme(): Theme {
  const dark = typeof window !== 'undefined' && window.matchMedia?.('(prefers-color-scheme: dark)').matches;
  return dark
    ? { series: SERIES_DARK, muted: '#8a8984', grid: 'rgba(255,255,255,0.08)', text: '#c3c2b7', surface: '#1a1a19' }
    : { series: SERIES_LIGHT, muted: '#9a9994', grid: 'rgba(0,0,0,0.06)', text: '#52514e', surface: '#ffffff' };
}

const COMPACT = new Intl.NumberFormat('fr-FR', { notation: 'compact', maximumFractionDigits: 1 });

export interface ChartConfig {
  type: 'line' | 'bar' | 'pie' | 'doughnut';
  data: unknown;
  options: unknown;
}

export function buildChart(viz: VizType, series: SeriesData): ChartConfig {
  const t = theme();
  return viz === 'PIE' || viz === 'DOUGHNUT' ? buildPie(viz, series, t) : buildCartesian(viz, series, t);
}

function buildCartesian(viz: VizType, data: SeriesData, t: Theme): ChartConfig {
  const horizontal = viz === 'HBAR';
  const line = viz === 'LINE';
  let slot = 0;
  const datasets = data.series.map(s => {
    const previous = s.name.startsWith(PREVIOUS_SERIES_PREFIX);
    const color = previous ? t.muted : t.series[slot++ % t.series.length];
    return line
      ? {
          label: s.name,
          data: s.values,
          borderColor: color,
          backgroundColor: color,
          borderWidth: 2,
          borderDash: previous ? [4, 4] : undefined,
          pointRadius: 0,
          pointHoverRadius: 4,
          tension: 0.25,
        }
      : {
          label: s.name,
          data: s.values,
          backgroundColor: previous ? t.muted : color,
          borderRadius: 4,
          borderSkipped: 'start',
          maxBarThickness: 28,
        };
  });

  const valueAxis = {
    beginAtZero: true,
    grid: { color: t.grid },
    border: { display: false },
    ticks: { color: t.text, callback: (v: number | string) => COMPACT.format(Number(v)) },
  };
  const categoryAxis = { grid: { display: false }, border: { display: false }, ticks: { color: t.text, autoSkip: true, maxRotation: 0 } };

  return {
    type: line ? 'line' : 'bar',
    data: { labels: data.labels, datasets },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      indexAxis: horizontal ? 'y' : 'x',
      interaction: { mode: 'index', intersect: false },
      plugins: {
        legend: { display: data.series.length >= 2, position: 'bottom', labels: { color: t.text, boxWidth: 12 } },
        tooltip: { callbacks: { label: (ctx: TooltipContext) => `${ctx.dataset.label} : ${formatNumber(Number(ctx.parsed[horizontal ? 'x' : 'y']))}` } },
      },
      scales: horizontal ? { x: valueAxis, y: categoryAxis } : { x: categoryAxis, y: valueAxis },
    },
  };
}

function buildPie(viz: VizType, data: SeriesData, t: Theme): ChartConfig {
  const values = data.series[0]?.values ?? [];
  let labels = data.labels;
  let slices = values.map(v => v ?? 0);
  if (slices.length > MAX_SLICES) {
    const kept = slices.slice(0, MAX_SLICES - 1);
    const others = slices.slice(MAX_SLICES - 1).reduce((a, b) => a + b, 0);
    slices = [...kept, others];
    labels = [...labels.slice(0, MAX_SLICES - 1), 'Autres'];
  }
  return {
    type: viz === 'PIE' ? 'pie' : 'doughnut',
    data: {
      labels,
      datasets: [
        {
          label: data.series[0]?.name ?? '',
          data: slices,
          backgroundColor: slices.map((_, i) => t.series[i]),
          borderColor: t.surface,
          borderWidth: 2,
        },
      ],
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { display: true, position: 'right', labels: { color: t.text, boxWidth: 12 } },
        tooltip: {
          callbacks: {
            label: (ctx: TooltipContext) => {
              const total = slices.reduce((a, b) => a + b, 0);
              const value = Number(ctx.parsed);
              const share = total > 0 ? ` (${formatDecimal((value * 100) / total, 1)} %)` : '';
              return `${ctx.label} : ${formatNumber(value)}${share}`;
            },
          },
        },
      },
    },
  };
}

interface TooltipContext {
  label: string;
  parsed: any;
  dataset: { label?: string };
}
