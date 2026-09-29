import {NgZone, signal} from '@angular/core';
import {TestBed} from '@angular/core/testing';
import {NgbModal} from '@ng-bootstrap/ng-bootstrap';
import {ISales, ISalesLine} from '../../../../shared/model';
import {ForceStockChoiceModalComponent} from '../../ui/force-stock-choice-modal/force-stock-choice-modal.component';
import {createForceStockHandling, ForceStockContext, StockErrorDetails} from './force-stock.mixin';

/** Lot 3 de PLAN-VENTE-SUR-STOCK-ERRONE : du choix du caissier à la ligne envoyée au serveur. */
describe('createForceStockHandling — choix du motif de forçage', () => {
  let modalResult: Promise<unknown>;
  let instance: Partial<ForceStockChoiceModalComponent>;
  const modalService = {open: jest.fn()};
  const zone = {run: (fn: () => unknown) => fn()} as unknown as NgZone;
  const authorizationService = {canForceStock: jest.fn(), canRegulariserStock: jest.fn()};
  const facade = {
    errorDetails: signal<StockErrorDetails | null>(null),
    clearError: jest.fn(),
    loadSaleForEdit: jest.fn(),
    updateItemQtyRequested: jest.fn(),
    updateItemQtyRequestedWithSet: jest.fn(),
  };
  const operations = {createSale: jest.fn(), addProduct: jest.fn()};
  const confirmDialog = {onConfirm: jest.fn()};
  const lastError = signal<string | null>(null);
  const currentSale = signal<ISales | null>(null);
  const resetProductSelection = jest.fn();

  function mixin() {
    return TestBed.runInInjectionContext(() =>
      createForceStockHandling({
        facade: facade as never,
        authorizationService: authorizationService as never,
        config: {saleType: 'COMPTANT'},
        currentSale,
        loading: signal(false),
        lastError,
        waitingForForceStockSuccess: signal(false),
        forceStockContext: signal<ForceStockContext>(null),
        getConfirmDialog: () => confirmDialog,
        resetProductSelection,
        operations,
        modalService: modalService as unknown as NgbModal,
        zone,
      }),
    );
  }

  function modaleRepond(motif: string | null): void {
    instance = {};
    modalResult = motif ? Promise.resolve(motif) : Promise.reject('annuler');
    modalResult.catch(() => undefined);
    modalService.open.mockReturnValue({componentInstance: instance, result: modalResult});
  }

  const ligne = (): ISalesLine => ({produitId: 1, produitLibelle: 'SPASFON', quantityRequested: 3});
  const flush = () => new Promise(resolve => setTimeout(resolve));

  beforeEach(() => {
    jest.clearAllMocks();
    TestBed.configureTestingModule({});
    authorizationService.canForceStock.mockReturnValue(true);
    authorizationService.canRegulariserStock.mockReturnValue(true);
    currentSale.set(null);
    lastError.set(null);
    facade.errorDetails.set(null);
  });

  it('ouvre le choix avec le produit, la quantité et les privilèges du caissier', () => {
    authorizationService.canRegulariserStock.mockReturnValue(false);
    modaleRepond('RUPTURE_AVOIR');

    mixin().handleStockError({errorKey: 'stock', attemptedLine: ligne()});

    expect(modalService.open).toHaveBeenCalledWith(ForceStockChoiceModalComponent, expect.objectContaining({backdrop: 'static'}));
    expect(instance).toEqual({produitLibelle: 'SPASFON', quantiteDemandee: 3, canRupture: true, canEcart: false});
  });

  it("envoie l'écart d'inventaire comme motif, forçage compris", async () => {
    modaleRepond('ECART_INVENTAIRE');
    const attemptedLine = ligne();

    mixin().handleStockError({errorKey: 'stock', attemptedLine});
    await flush();

    expect(operations.createSale).toHaveBeenCalledWith(expect.objectContaining({forceStock: true, motifForcage: 'ECART_INVENTAIRE'}));
  });

  it('envoie la rupture sur une vente déjà ouverte', async () => {
    modaleRepond('RUPTURE_AVOIR');
    currentSale.set({id: 7, saleId: {id: 7, saleDate: '2026-09-28'}});

    mixin().handleStockError({errorKey: 'stock', attemptedLine: ligne()});
    await flush();

    expect(operations.addProduct).toHaveBeenCalledWith(expect.objectContaining({forceStock: true, motifForcage: 'RUPTURE_AVOIR'}));
  });

  it('transmet le motif quand la quantité est modifiée dans le tableau', async () => {
    modaleRepond('ECART_INVENTAIRE');

    mixin().handleStockError({errorKey: 'stock', attemptedLine: {...ligne(), id: 5}, isFromTableCellEdit: true});
    await flush();

    expect(facade.updateItemQtyRequestedWithSet).toHaveBeenCalledWith(expect.objectContaining({motifForcage: 'ECART_INVENTAIRE'}));
  });

  it('annuler ne force rien et libère la sélection du produit', async () => {
    modaleRepond(null);

    mixin().handleStockError({errorKey: 'stock', attemptedLine: ligne()});
    await flush();

    expect(operations.createSale).not.toHaveBeenCalled();
    expect(operations.addProduct).not.toHaveBeenCalled();
    expect(facade.clearError).toHaveBeenCalled();
    expect(resetProductSelection).toHaveBeenCalled();
  });

  it('le transfert depuis la réserve force sans motif : le serveur tranche', () => {
    const attemptedLine = ligne();

    mixin().onForceStockConfirmed({errorKey: 'stock.reserve.available', attemptedLine}, 'addProduct');

    expect(attemptedLine.forceStock).toBe(true);
    expect(attemptedLine.motifForcage).toBeUndefined();
  });

  describe("déclenchement sur l'erreur de stock", () => {
    function erreur(errorKey: string): void {
      const handling = mixin();
      TestBed.runInInjectionContext(() => handling.setupErrorHandlingEffect());
      facade.errorDetails.set({errorKey, attemptedLine: ligne()});
      lastError.set('Stock insuffisant');
      TestBed.tick();
    }

    it("propose le choix à qui n'a que le privilège de régularisation", () => {
      authorizationService.canForceStock.mockReturnValue(false);
      modaleRepond('ECART_INVENTAIRE');

      erreur('stock');

      expect(modalService.open).toHaveBeenCalled();
    });

    it("ne propose rien à qui n'a aucun des deux privilèges", () => {
      authorizationService.canForceStock.mockReturnValue(false);
      authorizationService.canRegulariserStock.mockReturnValue(false);

      erreur('stock');

      expect(modalService.open).not.toHaveBeenCalled();
    });

    it('garde la confirmation simple pour le transfert depuis la réserve', () => {
      erreur('stock.reserve.available');

      expect(confirmDialog.onConfirm).toHaveBeenCalled();
      expect(modalService.open).not.toHaveBeenCalled();
    });

    it("la réserve n'est pas proposée à qui ne peut que régulariser", () => {
      authorizationService.canForceStock.mockReturnValue(false);

      erreur('stock.reserve.available');

      expect(confirmDialog.onConfirm).not.toHaveBeenCalled();
    });
  });
});
