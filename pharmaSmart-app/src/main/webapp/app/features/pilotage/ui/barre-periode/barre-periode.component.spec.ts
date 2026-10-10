import dayjs from 'dayjs/esm';

import { calculerPeriode } from './barre-periode.component';

describe('Barre de période — périodes prédéfinies', () => {
  const NEUF_OCTOBRE = dayjs('2026-10-09');

  it.each([
    ['MOIS_EN_COURS', '2026-10-01', '2026-10-31'],
    ['MOIS_PRECEDENT', '2026-09-01', '2026-09-30'],
    ['TRIMESTRE_EN_COURS', '2026-10-01', '2026-12-31'],
    ['ANNEE_EN_COURS', '2026-01-01', '2026-12-31'],
    ['ANNEE_PRECEDENTE', '2025-01-01', '2025-12-31'],
    ['DOUZE_MOIS_GLISSANTS', '2025-11-01', '2026-10-31'],
  ] as const)('%s : du %s au %s', (predefinie, du, au) => {
    expect(calculerPeriode(predefinie, NEUF_OCTOBRE)).toEqual({ du, au });
  });

  it('le trimestre en cours de février commence en janvier', () => {
    expect(calculerPeriode('TRIMESTRE_EN_COURS', dayjs('2026-02-14'))).toEqual({ du: '2026-01-01', au: '2026-03-31' });
  });
});
