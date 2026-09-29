import { KpiData, SeriesData, TableData } from './dashboard.model';
import { evolutionPct, formatCell, isEmpty, kpiAlert, toSeries, toTable } from './widget-data';
import { buildChart, PREVIOUS_SERIES_PREFIX } from './chart-config';
import { DASHBOARD_TEMPLATES, instantiateTemplate } from './dashboard-templates';

const topProduits: TableData = {
  kind: 'TABLE',
  columns: [
    { field: 'libelle', header: 'Produit', type: 'string' },
    { field: 'quantite', header: 'Quantité', type: 'number' },
    { field: 'montant', header: 'Montant', type: 'amount' },
  ],
  rows: [
    { libelle: 'Doliprane', quantite: 120, montant: 90_000 },
    { libelle: 'Efferalgan', quantite: 80, montant: 60_000 },
  ],
  footer: null,
};

describe('widget-data', () => {
  it('trace un tableau en série, en ne gardant que la colonne demandée', () => {
    const series = toSeries(topProduits, ['montant'])!;
    expect(series.labels).toEqual(['Doliprane', 'Efferalgan']);
    expect(series.series).toEqual([{ name: 'Montant', values: [90_000, 60_000] }]);
  });

  it('sans colonne précisée, trace toutes les mesures', () => {
    expect(toSeries(topProduits)!.series.map(s => s.name)).toEqual(['Quantité', 'Montant']);
  });

  it('présente une série en tableau, une colonne par série', () => {
    const series: SeriesData = { kind: 'SERIES', labels: ['Comptant', 'Tiers payant'], series: [{ name: 'CA net', values: [10, 20] }], footer: 'pied' };
    const table = toTable(series)!;
    expect(table.columns.map(c => c.header)).toEqual(['', 'CA net']);
    expect(table.rows).toEqual([
      { _label: 'Comptant', s0: 10 },
      { _label: 'Tiers payant', s0: 20 },
    ]);
    expect(table.footer).toBe('pied');
  });

  it('une série toute à zéro compte comme vide, un indicateur jamais', () => {
    expect(isEmpty({ kind: 'SERIES', labels: ['a'], series: [{ name: 'x', values: [0] }], footer: null })).toBe(true);
    expect(isEmpty({ kind: 'TABLE', columns: [], rows: [], footer: null })).toBe(true);
    expect(isEmpty({ kind: 'KPI', value: 0, previousValue: null, unit: 'F', detail: null })).toBe(false);
  });

  it('calcule l’évolution, sauf si la période précédente est nulle', () => {
    const kpi = (value: number | null, previousValue: number | null): KpiData => ({ kind: 'KPI', value, previousValue, unit: 'F', detail: null });
    expect(evolutionPct(kpi(120, 100))).toBeCloseTo(20);
    expect(evolutionPct(kpi(80, 100))).toBeCloseTo(-20);
    expect(evolutionPct(kpi(80, 0))).toBeNull();
    expect(evolutionPct(kpi(null, 100))).toBeNull();
  });

  it('signale un indicateur sous ou au-dessus de son seuil, jamais sans seuil', () => {
    expect(kpiAlert(80, { seuil: '100' })).toMatch(/^Sous le seuil/);
    expect(kpiAlert(120, { seuil: '100' })).toBeNull();
    expect(kpiAlert(120, { seuil: '100', alerte: 'DESSUS' })).toMatch(/^Au-dessus du seuil/);
    expect(kpiAlert(80, {})).toBeNull();
    expect(kpiAlert(null, { seuil: '100' })).toBeNull();
  });

  it('formate les cellules à la française', () => {
    expect(formatCell(12500, 'amount')).toBe(new Intl.NumberFormat('fr-FR').format(12500));
    expect(formatCell(12.34, 'percent')).toBe(`${new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 1 }).format(12.3)} %`);
    expect(formatCell(null, 'number')).toBe('');
  });
});

describe('chart-config', () => {
  const evolution: SeriesData = {
    kind: 'SERIES',
    labels: ['01/09', '02/09'],
    series: [
      { name: 'CA', values: [1, 2] },
      { name: PREVIOUS_SERIES_PREFIX, values: [1, 1] },
    ],
    footer: null,
  };

  it('dessine la période précédente en gris pointillé, la série principale en couleur', () => {
    const chart = buildChart('LINE', evolution) as { type: string; data: { datasets: { borderColor: string; borderDash?: number[] }[] } };
    expect(chart.type).toBe('line');
    const [ca, precedente] = chart.data.datasets;
    expect(ca.borderDash).toBeUndefined();
    expect(precedente.borderDash).toEqual([4, 4]);
    expect(precedente.borderColor).not.toBe(ca.borderColor);
  });

  it('regroupe au-delà de huit parts en « Autres » plutôt que d’inventer une couleur', () => {
    const neuf: SeriesData = {
      kind: 'SERIES',
      labels: ['a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i'],
      series: [{ name: 'x', values: [1, 1, 1, 1, 1, 1, 1, 1, 1] }],
      footer: null,
    };
    const chart = buildChart('DOUGHNUT', neuf) as { data: { labels: string[]; datasets: { data: number[] }[] } };
    expect(chart.data.labels).toHaveLength(8);
    expect(chart.data.labels[7]).toBe('Autres');
    expect(chart.data.datasets[0].data[7]).toBe(2);
  });

  it('n’affiche la légende qu’à partir de deux séries', () => {
    const une = buildChart('BAR', { ...evolution, series: [evolution.series[0]] }) as { options: { plugins: { legend: { display: boolean } } } };
    const deux = buildChart('BAR', evolution) as { options: { plugins: { legend: { display: boolean } } } };
    expect(une.options.plugins.legend.display).toBe(false);
    expect(deux.options.plugins.legend.display).toBe(true);
  });
});

describe('dashboard-templates', () => {
  it('retire d’un modèle les widgets que l’utilisateur n’a pas le droit de voir', () => {
    const caisse = DASHBOARD_TEMPLATES.find(t => t.key === 'caisse')!;
    const items = instantiateTemplate(caisse, [
      { key: 'ma-caisse', licensed: true },
      { key: 'mes-encaissements', licensed: true },
    ]);
    expect(items.map(i => i.widgetKey)).toEqual(['ma-caisse', 'mes-encaissements']);
    expect(new Set(items.map(i => i.id)).size).toBe(2);
  });

  it('chaque widget des modèles est connu du registre', () => {
    const all = DASHBOARD_TEMPLATES.flatMap(t => t.items.map(i => ({ key: i.widgetKey, licensed: true })));
    for (const template of DASHBOARD_TEMPLATES) {
      expect(instantiateTemplate(template, all)).toHaveLength(template.items.length);
    }
  });
});
