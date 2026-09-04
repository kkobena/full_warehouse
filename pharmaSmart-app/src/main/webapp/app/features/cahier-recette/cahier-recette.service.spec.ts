import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { SERVER_API_URL } from 'app/app.constants';
import { CahierRecetteService, CheminsMenu, IndexCaptures } from './cahier-recette.service';

describe('CahierRecetteService', () => {
  let service: CahierRecetteService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(CahierRecetteService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('demande le manuel complet sans filtre de module', () => {
    service.downloadPdf().subscribe();

    const request = httpMock.expectOne(`${SERVER_API_URL}api/cahier-recette/pdf`);
    expect(request.request.params.has('modules')).toBe(false);
    expect(request.request.responseType).toBe('blob');
    request.flush(new Blob([], { type: 'application/pdf' }));
  });

  it('transmet les modules sélectionnés à l API', () => {
    service.downloadPdf(['VTE', 'FAC']).subscribe();

    const request = httpMock.expectOne(
      req => req.url === `${SERVER_API_URL}api/cahier-recette/pdf` && req.params.get('modules') === 'VTE,FAC',
    );
    expect(request.request.responseType).toBe('blob');
    request.flush(new Blob([], { type: 'application/pdf' }));
  });

  it('transmet les scénarios du groupe de parcours liés', () => {
    service.downloadPdf(['STK'], ['STK-04', 'STK-05', 'STK-07']).subscribe();

    const request = httpMock.expectOne(
      req =>
        req.url === `${SERVER_API_URL}api/cahier-recette/pdf` &&
        req.params.get('modules') === 'STK' &&
        req.params.get('scenarios') === 'STK-04,STK-05,STK-07',
    );
    request.flush(new Blob([], { type: 'application/pdf' }));
  });

  it("lit l'index des écrans à côté des images", () => {
    let index: IndexCaptures | undefined;
    service.loadCaptures().subscribe(recu => (index = recu));

    httpMock.expectOne('content/captures/index.json').flush({
      'VTE-01': [{ ordre: 1, fichier: 'content/captures/VTE-01/etape-1.jpg' }],
    });

    expect(index?.['VTE-01']).toHaveLength(1);
  });

  it("traite l'absence d'index comme un guide sans illustration, pas comme une erreur", () => {
    let index: IndexCaptures | undefined;
    service.loadCaptures().subscribe(recu => (index = recu));

    httpMock.expectOne('content/captures/index.json').flush('', { status: 404, statusText: 'Not Found' });

    expect(index).toEqual({});
  });

  it('lit les chemins de menu depuis le backend plutôt que de les coder en dur', () => {
    let chemins: CheminsMenu | undefined;
    service.loadNavPaths().subscribe(recu => (chemins = recu));

    httpMock.expectOne(`${SERVER_API_URL}api/nav/paths`).flush({
      'ventes.devis': 'Barre de navigation ▸ Gestion Courante ▸ Ventes ▸ Proformas',
    });

    expect(chemins?.['ventes.devis']).toBe('Barre de navigation ▸ Gestion Courante ▸ Ventes ▸ Proformas');
  });

  it('prive le guide de ses chemins sans le mettre en erreur si la navigation est injoignable', () => {
    let chemins: CheminsMenu | undefined;
    service.loadNavPaths().subscribe(recu => (chemins = recu));

    httpMock
      .expectOne(`${SERVER_API_URL}api/nav/paths`)
      .flush('', { status: 500, statusText: 'Server Error' });

    expect(chemins).toEqual({});
  });
});

