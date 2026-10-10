import { RequetePilotage } from '../models/pilotage.model';
import { elargirAuxTreizeTranches, libellerHorizon } from './horizon-courbe';

const OCTOBRE: RequetePilotage = {
  predefinie: 'MOIS_EN_COURS',
  du: '2026-10-01',
  au: '2026-10-31',
  comparaison: 'MEME_PERIODE_N_1',
  anneesEnArriere: 1,
  aDate: true,
  granularite: 'MOIS',
};

describe('Horizon des courbes du pilotage', () => {
  it('remonte 13 tranches de la granularité, fin de période inchangée', () => {
    expect(elargirAuxTreizeTranches(OCTOBRE)).toEqual({ ...OCTOBRE, du: '2025-10-01' });
    expect(elargirAuxTreizeTranches({ ...OCTOBRE, granularite: 'SEMAINE' }).du).toBe('2026-08-03');
    expect(elargirAuxTreizeTranches({ ...OCTOBRE, granularite: 'TRIMESTRE' }).du).toBe('2023-10-01');
    expect(elargirAuxTreizeTranches({ ...OCTOBRE, granularite: 'ANNEE' }).du).toBe('2021-01-01');
    expect(libellerHorizon('MOIS')).toBe('13 derniers mois');
  });

  it('ne rétrécit jamais une période déjà plus longue', () => {
    const longue = { ...OCTOBRE, du: '2024-01-01' };
    expect(elargirAuxTreizeTranches(longue)).toBe(longue);
  });
});
