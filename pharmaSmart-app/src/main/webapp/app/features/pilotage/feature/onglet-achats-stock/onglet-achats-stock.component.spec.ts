import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { PilotageApiService } from '../../data-access/pilotage-api.service';
import { CelluleAnalyse, RequetePilotage, RupturesPilotage } from '../../models/pilotage.model';
import { SectionAchatsVentesComponent } from './section-achats-ventes.component';
import { SectionRupturesComponent } from './section-ruptures.component';

const REQUETE: RequetePilotage = {
  predefinie: 'MOIS_PRECEDENT',
  du: '2026-09-01',
  au: '2026-09-30',
  comparaison: 'MEME_PERIODE_N_1',
  anneesEnArriere: 1,
  aDate: true,
  granularite: 'MOIS',
};
const COMPARAISON = { periode: { du: REQUETE.du, au: REQUETE.au }, reference: { du: '2025-09-01', au: '2025-09-30' }, aDate: false };
const cellule = (valeur: number, valeurReference: number): CelluleAnalyse => ({ valeur, valeurReference, ecart: valeur - valeurReference, ecartPct: null });

const RUPTURES: RupturesPilotage = {
  comparaison: COMPARAISON,
  tauxRupture: cellule(5, 4),
  ventesManquees: cellule(7_500, 6_000),
  tauxVentesManquees: cellule(2, 2.5),
  valeurPerimee: cellule(3_000, 0),
  valeurAPerimerTroisMois: 12_000,
  rupturesParFournisseur: [],
  rupturesParProduit: [],
  ventesManqueesParProduit: [{ cle: '1', libelle: 'Doliprane 1000', nombre: 3, quantite: 5, montant: 7_500 }],
  peremptionsAVenir: [{ annee: 2026, mois: 11, quantite: 4, valeur: 2_000 }],
};

describe('Onglet « Achats & stock »', () => {
  const api = {
    lireRuptures: () => of(RUPTURES),
    lireAchatsVentes: jest.fn(() => of({ comparaison: COMPARAISON, points: [], familles: [] })),
    lireSeries: jest.fn(() => of({ comparaison: COMPARAISON, joursOuvres: 20, joursOuvresReference: 20, series: [] })),
  };

  beforeEach(async () => {
    jest.clearAllMocks();
    await TestBed.configureTestingModule({ providers: [{ provide: PilotageApiService, useValue: api }] }).compileComponents();
  });

  it('ruptures : cinq tuiles, ventes manquées par produit, péremptions par mois en clair', async () => {
    const fixture = TestBed.createComponent(SectionRupturesComponent);
    fixture.componentRef.setInput('requete', REQUETE);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;

    expect(element.querySelectorAll('app-kpi-item').length).toBe(5);
    expect(element.textContent).toContain('Doliprane 1000');
    expect(element.textContent).toContain('novembre 2026');
    // Listes vides : pas de carte.
    expect(element.textContent).not.toContain('Ruptures par fournisseur');
    expect(element.textContent).not.toContain('Ruptures par produit');
  });

  it('le ratio sur 12 mois glissants se lit sur les 12 mois qui finissent la période, sans comparaison', async () => {
    const fixture = TestBed.createComponent(SectionAchatsVentesComponent);
    fixture.componentRef.setInput('requete', REQUETE);
    fixture.detectChanges();
    await fixture.whenStable();

    expect(api.lireSeries).toHaveBeenCalledWith(expect.objectContaining({ du: '2025-10-01', au: '2026-09-30', comparaison: 'AUCUNE' }), ['RATIO_VENTES_ACHATS']);
  });
});
