import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { of } from 'rxjs';

import { PilotageApiService } from '../../data-access/pilotage-api.service';
import { CelluleAnalyse, LigneRemise, MargeRentabilite, RemisesRentabilite, RequetePilotage } from '../../models/pilotage.model';
import { TableauRemisesComponent } from '../../ui/tableau-remises/tableau-remises.component';
import { SectionMargeComponent } from './section-marge.component';
import { SectionRemisesComponent } from './section-remises.component';

const REQUETE: RequetePilotage = {
  predefinie: 'MOIS_EN_COURS',
  du: '2026-10-01',
  au: '2026-10-31',
  comparaison: 'MEME_PERIODE_N_1',
  anneesEnArriere: 1,
  aDate: true,
  granularite: 'MOIS',
};
const COMPARAISON = { periode: { du: REQUETE.du, au: '2026-10-09' }, reference: { du: '2025-10-01', au: '2025-10-09' }, aDate: true };
const cellule = (valeur: number, valeurReference: number | null = null): CelluleAnalyse => ({
  valeur,
  valeurReference,
  ecart: valeurReference === null ? null : valeur - valeurReference,
  ecartPct: null,
});

const MARGE: MargeRentabilite = {
  comparaison: COMPARAISON,
  axe: { code: 'FAMILLE', libelle: 'Famille', sources: ['LIGNES'], suivant: 'PRODUIT', filtrable: true, ordonne: false },
  tauxMarge: 30,
  tauxMargeReference: 34,
  effetMix: -1.5,
  effetTaux: -2.5,
  lignes: [],
  seuilFaibleMarge: 15,
  faiblesMarges: [],
  ventesAMargeNegative: [],
};

const ligneRemise = (libelle: string, alerte = false, tauxMarge: CelluleAnalyse | null = null): LigneRemise => ({
  cle: libelle,
  libelle,
  nbVentes: cellule(10),
  caTtc: cellule(10_000),
  remises: cellule(500),
  tauxRemise: cellule(5),
  tauxMarge,
  alerte,
});

const REMISES: RemisesRentabilite = {
  comparaison: COMPARAISON,
  remises: cellule(2_500, 2_000),
  tauxRemise: cellule(2.3, 2.0),
  partVentesRemisees: cellule(20, 18),
  remiseMoyenne: cellule(125, 110),
  poidsRemisesMarge: cellule(8.3, 7.0),
  octrois: [ligneRemise('Privilège du vendeur')],
  tranches: [ligneRemise('5 à 10 %', false, cellule(10))],
  vendeurs: [ligneRemise('Awa', true)],
  multipleAlerte: 2,
  plusFortesRemises: [
    { id: 1, saleDate: '2026-10-02', numero: 'V1', vendeur: 'Awa', montant: 10_000, remise: 1_500, taux: 15, autorisePar: null },
    { id: 2, saleDate: '2026-10-03', numero: 'V2', vendeur: 'Koffi', montant: 10_000, remise: 400, taux: 4, autorisePar: null },
  ],
};

describe('Onglet « Rentabilité & remises »', () => {
  const router = { navigate: jest.fn() };
  const api = {
    lireSeries: () => of({ comparaison: COMPARAISON, joursOuvres: 8, joursOuvresReference: 8, series: [] }),
    lireMarge: () => of(MARGE),
    lireRemises: () => of(REMISES),
  };

  beforeEach(async () => {
    router.navigate.mockReset();
    await TestBed.configureTestingModule({
      providers: [
        { provide: PilotageApiService, useValue: api },
        { provide: Router, useValue: router },
        { provide: ActivatedRoute, useValue: {} },
      ],
    }).compileComponents();
  });

  async function afficher<T>(composant: new (...args: never[]) => T): Promise<ComponentFixture<T>> {
    const fixture = TestBed.createComponent(composant);
    fixture.componentRef.setInput('requete', REQUETE);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    return fixture;
  }

  it('la marge explique la variation de son taux : mix et taux de chacun', async () => {
    const fixture = await afficher(SectionMargeComponent);

    expect((fixture.nativeElement as HTMLElement).textContent).toContain(
      'Le taux de marge recule de 4,0 pt : −1,5 pt dû au mix (le poids de chaque famille dans le CA), −2,5 pt au taux de chacun.',
    );
  });

  it('un taux au-delà du multiple d’alerte du taux moyen prend la couleur défavorable, mention écrite', async () => {
    const fixture = await afficher(SectionRemisesComponent);
    const taux = Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('.taux-remise'));

    expect(taux.map(t => t.classList.contains('taux-remise--excessif'))).toEqual([true, false]);
    expect(taux[0].textContent).toContain('plus de 2 fois le taux moyen des ventes remisées');
  });

  it('les remises montrent cinq tuiles, et renvoient à Analyser pour les autres ventilations', async () => {
    const fixture = await afficher(SectionRemisesComponent);

    expect((fixture.nativeElement as HTMLElement).querySelectorAll('app-kpi-item').length).toBe(5);
    fixture.componentInstance['analyserLesRemises']();
    expect(router.navigate.mock.calls[0][1].queryParams).toEqual(
      expect.objectContaining({ onglet: 'analyser', ind: 'REMISES,TAUX_REMISE,CA_TTC', axe: 'FAMILLE', aff: 'TABLEAU' }),
    );
  });
});

describe('Tableau des remises', () => {
  it('signale le vendeur au-delà du multiple, et ajoute la marge quand les lignes la donnent', () => {
    const fixture = TestBed.createComponent(TableauRemisesComponent);
    fixture.componentRef.setInput('lignes', [ligneRemise('Awa', true, cellule(12)), ligneRemise('Koffi')]);
    fixture.componentRef.setInput('entete', 'Vendeur');
    fixture.componentRef.setInput('avecAlerte', true);
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;

    expect(element.querySelectorAll('app-badge').length).toBe(1);
    expect(element.textContent).toContain('Plus de 2 fois le taux de l’équipe');
    expect(element.textContent).toContain('Taux de marge');
  });
});
