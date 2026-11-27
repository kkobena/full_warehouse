import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideTranslateService } from '@ngx-translate/core';
import { AbilityService } from 'app/core/auth/ability.service';
import { JournalRemisesFiltresStore } from '../../data-access/journal-remises-filtres.store';
import { ILigneJournal, ISyntheseJournal, NatureJournal } from '../../models/journal-remises.model';
import { JournalRemisesLignesComponent } from './journal-lignes.component';

const SYNTHESE: ISyntheseJournal = {
  du: '2026-11-01',
  au: '2026-11-27',
  remiseTotale: 580,
  promotions: { lignes: 1, tickets: 1, unites: 2, montantBrut: 2360, remise: 380, montantNet: 1780 },
  remisesProduit: { lignes: 1, tickets: 1, unites: 2, montantBrut: 2360, remise: 200, montantNet: 1780 },
  ug: {
    lignes: 1,
    tickets: 1,
    unites: 3,
    valeur: 3540,
    remise: 0,
    htNet: 3000,
    coutEvite: 2124,
    marge: 3000,
    margeComptabilisee: 2920,
    margeReelle: 5044,
  },
  parPromotion: [],
  parGrille: [],
};

const LIGNE = {
  date: '2026-11-03',
  heure: '2026-11-03T10:00:00',
  ticket: '42',
  natureVente: 'COMPTANT',
  cip: '3400930000001',
  produit: 'Doliprane 1000',
  quantite: 10,
  montantBrut: 11800,
  partPromotion: 380,
  partRemiseProduit: 200,
  origine: 'CUMUL',
  promotionLibelle: 'Novembre -20 %',
  quantiteUg: 3,
  valeurUg: 3540,
  remiseUg: 0,
  margeUg: 3000,
  coutEviteUg: 2124,
  margeReelle: 5044,
} as ILigneJournal;

describe('Journal des remises — journal ligne à ligne', () => {
  let fixture: ComponentFixture<JournalRemisesLignesComponent>;
  let http: HttpTestingController;

  function repondre(lignes: ILigneJournal[]): void {
    http.expectOne(r => r.url.endsWith('/journal-remises/lignes')).flush(lignes, { headers: { 'X-Total-Count': String(lignes.length) } });
    http.expectOne(r => r.url.endsWith('/journal-remises/synthese')).flush(SYNTHESE);
    fixture.detectChanges();
  }

  function creer(nature: NatureJournal, peutExporter = true): HTMLElement {
    TestBed.inject(AbilityService).setFromNavTree([
      {
        id: 1,
        code: 'journal-remises.ug',
        libelle: 'UG',
        targetType: 'SECTION',
        ordre: 1,
        permissions: {
          canDisplay: true,
          canAccess: true,
          canCreate: false,
          canEdit: false,
          canDelete: false,
          canExport: peutExporter,
          canExecute: false,
        },
      },
    ]);
    fixture = TestBed.createComponent(JournalRemisesLignesComponent);
    fixture.componentRef.setInput('nature', nature);
    fixture.componentRef.setInput('navCode', 'journal-remises.ug');
    fixture.detectChanges();
    repondre([LIGNE]);
    return fixture.nativeElement as HTMLElement;
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [JournalRemisesLignesComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideTranslateService(), AbilityService, JournalRemisesFiltresStore],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it("montre les UG, leur marge et le coût d'achat évité", () => {
    const element = creer('UG');
    expect(element.textContent).toContain('Marge UG');
    expect(element.textContent).toContain('Coût évité');
    expect(element.querySelector('tbody')?.textContent).toContain('Doliprane 1000');
    // L'origine d'une remise n'a pas de sens pour les UG.
    expect(element.querySelector('[aria-label="Origine de la remise"]')).toBeNull();
  });

  it('ne compte, au journal des remises produit, que la part de la grille', () => {
    const element = creer('REMISES');
    const cellules = Array.from(element.querySelectorAll('tbody td')).map(td => td.textContent?.trim());
    expect(cellules).toContain('200');
    expect(cellules).not.toContain('380');
    expect(cellules).toContain('Promotion et remise produit');
  });

  it('recharge à « Afficher », avec les filtres saisis', () => {
    creer('PROMOTIONS');
    const store = TestBed.inject(JournalRemisesFiltresStore);
    store.natureVente.set('ASSURANCE');
    store.appliquer();
    fixture.detectChanges();

    const page = http.expectOne(r => r.url.endsWith('/journal-remises/lignes'));
    expect(page.request.params.getAll('naturesVente')).toEqual(['ASSURANCE']);
    expect(page.request.params.get('page')).toBe('0');
    expect(page.request.params.get('nature')).toBe('PROMOTIONS');
    page.flush([], { headers: { 'X-Total-Count': '0' } });
    http.expectOne(r => r.url.endsWith('/journal-remises/synthese')).flush(SYNTHESE);
  });

  it('demande au serveur la page choisie', () => {
    creer('UG');
    fixture.componentInstance['onLazyLoad']({ first: 50, rows: 50 });

    const page = http.expectOne(r => r.url.endsWith('/journal-remises/lignes'));
    expect(page.request.params.get('page')).toBe('1');
    expect(page.request.params.get('size')).toBe('50');
    page.flush([], { headers: { 'X-Total-Count': '60' } });
  });

  it("n'offre pas l'export sans le droit", () => {
    const element = creer('UG', false);
    expect(element.textContent).not.toContain('Exporter');
  });
});
