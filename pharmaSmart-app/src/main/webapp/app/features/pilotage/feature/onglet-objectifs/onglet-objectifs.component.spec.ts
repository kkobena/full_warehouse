import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { PilotageApiService } from '../../data-access/pilotage-api.service';
import { GrilleObjectifs, Indicateur, SuiviMois, SuiviObjectifs } from '../../models/pilotage.model';
import { SaisieObjectifsComponent } from './saisie-objectifs.component';
import { SuiviObjectifsComponent } from './suivi-objectifs.component';

const CA = { code: 'CA_TTC', libelle: "Chiffre d'affaires TTC", definition: '', unite: 'MONTANT', sensFavorable: 'HAUSSE', droit: 'pilotage' } as Indicateur;
const mois = (numero: number, objectif: number | null, realise: number | null, tenu: boolean | null, enCours = false): SuiviMois => ({
  mois: numero,
  objectif,
  realise,
  ecart: objectif !== null && realise !== null ? realise - objectif : null,
  atteinte: objectif && realise !== null ? (realise * 100) / objectif : null,
  tenu,
  enCours,
});
const SUIVI: SuiviObjectifs = {
  annee: 2026,
  jusquAu: '2026-10-10',
  indicateurs: [
    {
      indicateur: CA,
      mois: [mois(1, 900, 1000, true), mois(2, 1200, 1000, false), ...Array.from({ length: 10 }, (_, rang) => mois(rang + 3, null, null, null, rang + 3 === 10))],
      cumul: mois(2, 2100, 2000, false),
      projection: { realiseADate: 1000, projection: 3333, objectif: 5000, atteinteProjetee: 66.7, tenu: false, methode: "D'après octobre 2025 : au même jour, 30 % du mois était fait." },
    },
  ],
};
const GRILLE: GrilleObjectifs = { annee: 2026, moisClos: 9, lignes: [{ indicateur: CA, mois: Array(12).fill(null), modifiePar: null, modifieLe: null }] };

describe('Onglet « Objectifs »', () => {
  const api = {
    lireObjectifs: jest.fn(() => of(GRILLE)),
    proposerObjectifs: jest.fn(() => of(Array(12).fill(1050))),
    enregistrerObjectifs: jest.fn(() => of(GRILLE)),
  };

  beforeEach(async () => {
    jest.clearAllMocks();
    await TestBed.configureTestingModule({ providers: [{ provide: PilotageApiService, useValue: api }] }).compileComponents();
  });

  it('suivi : projection du mois dite en clair, mois tenus et manqués écrits en toutes lettres, cumul des mois clos', () => {
    const fixture = TestBed.createComponent(SuiviObjectifsComponent);
    fixture.componentRef.setInput('suivi', SUIVI);
    fixture.detectChanges();
    const texte = (fixture.nativeElement as HTMLElement).textContent ?? '';

    expect(texte).toContain("Au 10/10/2026, chiffre d'affaires TTC projeté à 67 % de l'objectif du mois.");
    expect(texte).toContain('✗ menacé');
    expect(texte).toContain('Tenu');
    expect(texte).toContain('Manqué');
    expect(texte).toContain('Oct. (à date)');
    expect(texte).toContain('Cumul à fin Févr.');
  });

  it('saisie : « Proposer » remplit la ligne, « Enregistrer » envoie les 12 mois et prévient l’onglet', async () => {
    const fixture = TestBed.createComponent(SaisieObjectifsComponent);
    fixture.componentRef.setInput('annee', 2026);
    const enregistre = jest.fn();
    fixture.componentInstance.enregistre.subscribe(enregistre);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    const ligne = GRILLE.lignes[0];

    fixture.componentInstance['proposer'](ligne);
    expect(api.proposerObjectifs).toHaveBeenCalledWith(2026, 'CA_TTC', 5);
    expect(fixture.componentInstance['estModifiee'](ligne)).toBe(true);

    fixture.componentInstance['enregistrer'](ligne);
    // Les 9 mois clos gardent leur valeur (vide) : seule la suite de l'année est proposée.
    expect(api.enregistrerObjectifs).toHaveBeenCalledWith({ annee: 2026, indicateur: 'CA_TTC', mois: [...Array(9).fill(null), 1050, 1050, 1050] });
    expect(enregistre).toHaveBeenCalled();
    expect(fixture.componentInstance['estModifiee'](ligne)).toBe(false);
  });

  it('saisie : les mois clos sont en lecture seule, une année close n’a ni « Proposer » ni « Enregistrer »', async () => {
    const fixture = TestBed.createComponent(SaisieObjectifsComponent);
    fixture.componentRef.setInput('annee', 2026);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    const champs = Array.from((fixture.nativeElement as HTMLElement).querySelectorAll<HTMLInputElement>('.objectifs-grille input'));

    expect(champs.map(champ => champ.disabled)).toEqual([...Array(9).fill(true), false, false, false]);

    api.lireObjectifs.mockReturnValueOnce(of({ ...GRILLE, annee: 2025, moisClos: 12 }));
    fixture.componentRef.setInput('annee', 2025);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    const texte = (fixture.nativeElement as HTMLElement).textContent ?? '';

    expect(texte).toContain('Année close');
    expect(texte).not.toContain('Proposer');
    expect(texte).not.toContain('Enregistrer');
  });
});
