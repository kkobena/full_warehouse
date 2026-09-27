import {TestBed} from '@angular/core/testing';
import {provideHttpClient} from '@angular/common/http';
import {HttpTestingController, provideHttpClientTesting} from '@angular/common/http/testing';
import {SalePaymentFacade} from './sale-payment.facade';
import {SalesStore} from '../store/sales.store';
import {PrintService} from '../services/print.service';
import {NotificationService} from '../../../../shared/services/notification.service';
import {ISales} from '../../../../shared/model/sales.model';
import {SalesStatut} from '../../../../shared/model';

/**
 * Verrou optimiste, volet client : un 409 à l'encaissement signifie que la vente a été écrite
 * ailleurs depuis son affichage. L'écran doit alors montrer l'état réel relu sur le serveur, sans
 * rien fusionner avec ce que le poste affichait.
 */
describe('SalePaymentFacade — conflit de version', () => {
  let facade: SalePaymentFacade;
  let store: InstanceType<typeof SalesStore>;
  let httpMock: HttpTestingController;
  const notification = {error: jest.fn(), warning: jest.fn(), success: jest.fn()};

  const saleId = {id: 7, saleDate: '2026-09-27'};
  const venteAffichee: ISales = {id: 7, saleId, version: 2, statut: SalesStatut.ACTIVE, salesAmount: 1000};
  const conflit = {status: 409, statusText: 'Conflict'};
  const corpsConflit = {message: 'Cette vente a été modifiée par une autre opération', errorKey: 'sale.concurrent.modification'};

  beforeEach(() => {
    jest.clearAllMocks();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        {provide: NotificationService, useValue: notification},
        {provide: PrintService, useValue: {printInvoice: jest.fn(), printReceipt: jest.fn()}},
      ],
    });
    facade = TestBed.inject(SalePaymentFacade);
    store = TestBed.inject(SalesStore);
    httpMock = TestBed.inject(HttpTestingController);
    store.setSaleType('COMPTANT');
    store.setCurrentSale({...venteAffichee});
  });

  afterEach(() => httpMock.verify());

  it("renvoie la version affichée à l'encaissement", () => {
    facade.saveSale().subscribe();

    const req = httpMock.expectOne(r => r.url.endsWith('api/sales/comptant/save'));
    expect(req.request.body.version).toBe(2);
    req.flush({saleId, success: true});
  });

  it('recharge la vente depuis le serveur après un 409, sans fusion', () => {
    facade.saveSale().subscribe();
    httpMock.expectOne(r => r.url.endsWith('api/sales/comptant/save')).flush(corpsConflit, conflit);

    httpMock
      .expectOne(r => r.method === 'GET' && r.url.endsWith(`api/sales/${saleId.id}/${saleId.saleDate}`))
      .flush({...venteAffichee, version: 3, salesAmount: 2500});

    expect(store.currentSale()?.version).toBe(3);
    expect(store.currentSale()?.salesAmount).toBe(2500);
    expect(notification.error).toHaveBeenCalledWith(corpsConflit.message);
  });

  it("retire de l'écran une vente déjà encaissée sur un autre poste", () => {
    facade.saveSale().subscribe();
    httpMock.expectOne(r => r.url.endsWith('api/sales/comptant/save')).flush(corpsConflit, conflit);

    httpMock
      .expectOne(r => r.method === 'GET' && r.url.endsWith(`api/sales/${saleId.id}/${saleId.saleDate}`))
      .flush({...venteAffichee, version: 3, statut: SalesStatut.CLOSED});

    expect(store.currentSale()).toBeNull();
    expect(notification.warning).toHaveBeenCalled();
  });

  it("ne recharge rien sur une autre erreur que le 409", () => {
    facade.saveSale().subscribe();
    httpMock.expectOne(r => r.url.endsWith('api/sales/comptant/save')).flush({message: 'Règlement insuffisant'}, {status: 400, statusText: 'Bad Request'});

    httpMock.expectNone(r => r.method === 'GET');
    expect(store.currentSale()?.version).toBe(2);
  });
});
