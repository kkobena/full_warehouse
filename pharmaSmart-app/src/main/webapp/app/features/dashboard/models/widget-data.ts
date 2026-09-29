import { formatCurrency, formatDateFR, formatDecimal, formatNumber } from 'app/shared/utils/format-utils';
import { ColumnType, KpiData, SeriesData, TableData, WidgetData } from './dashboard.model';

const NUMERIC: ColumnType[] = ['number', 'amount', 'percent'];

/** Un même résultat s'affiche en graphique ou en tableau : ces conversions font le pont. */
export function toSeries(data: WidgetData, chartColumns?: string[]): SeriesData | null {
  if (data.kind === 'SERIES') {
    return data;
  }
  if (data.kind !== 'TABLE') {
    return null;
  }
  const labelColumn = data.columns.find(c => c.type === 'string');
  const valueColumns = data.columns.filter(c => NUMERIC.includes(c.type) && (!chartColumns || chartColumns.includes(c.field)));
  if (!labelColumn || valueColumns.length === 0) {
    return null;
  }
  return {
    kind: 'SERIES',
    labels: data.rows.map(r => String(r[labelColumn.field] ?? '')),
    series: valueColumns.map(c => ({ name: c.header, values: data.rows.map(r => toNumber(r[c.field])) })),
    footer: data.footer,
  };
}

export function toTable(data: WidgetData, valueType: ColumnType = 'number'): TableData | null {
  if (data.kind === 'TABLE') {
    return data;
  }
  if (data.kind !== 'SERIES') {
    return null;
  }
  return {
    kind: 'TABLE',
    columns: [{ field: '_label', header: '', type: 'string' }, ...data.series.map((s, i) => ({ field: `s${i}`, header: s.name, type: valueType }))],
    rows: data.labels.map((label, row) => {
      const cells: Record<string, unknown> = { _label: label };
      data.series.forEach((s, i) => (cells[`s${i}`] = s.values[row]));
      return cells;
    }),
    footer: data.footer,
  };
}

export function isEmpty(data: WidgetData): boolean {
  switch (data.kind) {
    case 'KPI':
      return false;
    case 'SERIES':
      return data.labels.length === 0 || data.series.every(s => s.values.every(v => !v));
    case 'TABLE':
      return data.rows.length === 0;
  }
}

/** Alerte d'un indicateur au regard du seuil réglé sur sa tuile ; null sans seuil ou hors alerte. */
export function kpiAlert(value: number | null, params: Record<string, string> | undefined): string | null {
  const raw = params?.['seuil'];
  if (value == null || raw == null || raw === '' || Number.isNaN(Number(raw))) {
    return null;
  }
  const seuil = Number(raw);
  if (params?.['alerte'] === 'DESSUS') {
    return value > seuil ? `Au-dessus du seuil (${formatCurrency(seuil)})` : null;
  }
  return value < seuil ? `Sous le seuil (${formatCurrency(seuil)})` : null;
}

/** Évolution en % par rapport à la période précédente ; null si elle n'a pas de sens. */
export function evolutionPct(kpi: KpiData): number | null {
  if (kpi.value == null || kpi.previousValue == null || kpi.previousValue === 0) {
    return null;
  }
  return ((kpi.value - kpi.previousValue) * 100) / Math.abs(kpi.previousValue);
}

/** Valeur d'un indicateur : montant sans décimale, « — » quand il n'y a rien à afficher. */
export function formatKpiValue(value: number | null | undefined): string {
  return value == null ? '—' : formatCurrency(value);
}

/** Évolution signée à une décimale : « +12,5 % ». */
export function formatEvolution(pct: number): string {
  return `${pct > 0 ? '+' : ''}${formatDecimal(pct, 1)} %`;
}

export function formatCell(value: unknown, type: ColumnType): string {
  if (value == null || value === '') {
    return '';
  }
  switch (type) {
    case 'number':
      return formatNumber(Number(value));
    case 'amount':
      return formatCurrency(Number(value));
    case 'percent':
      return `${formatDecimal(Number(value), 1)} %`;
    case 'date':
      return formatDateFR(String(value));
    case 'datetime':
      return formatDateTime(String(value));
    default:
      return String(value);
  }
}

/** Heure seule pour aujourd'hui, date et heure sinon. */
function formatDateTime(value: string): string {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  const time = date.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
  return date.toDateString() === new Date().toDateString() ? time : `${date.toLocaleDateString('fr-FR', { day: '2-digit', month: '2-digit' })} ${time}`;
}

function toNumber(value: unknown): number | null {
  const n = Number(value);
  return value == null || Number.isNaN(n) ? null : n;
}
