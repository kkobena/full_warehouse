import { ecartMargeUg, IFiltreJournal, ITotauxUg, nomFichier, parametres } from './journal-remises.model';

const NOVEMBRE: IFiltreJournal = { du: '2026-11-01', au: '2026-11-30', naturesVente: [], origines: [] };

describe('Journal des remises — modèle', () => {
  it("n'envoie pas les listes vides", () => {
    expect(parametres(NOVEMBRE)).toEqual({ du: '2026-11-01', au: '2026-11-30' });
  });

  it('envoie les filtres renseignés', () => {
    expect(parametres({ ...NOVEMBRE, naturesVente: ['COMPTANT'], origines: ['CUMUL'] })).toEqual({
      du: '2026-11-01',
      au: '2026-11-30',
      naturesVente: ['COMPTANT'],
      origines: ['CUMUL'],
    });
  });

  it('nomme le fichier comme le serveur', () => {
    expect(nomFichier('XLSX', 'UG', NOVEMBRE)).toBe('journal-ug-20261101-20261130.xlsx');
    expect(nomFichier('PDF', null, NOVEMBRE)).toBe('journal-remises-20261101-20261130.pdf');
  });

  it('mesure ce que les états de marge sous-estiment à cause des UG', () => {
    const ug = { margeComptabilisee: 1000, margeReelle: 3124 } as ITotauxUg;
    expect(ecartMargeUg(ug)).toBe(2124);
  });
});
