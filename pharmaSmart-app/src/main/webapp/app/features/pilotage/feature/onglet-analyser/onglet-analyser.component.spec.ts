import { HttpResponse } from '@angular/common/http';
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, ParamMap, Router } from '@angular/router';
import { BehaviorSubject, of } from 'rxjs';

import { AbilityService } from 'app/core/auth/ability.service';
import { BlobDownloadService } from 'app/shared/services/blob-download.service';
import { PilotageApiService } from '../../data-access/pilotage-api.service';
import { AnalysePilotage, Axe, ElementAnalyse, RequetePilotage, VuePilotage } from '../../models/pilotage.model';
import { OngletAnalyserComponent } from './onglet-analyser.component';

const REQUETE: RequetePilotage = {
  predefinie: 'MOIS_EN_COURS',
  du: '2026-10-01',
  au: '2026-10-31',
  comparaison: 'MEME_PERIODE_N_1',
  anneesEnArriere: 1,
  aDate: true,
  granularite: 'MOIS',
};

const axe = (code: Axe['code'], suivant: Axe['suivant'], sources: Axe['sources'] = ['LIGNES']): Axe => ({
  code,
  libelle: code === 'FAMILLE' ? 'Famille' : code === 'PRODUIT' ? 'Produit' : code,
  sources,
  suivant,
  filtrable: true,
  ordonne: false,
});

const AXES = [axe('FAMILLE', 'PRODUIT'), axe('PRODUIT', null), axe('HEURE', null, ['ENTETES']), axe('NATURE_VENTE', 'FAMILLE', ['LIGNES', 'ENTETES'])];

const ANTALGIQUES: ElementAnalyse = {
  cle: '12',
  libelle: 'Antalgiques',
  cellules: [{ valeur: 60_000, valeurReference: 50_000, ecart: 10_000, ecartPct: 20 }],
  part: 60,
  contribution: 100,
};

const ANALYSE: AnalysePilotage = {
  comparaison: { periode: { du: REQUETE.du, au: '2026-10-09' }, reference: { du: '2025-10-01', au: '2025-10-09' }, aDate: true },
  source: 'LIGNES',
  indicateurs: [{ code: 'CA_TTC', libelle: 'CA TTC', definition: '', unite: 'MONTANT', sensFavorable: 'HAUSSE', droit: 'PAGE' }],
  indicateursIgnores: [],
  axe: AXES[0],
  axe2: null,
  total: [{ valeur: 100_000, valeurReference: 90_000, ecart: 10_000, ecartPct: 11.1 }],
  elements: [ANTALGIQUES],
  autres: null,
  nombreElements: 1,
  croise: null,
};

const VUE_LIVREE: VuePilotage = {
  id: 2,
  libelle: 'Meilleures ventes',
  indicateurs: ['CA_TTC', 'QUANTITES_VENDUES'],
  axe: 'PRODUIT',
  axe2: null,
  top: 50,
  tri: 'VALEUR',
  affichage: 'TABLEAU',
  livree: true,
  partagee: false,
  modifiable: false,
};

describe('Onglet Analyser du pilotage', () => {
  let fixture: ComponentFixture<OngletAnalyserComponent>;
  let parametres: BehaviorSubject<ParamMap>;
  const router = { navigate: jest.fn() };
  const api = {
    listerIndicateurs: () => of(ANALYSE.indicateurs),
    listerAxes: () => of(AXES),
    listerVues: () => of([VUE_LIVREE]),
    analyser: jest.fn(() => of(ANALYSE)),
    expliquerEcart: jest.fn(),
    exporterAnalyse: jest.fn(() => of(new HttpResponse<Blob>())),
  };
  const telechargement = { downloadFromObservable: jest.fn() };

  async function ouvrir(params: Record<string, string | string[]> = {}): Promise<OngletAnalyserComponent> {
    parametres = new BehaviorSubject(convertToParamMap(params));
    await TestBed.configureTestingModule({
      imports: [OngletAnalyserComponent],
      providers: [
        { provide: PilotageApiService, useValue: api },
        { provide: ActivatedRoute, useValue: { queryParamMap: parametres } },
        { provide: Router, useValue: router },
        { provide: BlobDownloadService, useValue: telechargement },
        { provide: AbilityService, useValue: { can: () => true, canSignal: () => signal(true) } },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(OngletAnalyserComponent);
    fixture.componentRef.setInput('requete', REQUETE);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    return fixture.componentInstance;
  }

  function derniereNavigation(): Record<string, unknown> {
    return router.navigate.mock.calls.at(-1)[1].queryParams;
  }

  beforeEach(() => {
    router.navigate.mockReset();
    telechargement.downloadFromObservable.mockReset();
    api.analyser.mockClear();
  });

  it('analyse avec le réglage de l’URL et affiche les éléments', async () => {
    await ouvrir({ ind: 'CA_TTC', axe: 'FAMILLE' });

    expect(api.analyser).toHaveBeenCalledWith(REQUETE, expect.objectContaining({ axe: 'FAMILLE', indicateurs: ['CA_TTC'] }));
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Antalgiques');
  });

  it("détailler un élément l'ajoute au chemin et ventile par l'axe suivant", async () => {
    const onglet = await ouvrir({ axe: 'FAMILLE' });

    onglet['descendre'](ANTALGIQUES, ANALYSE);

    expect(derniereNavigation()).toEqual(expect.objectContaining({ axe: 'PRODUIT', f: ['FAMILLE:12:Antalgiques'], vue: null }));
  });

  it('remonter dans le fil rend l’axe de ce niveau', async () => {
    const onglet = await ouvrir({ axe: 'PRODUIT', f: ['FAMILLE:12:Antalgiques'] });

    onglet['remonter'](0);

    expect(derniereNavigation()).toEqual(expect.objectContaining({ axe: 'FAMILLE', f: null }));
  });

  it('changer d’axe abandonne un second axe qui ne se croise plus', async () => {
    const onglet = await ouvrir({ axe: 'NATURE_VENTE', axe2: 'HEURE' });

    onglet['choisirAxe']('FAMILLE');

    expect(derniereNavigation()).toEqual(expect.objectContaining({ axe: 'FAMILLE', axe2: null }));
  });

  it('choisir une vue applique son réglage, repart du haut et la retient', async () => {
    const onglet = await ouvrir({ axe: 'PRODUIT', f: ['FAMILLE:12:Antalgiques'] });

    onglet['choisirVue'](2);

    expect(derniereNavigation()).toEqual(
      expect.objectContaining({ ind: 'CA_TTC,QUANTITES_VENDUES', axe: 'PRODUIT', top: 50, aff: 'TABLEAU', f: null, vue: 2 }),
    );
  });

  it('fait générer le fichier par le serveur, avec le réglage affiché', async () => {
    const onglet = await ouvrir({ axe: 'FAMILLE' });

    onglet['exporter'](ANALYSE);

    expect(api.exporterAnalyse).toHaveBeenCalledWith(REQUETE, expect.objectContaining({ axe: 'FAMILLE' }));
    expect(telechargement.downloadFromObservable).toHaveBeenCalledWith(expect.anything(), 'pilotage-famille-2026-10-01-2026-10-31', 'csv');
  });
});
