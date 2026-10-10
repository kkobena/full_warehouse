import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, Router } from '@angular/router';
import { BehaviorSubject, of } from 'rxjs';

import { AbilityService } from 'app/core/auth/ability.service';
import { BlobDownloadService } from 'app/shared/services/blob-download.service';
import { PilotageApiService } from '../../data-access/pilotage-api.service';
import { CelluleAnalyse, ComparaisonAnnees, RequetePilotage } from '../../models/pilotage.model';
import { OngletComparerAnneesComponent } from './onglet-comparer-annees.component';
import { lireReglageAnnees, REGLAGE_ANNEES_PAR_DEFAUT, versParametresAnnees } from './reglage-annees';

const REQUETE: RequetePilotage = {
  predefinie: 'MOIS_EN_COURS',
  du: '2026-03-01',
  au: '2026-03-31',
  comparaison: 'MEME_PERIODE_N_1',
  anneesEnArriere: 1,
  aDate: true,
  granularite: 'MOIS',
};

const cellule = (valeur: number | null): CelluleAnalyse => ({ valeur, valeurReference: null, ecart: null, ecartPct: null });
const douzeMois = (valeur: number, moisEcoules = 12): CelluleAnalyse[] =>
  Array.from({ length: 12 }, (_, rang) => cellule(rang < moisEcoules ? valeur : null));

const COMPARAISON: ComparaisonAnnees = {
  indicateur: { code: 'CA_TTC', libelle: 'CA TTC', definition: '', unite: 'MONTANT', sensFavorable: 'HAUSSE', droit: 'PAGE' },
  mode: 'MENSUEL',
  parJourOuvre: false,
  jusquAu: '2026-03-15',
  annees: [
    { annee: 2025, complete: true, mois: douzeMois(1_100), trimestres: [], total: cellule(13_200) },
    { annee: 2026, complete: false, mois: douzeMois(1_200, 3), trimestres: [], total: cellule(3_600) },
  ],
  saisonnalite: [],
  croissanceAnnuelleMoyenne: 6.8,
  croissanceDepuis: 2021,
  croissanceJusqua: 2025,
  moisMaximum: { annee: 2025, mois: 12, valeur: 2_200_000 },
  synthese: [
    { annee: 2025, complete: true, caTtc: 2_000_000, margeBrute: null, tauxMarge: null, nbVentes: 900, panierMoyen: 2_222, remises: 10_000, croissanceCa: 4.2 },
    { annee: 2026, complete: false, caTtc: 1_500_000, margeBrute: null, tauxMarge: null, nbVentes: 700, panierMoyen: 2_142, remises: 9_000, croissanceCa: -3.1 },
  ],
  parFamille: null,
  parNatureVente: null,
};

describe('Onglet « Comparer les années »', () => {
  let fixture: ComponentFixture<OngletComparerAnneesComponent>;
  const router = { navigate: jest.fn() };
  const api = { listerIndicateurs: () => of([COMPARAISON.indicateur]), comparerAnnees: jest.fn(() => of(COMPARAISON)) };

  beforeEach(async () => {
    router.navigate.mockReset();
    api.comparerAnnees.mockClear();
    await TestBed.configureTestingModule({
      imports: [OngletComparerAnneesComponent],
      providers: [
        { provide: PilotageApiService, useValue: api },
        { provide: ActivatedRoute, useValue: { queryParamMap: new BehaviorSubject(convertToParamMap({ annNb: '2' })) } },
        { provide: Router, useValue: router },
        { provide: BlobDownloadService, useValue: { downloadFromObservable: jest.fn() } },
        { provide: AbilityService, useValue: { canSignal: () => signal(true) } },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(OngletComparerAnneesComponent);
    fixture.componentRef.setInput('requete', REQUETE);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  });

  it('compare les années du réglage, « à date » venant de la barre', () => {
    expect(api.comparerAnnees).toHaveBeenCalledWith(true, expect.objectContaining({ annees: 2, indicateur: 'CA_TTC' }));
  });

  it('synthèse : la croissance suit les couleurs des variations, sens écrit pour les lecteurs d’écran', () => {
    const variations = Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('app-card app-variation .variation'));

    expect(variations.map(v => v.className)).toEqual(['variation variation--favorable', 'variation variation--defavorable']);
    expect(variations.map(v => v.textContent?.replace(/\s+/g, ' ').trim())).toEqual(['▲en hausse de 4,2 %', '▼en baisse de 3,1 %']);
  });

  it('dit le mois le plus élevé et la croissance annuelle moyenne', () => {
    const texte = (fixture.nativeElement as HTMLElement).textContent;
    expect(texte).toContain('Mois le plus élevé : décembre 2025 (2,2 M).');
    expect(texte).toContain('Croissance annuelle moyenne 2021-2025 : +6,8 %.');
  });

  it('marque le mois en cours, incomplet pour l’année en cours', () => {
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Mars (au 15)');
  });

  it('filtrer sur une famille passe par l’URL', () => {
    fixture.componentInstance['filtrerFamille']({ cle: '12', libelle: 'Antalgiques', cellules: [] });

    expect(router.navigate.mock.calls.at(-1)[1].queryParams).toEqual(expect.objectContaining({ annF: 'FAMILLE:12:Antalgiques', annNb: 2 }));
  });
});

describe("Réglage de « Comparer les années » dans l'URL", () => {
  it('aller-retour', () => {
    const reglage = { indicateur: 'MARGE_BRUTE', annees: 5, mode: 'GLISSANT' as const, parJourOuvre: true, filtre: { axe: 'FAMILLE' as const, cle: '3', libelle: 'Vitamines' } };
    expect(lireReglageAnnees(convertToParamMap(versParametresAnnees(reglage)))).toEqual(reglage);
  });

  it('par défaut : CA TTC sur trois ans, mois par mois', () => {
    expect(lireReglageAnnees(convertToParamMap({}))).toEqual(REGLAGE_ANNEES_PAR_DEFAUT);
  });
});
