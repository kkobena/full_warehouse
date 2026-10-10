import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { PilotageApiService } from '../../data-access/pilotage-api.service';
import { CelluleAnalyse, EncaissementsTresorerie, OrganismeTresorerie, RequetePilotage, TiersPayantTresorerie } from '../../models/pilotage.model';
import { SectionEncaissementsComponent } from './section-encaissements.component';
import { SectionTiersPayantComponent } from './section-tiers-payant.component';

const REQUETE: RequetePilotage = {
  predefinie: 'MOIS_PRECEDENT',
  du: '2026-09-01',
  au: '2026-09-30',
  comparaison: 'AUCUNE',
  anneesEnArriere: 1,
  aDate: false,
  granularite: 'MOIS',
};
const COMPARAISON = { periode: { du: REQUETE.du, au: REQUETE.au }, reference: null, aDate: false };
const seule = (valeur: number): CelluleAnalyse => ({ valeur, valeurReference: null, ecart: null, ecartPct: null });

const ENCAISSEMENTS: EncaissementsTresorerie = {
  comparaison: COMPARAISON,
  total: seule(12_000),
  modes: [
    { cle: 'CASH', libelle: 'Espèces', montant: seule(7_000), part: 58.3 },
    { cle: 'OM', libelle: 'Orange Money', montant: seule(5_000), part: 41.7 },
  ],
  tranches: [{ du: REQUETE.du, au: REQUETE.au }],
  series: [
    { libelle: 'Espèces', valeurs: [7_000] },
    { libelle: 'Orange Money', valeurs: [5_000] },
  ],
  caissiers: null,
};

const MUGEFCI: OrganismeTresorerie = {
  cle: 'T1',
  libelle: 'MUGEFCI',
  facture: 100_000,
  regle: 60_000,
  tauxRecouvrement: 60,
  encours: 40_000,
  partEncours: 100,
  dso: 39,
  enRetard: 40_000,
  delaiRetenu: 20,
  origineDelai: 'OBSERVE',
};

const TIERS_PAYANT: TiersPayantTresorerie = {
  comparaison: COMPARAISON,
  facture: seule(100_000),
  regle: seule(60_000),
  encours: 40_000,
  dso: 39,
  organismes: [MUGEFCI],
  vieillissement: [],
  encaissementsAttendus: [],
  concentrationTrois: 100,
  concentrationCinq: 100,
  seuilHistorique: 3,
};

describe('Onglet « Trésorerie & tiers payant »', () => {
  const api = {
    lireEncaissements: () => of(ENCAISSEMENTS),
    lireTiersPayant: () => of(TIERS_PAYANT),
    lireSeries: () => of({ comparaison: COMPARAISON, joursOuvres: 20, joursOuvresReference: 0, series: [] }),
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({ providers: [{ provide: PilotageApiService, useValue: api }] }).compileComponents();
  });

  async function afficher<T>(composant: new (...args: never[]) => T): Promise<HTMLElement> {
    const fixture = TestBed.createComponent(composant);
    fixture.componentRef.setInput('requete', REQUETE);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  it('encaissements : le total puis les premiers modes ; pas de caissiers sans le droit', async () => {
    const element = await afficher(SectionEncaissementsComponent);

    expect(element.querySelectorAll('app-kpi-item').length).toBe(3);
    expect(element.textContent).toContain('dont Espèces');
    expect(element.textContent).not.toContain('Par caissier');
  });

  it('tiers payant : chaque organisme dit d’où vient son délai', async () => {
    const element = await afficher(SectionTiersPayantComponent);
    const delai = Array.from(element.querySelectorAll('td')).find(cellule => cellule.textContent?.trim() === '20 j');

    expect(delai?.getAttribute('title')).toBe('20 j : délai observé sur ses règlements');
  });
});
