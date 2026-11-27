import { TestBed } from '@angular/core/testing';
import { JournalRemisesFiltresStore } from './journal-remises-filtres.store';

describe('Journal des remises — filtres partagés', () => {
  let store: JournalRemisesFiltresStore;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [JournalRemisesFiltresStore] });
    store = TestBed.inject(JournalRemisesFiltresStore);
  });

  it("s'ouvre sur le mois en cours, sans filtre", () => {
    const demande = store.demande();
    expect(demande.du.endsWith('-01')).toBe(true);
    expect(demande.naturesVente).toEqual([]);
    expect(demande.origines).toEqual([]);
  });

  it("n'applique la saisie qu'à « Afficher »", () => {
    store.du.set({ year: 2026, month: 11, day: 3 });
    store.natureVente.set('ASSURANCE');
    store.origine.set('PROMOTION');
    expect(store.demande().naturesVente).toEqual([]);

    store.appliquer();

    expect(store.demande()).toEqual(expect.objectContaining({ du: '2026-11-03', naturesVente: ['ASSURANCE'], origines: ['PROMOTION'] }));
  });
});
