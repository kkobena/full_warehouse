import { parseContextSettings, resolvePeriod } from './period';

describe('period', () => {
  const lundi28Septembre = new Date(2026, 8, 28, 15, 30);

  it.each([
    ['TODAY', '2026-09-28', '2026-09-28'],
    ['YESTERDAY', '2026-09-27', '2026-09-27'],
    ['LAST_7_DAYS', '2026-09-22', '2026-09-28'],
    ['LAST_30_DAYS', '2026-08-30', '2026-09-28'],
    ['THIS_MONTH', '2026-09-01', '2026-09-28'],
    ['LAST_MONTH', '2026-08-01', '2026-08-31'],
    ['THIS_YEAR', '2026-01-01', '2026-09-28'],
  ] as const)('%s va du %s au %s', (preset, start, end) => {
    expect(resolvePeriod(preset, lundi28Septembre)).toEqual({ start, end });
  });

  it('le mois dernier de janvier est décembre de l’année précédente', () => {
    expect(resolvePeriod('LAST_MONTH', new Date(2026, 0, 15))).toEqual({ start: '2025-12-01', end: '2025-12-31' });
  });

  it('remplace une configuration inconnue par les valeurs par défaut', () => {
    expect(parseContextSettings({ period: 'HIER_SOIR', refreshSeconds: 7 })).toEqual({ period: 'TODAY', refreshSeconds: 0 });
    expect(parseContextSettings({ period: 'THIS_MONTH', refreshSeconds: 300 })).toEqual({ period: 'THIS_MONTH', refreshSeconds: 300 });
    expect(parseContextSettings(undefined)).toEqual({ period: 'TODAY', refreshSeconds: 0 });
  });
});
