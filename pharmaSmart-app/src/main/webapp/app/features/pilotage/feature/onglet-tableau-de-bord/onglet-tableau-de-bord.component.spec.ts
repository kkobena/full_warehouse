import { provideRouter } from '@angular/router';
import { HttpResponse } from '@angular/common/http';
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { AbilityService } from 'app/core/auth/ability.service';
import { BlobDownloadService } from 'app/shared/services/blob-download.service';
import { PilotageApiService } from '../../data-access/pilotage-api.service';
import { AlertePilotage, EcartsPilotage, RequetePilotage, SerieIndicateur, SeriesPilotage } from '../../models/pilotage.model';
import { OngletTableauDeBordComponent } from './onglet-tableau-de-bord.component';

const REQUETE: RequetePilotage = {
  predefinie: 'MOIS_EN_COURS',
  du: '2026-10-01',
  au: '2026-10-31',
  comparaison: 'MEME_PERIODE_N_1',
  anneesEnArriere: 1,
  aDate: true,
  granularite: 'MOIS',
};

const CA_TTC: SerieIndicateur = {
  indicateur: { code: 'CA_TTC', libelle: 'CA TTC', definition: 'Ventes encaissées TTC', unite: 'MONTANT', sensFavorable: 'HAUSSE', droit: 'PAGE' },
  valeur: 1250.5,
  valeurReference: 1000,
  ecart: 250.5,
  ecartPct: 25.05,
  points: [{ debut: '2026-10-01', fin: '2026-10-09', libelle: 'Oct. 2026 (au 9)', valeur: 1250.5, valeurReference: 1000, ecart: 250.5, ecartPct: 25.05, objectif: 1500 }],
  objectif: 1500,
  projection: { realiseADate: 1250.5, projection: 4300, objectif: 4650, atteinteProjetee: 92.5, tenu: false, methode: "D'après octobre 2025 : au même jour, 29 % du mois était fait." },
};

const SERIES: SeriesPilotage = {
  comparaison: { periode: { du: '2026-10-01', au: '2026-10-09' }, reference: { du: '2025-10-01', au: '2025-10-09' }, aDate: true },
  joursOuvres: 8,
  joursOuvresReference: 8,
  series: [CA_TTC],
};

const ECARTS: EcartsPilotage = {
  comparaison: SERIES.comparaison,
  ca: 1250.5,
  caReference: 1000,
  effetFrequentation: 100,
  effetArticles: 50,
  effetPrix: 100.5,
  hausses: [{ axe: 'FAMILLE', libelleAxe: 'Famille', cle: '1', libelle: 'Antalgiques', valeur: 600, valeurReference: 400, ecart: 200 }],
  baisses: [],
};

const ALERTES: AlertePilotage[] = [
  { code: 'CHUTE_ACTIVITE', gravite: 'HAUTE', titre: "Chute d'activité", detail: 'Le CA des 7 derniers jours atteint 85 %.', onglet: 'tableau-de-bord' },
  { code: 'CREANCES', gravite: 'MOYENNE', titre: 'Un organisme tarde à payer', detail: 'MUGEF-CI (120 j).', onglet: 'tresorerie-tiers-payant' },
];

describe('Onglet Tableau de bord du pilotage', () => {
  let fixture: ComponentFixture<OngletTableauDeBordComponent>;
  const telechargement = { downloadFromObservable: jest.fn() };
  const exporterSeries = jest.fn(() => of(new HttpResponse<Blob>()));
  const droitExport = signal(true);

  beforeEach(async () => {
    localStorage.clear();
    telechargement.downloadFromObservable.mockReset();
    await TestBed.configureTestingModule({
      imports: [OngletTableauDeBordComponent],
      providers: [
        { provide: PilotageApiService, useValue: { lireSeries: () => of(SERIES), lireEcarts: () => of(ECARTS), exporterSeries, listerAlertes: () => of(ALERTES) } },
        { provide: BlobDownloadService, useValue: telechargement },
        { provide: AbilityService, useValue: { canSignal: () => droitExport } },
        provideRouter([]),
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(OngletTableauDeBordComponent);
    fixture.componentRef.setInput('requete', REQUETE);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  });

  it('affiche seulement les tuiles des indicateurs reçus, et ce qui a bougé', () => {
    const element = fixture.nativeElement as HTMLElement;
    expect(element.querySelectorAll('app-kpi-item').length).toBe(1);
    expect(element.textContent).toContain('Antalgiques');
    expect(element.textContent).toContain('Fréquentation');
  });

  it('libelle les tranches comme le serveur les nomme', () => {
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Oct. 2026 (au 9)');
  });

  it('fait générer le CSV par le serveur, sur les colonnes du tableau', () => {
    (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('app-button button')!.click();

    expect(exporterSeries).toHaveBeenCalledWith(REQUETE, expect.arrayContaining(['CA_TTC', 'MARGE_BRUTE']));
    expect(telechargement.downloadFromObservable).toHaveBeenCalledWith(expect.anything(), 'pilotage-2026-10-01-2026-10-31', 'csv');
  });

  it('alertes : repliées par défaut, le choix est retenu', () => {
    const element = fixture.nativeElement as HTMLElement;
    expect(element.querySelector('.bandeau-alerte')).toBeNull();

    element.querySelector<HTMLButtonElement>('.bandeau-alertes-entete button')!.click();
    fixture.detectChanges();

    expect(element.querySelectorAll('.bandeau-alerte').length).toBe(2);
    expect(localStorage.getItem('pilotage.alertes.ouvert')).toBe('true');
  });

  it('alertes : gravité écrite, chacune mène à son onglet', () => {
    (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('.bandeau-alertes-entete button')!.click();
    fixture.detectChanges();
    const liens = Array.from((fixture.nativeElement as HTMLElement).querySelectorAll<HTMLAnchorElement>('.bandeau-alerte a'));

    expect(liens.map(lien => lien.textContent?.replace(/\s+/g, ' ').trim())).toEqual([
      "Grave : Chute d'activité. Le CA des 7 derniers jours atteint 85 %.",
      'À surveiller : Un organisme tarde à payer. MUGEF-CI (120 j).',
    ]);
    expect(liens[1].getAttribute('href')).toContain('onglet=tresorerie-tiers-payant');
  });

  it('tuile CA : objectif de la période et fin de mois projetée, méthode en infobulle', () => {
    const element = fixture.nativeElement as HTMLElement;
    const texte = element.textContent?.replace(/\s+/g, ' ') ?? '';

    expect(texte).toContain('Objectif : ');
    expect(texte).toContain('Fin de mois projetée : ');
    expect(texte).toContain("de l'objectif)");
    expect(element.querySelector('[aria-label^="Aide : D\'après octobre 2025"]')).not.toBeNull();
    expect(element.querySelector('app-kpi-item .kpi-strip-info')?.getAttribute('aria-label')).toMatch(/^Explication : /);
    expect(element.querySelector('app-kpi-item[title]')).toBeNull();
  });

  it("sans droit d'export, pas de bouton", () => {
    droitExport.set(false);
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).querySelector('app-button')).toBeNull();
    droitExport.set(true);
  });
});

