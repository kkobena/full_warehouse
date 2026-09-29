import {ComponentFixture, TestBed} from '@angular/core/testing';
import {provideHttpClient} from '@angular/common/http';
import {HttpTestingController, provideHttpClientTesting} from '@angular/common/http/testing';
import {Router} from '@angular/router';
import {EcartsARegulariserComponent} from './ecarts-a-regulariser.component';
import {IEcartStock} from '../../models';

/** Lot 4 de PLAN-VENTE-SUR-STOCK-ERRONE : la liste des écarts machine/physique. */
describe('EcartsARegulariserComponent', () => {
  let fixture: ComponentFixture<EcartsARegulariserComponent>;
  let httpMock: HttpTestingController;
  const router = {navigate: jest.fn()};

  const ecarts: IEcartStock[] = [
    {produitId: 1, libelle: 'SPASFON', codeCip: '3400001', stock: -9, quantiteDue: 2, ecart: 7},
    {produitId: 2, libelle: 'SMECTA', codeCip: null, stock: -1, quantiteDue: 0, ecart: 1},
  ];

  function ouvrir(): HTMLElement {
    fixture = TestBed.createComponent(EcartsARegulariserComponent);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  const requete = () => httpMock.expectOne(r => r.method === 'GET' && r.url.endsWith('api/ajustements/ecarts-a-regulariser'));

  beforeEach(() => {
    jest.clearAllMocks();
    TestBed.configureTestingModule({
      imports: [EcartsARegulariserComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), {provide: Router, useValue: router}],
    });
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('affiche chaque produit avec son stock, sa dette et son écart', () => {
    const el = ouvrir();
    requete().flush(ecarts);
    fixture.detectChanges();

    const ligne = el.querySelector('[data-produit="1"]')!;
    const cellules = Array.from(ligne.querySelectorAll('td')).map(td => td.textContent!.trim());
    expect(cellules[0]).toBe('3400001');
    expect(cellules[1]).toBe('SPASFON');
    expect(cellules[2]).toContain('9');
    expect(cellules[3]).toBe('2');
    expect(cellules[4]).toBe('7');
    expect(el.querySelector('[data-produit="2"] td')!.textContent!.trim()).toBe('–');
  });

  it('résume le nombre de produits et les unités non expliquées', () => {
    const el = ouvrir();
    requete().flush(ecarts);
    fixture.detectChanges();

    expect(el.textContent).toContain('2 produit(s), 8 unité(s) non expliquée(s)');
  });

  it("rassure quand aucun négatif ne dépasse les avoirs", () => {
    const el = ouvrir();
    requete().flush([]);
    fixture.detectChanges();

    expect(el.textContent).toContain('Aucun écart');
  });

  it("signale l'échec du chargement", () => {
    const el = ouvrir();
    requete().flush({}, {status: 500, statusText: 'Erreur'});
    fixture.detectChanges();

    expect(el.textContent).toContain('Impossible de charger les écarts');
  });

  it('mène au nouvel ajustement', () => {
    const el = ouvrir();
    requete().flush(ecarts);
    fixture.detectChanges();

    const bouton = Array.from(el.querySelectorAll('button')).find(b => b.textContent!.includes('Nouvel ajustement'))!;
    bouton.click();

    expect(router.navigate).toHaveBeenCalledWith(['/features-ajustement/new']);
  });
});
