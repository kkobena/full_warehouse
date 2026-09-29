import { formatDate } from 'app/shared/utils/format-utils';
import { DashboardContextSettings, DateRange, PeriodPreset } from './dashboard.model';

export const DEFAULT_CONTEXT: DashboardContextSettings = { period: 'TODAY', refreshSeconds: 0 };

export const PERIOD_OPTIONS: { value: PeriodPreset; label: string }[] = [
  { value: 'TODAY', label: "Aujourd'hui" },
  { value: 'YESTERDAY', label: 'Hier' },
  { value: 'LAST_7_DAYS', label: '7 derniers jours' },
  { value: 'LAST_30_DAYS', label: '30 derniers jours' },
  { value: 'THIS_MONTH', label: 'Ce mois-ci' },
  { value: 'LAST_MONTH', label: 'Le mois dernier' },
  { value: 'THIS_YEAR', label: 'Cette année' },
];

export const REFRESH_OPTIONS: { value: number; label: string }[] = [
  { value: 0, label: 'Pas de rafraîchissement' },
  { value: 60, label: 'Toutes les minutes' },
  { value: 300, label: 'Toutes les 5 minutes' },
  { value: 900, label: 'Toutes les 15 minutes' },
];

/** Bornes d'une période relative, calculées en date locale (celle de l'officine). */
export function resolvePeriod(preset: PeriodPreset, today: Date = new Date()): DateRange {
  const d = new Date(today.getFullYear(), today.getMonth(), today.getDate());
  const shift = (days: number): Date => new Date(d.getFullYear(), d.getMonth(), d.getDate() + days);
  switch (preset) {
    case 'YESTERDAY':
      return range(shift(-1), shift(-1));
    case 'LAST_7_DAYS':
      return range(shift(-6), d);
    case 'LAST_30_DAYS':
      return range(shift(-29), d);
    case 'THIS_MONTH':
      return range(new Date(d.getFullYear(), d.getMonth(), 1), d);
    case 'LAST_MONTH':
      return range(new Date(d.getFullYear(), d.getMonth() - 1, 1), new Date(d.getFullYear(), d.getMonth(), 0));
    case 'THIS_YEAR':
      return range(new Date(d.getFullYear(), 0, 1), d);
    case 'TODAY':
    default:
      return range(d, d);
  }
}

export function parseContextSettings(raw: unknown): DashboardContextSettings {
  const value = raw as Partial<DashboardContextSettings> | null | undefined;
  const period = PERIOD_OPTIONS.some(o => o.value === value?.period) ? value!.period! : DEFAULT_CONTEXT.period;
  const refreshSeconds = REFRESH_OPTIONS.some(o => o.value === value?.refreshSeconds) ? value!.refreshSeconds! : DEFAULT_CONTEXT.refreshSeconds;
  return { period, refreshSeconds };
}

function range(start: Date, end: Date): DateRange {
  return { start: formatDate(start), end: formatDate(end) };
}
