import {TestBed} from '@angular/core/testing';
import {InventoryStore} from './inventory.store';
import {IInventoryLine} from '../../models';

describe('InventoryStore', () => {
  let store: InstanceType<typeof InventoryStore>;

  const line = (id: number, extra: Partial<IInventoryLine> = {}): IInventoryLine => ({
    id,
    produitLibelle: `Produit ${id}`,
    quantityInit: 10,
    quantityOnHand: 10,
    updated: false,
    ...extra,
  });

  beforeEach(() => {
    TestBed.configureTestingModule({providers: [InventoryStore]});
    store = TestBed.inject(InventoryStore);
  });

  describe('lignes', () => {
    it('expose les lignes et le total renvoyés par le serveur', () => {
      store.setLines([line(1), line(2)], 42);

      expect(store.lines().length).toBe(2);
      expect(store.totalLines()).toBe(42);
    });

    it('remplace une ligne par sa version sauvegardée sans toucher aux autres', () => {
      store.setLines([line(1), line(2)], 2);

      store.updateLine(line(2, {quantityOnHand: 7, updated: true}));

      expect(store.lines()[0].quantityOnHand).toBe(10);
      expect(store.lines()[1].quantityOnHand).toBe(7);
      expect(store.lines()[1].updated).toBe(true);
    });
  });

  describe('saisies en attente', () => {
    it("n'a aucune saisie en attente à l'état initial", () => {
      expect(store.hasPendingEdits()).toBe(false);
      expect(store.pendingEditCount()).toBe(0);
    });

    it('accumule les saisies par ligne', () => {
      store.addPendingEdit(1, 5);
      store.addPendingEdit(2, 8);

      expect(store.pendingEditCount()).toBe(2);
      expect(store.hasPendingEdits()).toBe(true);
      expect(store.pendingEdits()[1]).toBe(5);
    });

    it('écrase la saisie précédente de la même ligne', () => {
      store.addPendingEdit(1, 5);
      store.addPendingEdit(1, 9);

      expect(store.pendingEditCount()).toBe(1);
      expect(store.pendingEdits()[1]).toBe(9);
    });

    it('vide les saisies après enregistrement', () => {
      store.addPendingEdit(1, 5);

      store.clearPendingEdits();

      expect(store.hasPendingEdits()).toBe(false);
    });
  });

  describe('progression', () => {
    it('expose le pourcentage fourni par le serveur', () => {
      store.setProgress({
        inventoryId: 1,
        totalLines: 200,
        updatedLines: 50,
        linesWithGap: 3,
        progressPercent: 25,
      });

      expect(store.progressPercent()).toBe(25);
    });
  });

  describe("bus d'évènements", () => {
    it('incrémente la séquence pour rendre deux évènements identiques distinguables', () => {
      store.emitEvent('LINES_LOADED');
      const first = store.lastEvent();

      store.emitEvent('LINES_LOADED');
      const second = store.lastEvent();

      expect(first?.type).toBe('LINES_LOADED');
      expect(second?.seq).toBeGreaterThan(first!.seq);
    });

    it('transporte la charge utile de l\'évènement', () => {
      store.emitEvent('BATCH_SAVED', {saved: 3, failed: 0, failedIds: [], conflictedIds: [7]});

      expect(store.lastEvent()?.payload.conflictedIds).toEqual([7]);
    });
  });
});
