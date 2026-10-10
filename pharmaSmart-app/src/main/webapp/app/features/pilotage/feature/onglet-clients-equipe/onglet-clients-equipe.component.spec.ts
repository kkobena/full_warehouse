import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { PilotageApiService } from '../../data-access/pilotage-api.service';
import { AnalysePilotage, RequetePilotage } from '../../models/pilotage.model';
import { CarteChaleurComponent } from '../../ui/carte-chaleur/carte-chaleur.component';
import { SectionFrequentationComponent } from './section-frequentation.component';

const REQUETE: RequetePilotage = {
  predefinie: 'MOIS_PRECEDENT',
  du: '2026-09-01',
  au: '2026-09-30',
  comparaison: 'AUCUNE',
  anneesEnArriere: 1,
  aDate: false,
  granularite: 'MOIS',
};
const cellule = (valeur: number | null) => ({ valeur, valeurReference: null, ecart: null, ecartPct: null });
const CROISE = {
  colonnes: [
    { cle: '1', libelle: 'Lundi' },
    { cle: '2', libelle: 'Mardi' },
  ],
  lignes: [
    { cle: '9', libelle: '9 h', cellules: [cellule(10), cellule(4)] },
    { cle: '10', libelle: '10 h', cellules: [cellule(42), cellule(null)] },
  ],
};

describe('Onglet « Clients & équipe »', () => {
  const api = {
    analyser: jest.fn(() => of({ croise: CROISE } as unknown as AnalysePilotage)),
    lireSeries: jest.fn(() => of({ comparaison: { periode: { du: REQUETE.du, au: REQUETE.au }, reference: null, aDate: false }, joursOuvres: 20, joursOuvresReference: 0, series: [] })),
  };

  beforeEach(async () => {
    jest.clearAllMocks();
    await TestBed.configureTestingModule({ providers: [{ provide: PilotageApiService, useValue: api }] }).compileComponents();
  });

  it('fréquentation : heure × jour de semaine, créneaux les plus chargés dits en clair, ventes par jour', async () => {
    const fixture = TestBed.createComponent(SectionFrequentationComponent);
    fixture.componentRef.setInput('requete', REQUETE);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(api.analyser).toHaveBeenCalledWith(REQUETE, expect.objectContaining({ axe: 'HEURE', axe2: 'JOUR_SEMAINE', indicateurs: ['NB_VENTES'] }));
    expect(api.lireSeries).toHaveBeenCalledWith(expect.objectContaining({ granularite: 'JOUR' }), ['NB_VENTES']);
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Créneaux les plus chargés : lundi 10 h (42 ventes), lundi 9 h (10 ventes), mardi 9 h (4 ventes).');
  });

  it('carte de chaleur : la valeur est écrite dans chaque case, le fond s’intensifie jusqu’au maximum', () => {
    const fixture = TestBed.createComponent(CarteChaleurComponent);
    fixture.componentRef.setInput('croise', CROISE);
    fixture.componentRef.setInput('entete', 'Heure');
    fixture.componentRef.setInput('legende', 'Ventes');
    fixture.detectChanges();
    const cases = Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('td'));

    expect(cases.map(td => td.textContent?.trim())).toEqual(['10', '4', '42', '']);
    expect(cases[2].style.getPropertyValue('--intensite')).toBe('45%');
    expect(cases[3].style.getPropertyValue('--intensite')).toBe('0%');
  });
});
