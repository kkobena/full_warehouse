import { HttpResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, Params, provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { AbilityService } from 'app/core/auth/ability.service';
import { BlobDownloadService } from 'app/shared/services/blob-download.service';
import { PilotageApiService } from '../../data-access/pilotage-api.service';
import { REGLAGE_PAR_DEFAUT } from '../../feature/onglet-analyser/reglage-analyse';
import { REGLAGE_ANNEES_PAR_DEFAUT } from '../../feature/onglet-comparer-annees/reglage-annees';
import { RequetePilotage } from '../../models/pilotage.model';
import { ExportRapportComponent } from './export-rapport.component';

const REQUETE: RequetePilotage = {
  predefinie: 'MOIS_EN_COURS',
  du: '2026-10-01',
  au: '2026-10-31',
  comparaison: 'MEME_PERIODE_N_1',
  anneesEnArriere: 1,
  aDate: true,
  granularite: 'MOIS',
};

describe('Export PDF du pilotage', () => {
  let fixture: ComponentFixture<ExportRapportComponent>;
  const telechargement = { downloadFromObservable: jest.fn() };
  const imprimerRapport = jest.fn(() => of(new HttpResponse<Blob>()));
  let droits: string[];

  async function creer(ongletActif?: string, parametres: Params = {}): Promise<HTMLElement> {
    await TestBed.configureTestingModule({
      imports: [ExportRapportComponent],
      providers: [
        { provide: PilotageApiService, useValue: { imprimerRapport } },
        { provide: BlobDownloadService, useValue: telechargement },
        { provide: AbilityService, useValue: { can: (action: string, code: string) => action === 'export' && droits.includes(code) } },
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { queryParamMap: convertToParamMap(parametres) } } },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(ExportRapportComponent);
    fixture.componentRef.setInput('requete', REQUETE);
    fixture.componentRef.setInput('ongletActif', ongletActif);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  beforeEach(() => {
    telechargement.downloadFromObservable.mockReset();
    imprimerRapport.mockClear();
    try { localStorage.clear(); } catch {}
  });

  it("n'offre pas le bouton sans droit d'export sur un onglet imprimable", async () => {
    // Les objectifs ne s'impriment pas : leur seul droit d'export ne suffit pas à offrir le bouton.
    droits = ['pilotage.objectifs'];
    const element = await creer('tableau-de-bord');

    expect(element.querySelector('app-button')).toBeNull();
  });

  it("coche l'onglet affiché et envoie la période de la barre", async () => {
    droits = ['pilotage.tableau-de-bord'];
    const element = await creer('tableau-de-bord');

    element.querySelector<HTMLButtonElement>('app-button button')!.click();
    fixture.detectChanges();
    document.querySelector<HTMLButtonElement>('.modal-footer app-button:last-child button')!.click();

    expect(imprimerRapport).toHaveBeenCalledWith(REQUETE, ['TABLEAU_DE_BORD'], REGLAGE_ANNEES_PAR_DEFAUT, REGLAGE_PAR_DEFAUT);
    expect(telechargement.downloadFromObservable).toHaveBeenCalledWith(
      expect.anything(),
      'pilotage-2026-10-01-2026-10-31',
      'pdf',
      expect.any(Function),
      expect.any(Function),
    );
  });

  it("imprime les onglets cochés dans l'ordre de la page, quel que soit l'ordre des clics", async () => {
    droits = ['pilotage.tableau-de-bord', 'pilotage.comparer-annees'];
    const element = await creer('comparer-annees');

    element.querySelector<HTMLButtonElement>('app-button button')!.click();
    fixture.detectChanges();
    // .onglets-liste exclut la case « Tout sélectionner »
    const cases = document.querySelectorAll<HTMLInputElement>('.modal-body .onglets-liste input[type="checkbox"]');
    expect(cases.length).toBe(2);
    cases[0].click();
    fixture.detectChanges();
    document.querySelector<HTMLButtonElement>('.modal-footer app-button:last-child button')!.click();

    expect(imprimerRapport).toHaveBeenCalledWith(REQUETE, ['TABLEAU_DE_BORD', 'COMPARER_ANNEES'], REGLAGE_ANNEES_PAR_DEFAUT, REGLAGE_PAR_DEFAUT);
  });

  it("imprime l'analyse telle que l'écran l'affiche : ventilation et chemin de descente de l'URL", async () => {
    droits = ['pilotage.analyser'];
    const element = await creer('analyser', { ind: 'CA_TTC,TAUX_MARGE', axe: 'PRODUIT', aff: 'COURBE', f: 'FAMILLE:12:Antalgiques' });

    element.querySelector<HTMLButtonElement>('app-button button')!.click();
    fixture.detectChanges();
    document.querySelector<HTMLButtonElement>('.modal-footer app-button:last-child button')!.click();

    expect(imprimerRapport).toHaveBeenCalledWith(
      REQUETE,
      ['ANALYSER'],
      REGLAGE_ANNEES_PAR_DEFAUT,
      expect.objectContaining({
        indicateurs: ['CA_TTC', 'TAUX_MARGE'],
        axe: 'PRODUIT',
        affichage: 'COURBE',
        chemin: [{ axe: 'FAMILLE', cle: '12', libelle: 'Antalgiques' }],
      }),
    );
  });

  it('restaure la dernière sélection depuis localStorage', async () => {
    droits = ['pilotage.tableau-de-bord', 'pilotage.comparer-annees'];
    try { localStorage.setItem('pilotage-export-selection', JSON.stringify(['comparer-annees'])); } catch {}
    const element = await creer('tableau-de-bord');

    element.querySelector<HTMLButtonElement>('app-button button')!.click();
    fixture.detectChanges();
    document.querySelector<HTMLButtonElement>('.modal-footer app-button:last-child button')!.click();

    // L'onglet actif (tableau-de-bord) est ignoré : la sélection mémorisée prend la priorité.
    expect(imprimerRapport).toHaveBeenCalledWith(REQUETE, ['COMPARER_ANNEES'], REGLAGE_ANNEES_PAR_DEFAUT, REGLAGE_PAR_DEFAUT);
  });

  it('coche tout avec la case « Tout sélectionner »', async () => {
    droits = ['pilotage.tableau-de-bord', 'pilotage.comparer-annees'];
    const element = await creer('tableau-de-bord');

    element.querySelector<HTMLButtonElement>('app-button button')!.click();
    fixture.detectChanges();
    // La case « Tout sélectionner » est hors de .onglets-liste
    const toutCase = document.querySelector<HTMLInputElement>('.modal-body > div input[type="checkbox"]')!;
    toutCase.click();
    fixture.detectChanges();
    document.querySelector<HTMLButtonElement>('.modal-footer app-button:last-child button')!.click();

    expect(imprimerRapport).toHaveBeenCalledWith(REQUETE, ['TABLEAU_DE_BORD', 'COMPARER_ANNEES'], REGLAGE_ANNEES_PAR_DEFAUT, REGLAGE_PAR_DEFAUT);
  });
});
