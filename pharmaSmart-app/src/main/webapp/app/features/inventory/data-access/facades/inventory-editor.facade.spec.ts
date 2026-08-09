import {TestBed} from '@angular/core/testing';
import {provideHttpClient} from '@angular/common/http';
import {HttpTestingController, provideHttpClientTesting} from '@angular/common/http/testing';
import {provideTranslateService} from '@ngx-translate/core';
import {InventoryEditorFacade} from './inventory-editor.facade';
import {InventoryStore} from '../store/inventory.store';
import {IInventoryLine} from '../../models';

describe('InventoryEditorFacade', () => {
  let facade: InventoryEditorFacade;
  let store: InstanceType<typeof InventoryStore>;
  let httpMock: HttpTestingController;

  const INVENTORY_ID = 12;

  beforeEach(() => {
    TestBed.configureTestingModule({
      // `ErrorService` injecte `TranslateService` : sans ce fournisseur, toute la suite
      // tombe au montage du TestBed, avant même d'exécuter le moindre test.
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideTranslateService(),
        InventoryStore,
        InventoryEditorFacade,
      ],
    });

    facade = TestBed.inject(InventoryEditorFacade);
    store = TestBed.inject(InventoryStore);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('loadLines', () => {
    it('alimente le store avec les lignes et le total paginé', () => {
      facade.loadLines(INVENTORY_ID, {page: 0, size: 20});

      const req = httpMock.expectOne(r => r.url === 'api/store-inventory-lines/v2');
      expect(req.request.params.get('storeInventoryId')).toBe(String(INVENTORY_ID));

      req.flush([{id: 1}, {id: 2}] as IInventoryLine[], {headers: {'X-Total-Count': '57'}});

      expect(store.lines().length).toBe(2);
      expect(store.totalLines()).toBe(57);
      expect(store.loadingLines()).toBe(false);
    });

    it('signale une erreur sans laisser le chargement bloqué', () => {
      facade.loadLines(INVENTORY_ID, {});

      httpMock
        .expectOne(r => r.url === 'api/store-inventory-lines/v2')
        .flush('boom', {status: 500, statusText: 'Server Error'});

      expect(store.loadingLines()).toBe(false);
      expect(store.error()).toBeTruthy();
    });
  });

  describe('saveBatch — verrou optimiste', () => {
    it('transporte la version lue de chaque ligne', () => {
      // La version vient des lignes chargées : c'est elle que le serveur compare
      store.setLines(
        [
          {id: 1, version: 4} as IInventoryLine,
          {id: 2, version: 9} as IInventoryLine,
        ],
        2,
      );
      store.addPendingEdit(1, 15);
      store.addPendingEdit(2, 3);

      facade.saveBatch(INVENTORY_ID);

      const req = httpMock.expectOne(r => r.url === 'api/store-inventory-lines/batch');
      expect(req.request.body).toEqual([
        {id: 1, quantityOnHand: 15, version: 4},
        {id: 2, quantityOnHand: 3, version: 9},
      ]);

      req.flush({saved: 2, failed: 0, failedIds: [], conflictedIds: []});
      httpMock.expectOne(r => r.url === `api/store-inventories/${INVENTORY_ID}/progress`).flush(null);
    });

    it('envoie une version indéfinie quand la ligne n\'est pas chargée', () => {
      store.setLines([], 0);
      store.addPendingEdit(99, 1);

      facade.saveBatch(INVENTORY_ID);

      const req = httpMock.expectOne(r => r.url === 'api/store-inventory-lines/batch');
      expect(req.request.body[0].version).toBeUndefined();

      req.flush({saved: 1, failed: 0, failedIds: [], conflictedIds: []});
      httpMock.expectOne(r => r.url === `api/store-inventories/${INVENTORY_ID}/progress`).flush(null);
    });

    it('vide les saisies en attente et remonte les conflits', () => {
      store.setLines([{id: 1, version: 1} as IInventoryLine], 1);
      store.addPendingEdit(1, 5);

      facade.saveBatch(INVENTORY_ID);

      httpMock
        .expectOne(r => r.url === 'api/store-inventory-lines/batch')
        .flush({saved: 0, failed: 1, failedIds: [], conflictedIds: [1]});
      httpMock.expectOne(r => r.url === `api/store-inventories/${INVENTORY_ID}/progress`).flush(null);

      expect(store.hasPendingEdits()).toBe(false);
      expect(store.lastEvent()?.type).toBe('PROGRESS_UPDATED');
    });

    it("n'appelle pas le serveur sans saisie en attente", () => {
      facade.saveBatch(INVENTORY_ID);

      httpMock.expectNone(r => r.url === 'api/store-inventory-lines/batch');
    });
  });

  describe('saveLine', () => {
    it('émet LINE_SAVE_ERROR avec le statut 409 en cas de comptage concurrent', () => {
      facade.saveLine({id: 1, version: 2} as IInventoryLine).subscribe({error: () => undefined});

      httpMock
        .expectOne(r => r.url === 'api/store-inventory-lines')
        .flush({detail: 'conflit'}, {status: 409, statusText: 'Conflict'});

      const event = store.lastEvent();
      expect(event?.type).toBe('LINE_SAVE_ERROR');
      expect(event?.payload.error.status).toBe(409);
      expect(store.error()).toContain('autre opérateur');
    });

    it('propage l\'erreur à l\'appelant pour qu\'il invalide la saisie', () => {
      let failed = false;
      facade.saveLine({id: 1} as IInventoryLine).subscribe({error: () => (failed = true)});

      httpMock
        .expectOne(r => r.url === 'api/store-inventory-lines')
        .flush(null, {status: 0, statusText: 'Backend indisponible'});

      expect(failed).toBe(true);
      // La ligne est signalée non persistée, et n'est pas comptée dans la progression locale
      expect(store.lines().find(l => l.id === 1)?.saveFailed).toBeUndefined();
      expect(facade.consumeLocalCounts()).toBe(0);
    });

    it('ne souscrit pas tant que l\'appelant ne le fait pas', () => {
      facade.saveLine({id: 1} as IInventoryLine);

      httpMock.expectNone(r => r.url === 'api/store-inventory-lines');
    });
  });

  describe('refreshProgress', () => {
    it('alimente le store et notifie la progression', () => {
      facade.refreshProgress(INVENTORY_ID);

      httpMock
        .expectOne(r => r.url === `api/store-inventories/${INVENTORY_ID}/progress`)
        .flush({inventoryId: INVENTORY_ID, totalLines: 100, updatedLines: 30, linesWithGap: 2, progressPercent: 30});

      expect(store.progressPercent()).toBe(30);
      expect(store.lastEvent()?.type).toBe('PROGRESS_UPDATED');
    });

    it('reste silencieux en cas d\'échec réseau', () => {
      facade.refreshProgress(INVENTORY_ID);

      httpMock
        .expectOne(r => r.url === `api/store-inventories/${INVENTORY_ID}/progress`)
        .flush('down', {status: 503, statusText: 'Unavailable'});

      expect(store.error()).toBeNull();
    });
  });
});
