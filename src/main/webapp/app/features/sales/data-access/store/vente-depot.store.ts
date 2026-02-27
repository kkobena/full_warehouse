import { computed } from '@angular/core';
import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';
import { ISales } from '../../../../shared/model/sales.model';
import { ISalesLine } from '../../../../shared/model/sales-line.model';
import { IMagasin } from '../../../../shared/model/magasin.model';
import { ProduitSearch } from '../../../../shared/model';
import { IUser } from '../../../../core/user/user.model';

/**
 * Types d'événements métier émis par la facade vente dépôt.
 */
export type VenteDepotEventType = 'PRODUCT_ADDED' | 'LINE_UPDATED' | 'LINE_REMOVED' | 'REMISE_UPDATED' | 'SALE_FINALIZED';

export interface VenteDepotEvent {
  type: VenteDepotEventType;
  payload?: any;
  /** Compteur monotone — garantit la détection de changement du signal */
  seq: number;
}

interface VenteDepotState {
  // Vente en cours
  currentSale: ISales | null;

  // Dépôt sélectionné
  selectedDepot: IMagasin | null;
  depots: IMagasin[];

  // Utilisateurs
  cashier: IUser | null;
  seller: IUser | null;

  // Configuration
  quantityMax: number;
  canReceipt: boolean;
  canInvoice: boolean;

  // Produit sélectionné
  selectedProductData: ProduitSearch | null;

  // Gestion des erreurs et du chargement
  loading: boolean;
  error: string | null;
  errorDetails: {
    errorKey: string | null;
    originalError: any;
    attemptedLine?: ISalesLine;
    isFromTableCellEdit?: boolean;
  } | null;

  // Bus d'événements — remplace les Subjects
  lastEvent: VenteDepotEvent | null;
  _eventSeq: number;
}

const initialState: VenteDepotState = {
  currentSale: null,
  selectedDepot: null,
  depots: [],
  cashier: null,
  seller: null,
  quantityMax: 10000,
  canReceipt: true,
  canInvoice: false,
  selectedProductData: null,
  loading: false,
  error: null,
  errorDetails: null,
  lastEvent: null,
  _eventSeq: 0,
};

/**
 * Store centralisé pour la vente dépôt.
 *
 * Gère tout l'état précédemment géré par DepotService (signals WritableSignal)
 * et les Subjects de la facade. Utilise le pattern ngrx/signals.
 */
export const VenteDepotStore = signalStore(
  { providedIn: 'root' },

  withState(initialState),

  withComputed(store => ({
    selectedProduct: computed(() => store.selectedProductData()),

    salesLines: computed(() => store.currentSale()?.salesLines ?? []),

    isEmpty: computed(() => {
      const sale = store.currentSale();
      return !sale || !sale.salesLines || sale.salesLines.length === 0;
    }),

    canSave: computed(() => {
      const sale = store.currentSale();
      return !!sale && (sale.salesLines?.length ?? 0) > 0 && !store.loading();
    }),

    totalQtyProduit: computed(() => (store.currentSale()?.salesLines ?? []).reduce((sum, l) => sum + l.quantityRequested, 0)),

    totalQtyServi: computed(() => (store.currentSale()?.salesLines ?? []).reduce((sum, l) => sum + l.quantitySold, 0)),

    isAvoir: computed(() => {
      const sale = store.currentSale();
      if (!sale?.salesLines?.length) return false;
      const totalRequested = sale.salesLines.reduce((sum, l) => sum + l.quantityRequested, 0);
      const totalSold = sale.salesLines.reduce((sum, l) => sum + l.quantitySold, 0);
      return totalRequested !== totalSold;
    }),
  })),

  withMethods(store => ({
    setCurrentSale(sale: ISales | null): void {
      patchState(store, { currentSale: sale, error: null });
    },

    setSelectedProduct(product: ProduitSearch | null): void {
      patchState(store, { selectedProductData: product });
    },

    setSelectedDepot(depot: IMagasin | null): void {
      patchState(store, { selectedDepot: depot });
    },

    setDepots(depots: IMagasin[]): void {
      patchState(store, { depots });
    },

    setCashier(cashier: IUser | null): void {
      patchState(store, { cashier });
    },

    setSeller(seller: IUser | null): void {
      patchState(store, { seller });
    },

    setQuantityMax(quantityMax: number): void {
      patchState(store, { quantityMax });
    },

    setCanReceipt(canReceipt: boolean): void {
      patchState(store, { canReceipt });
    },

    setCanInvoice(canInvoice: boolean): void {
      patchState(store, { canInvoice });
    },

    setLoading(loading: boolean): void {
      patchState(store, { loading });
    },

    setError(error: string | null): void {
      patchState(store, { error });
    },

    setErrorDetails(errorDetails: { errorKey: string | null; originalError: any; attemptedLine?: any; isFromTableCellEdit?: boolean } | null): void {
      patchState(store, { errorDetails });
    },

    clearError(): void {
      patchState(store, { error: null, errorDetails: null });
    },

    /**
     * Émet un événement métier (remplace Subject.next() dans la facade).
     * Le compteur seq monotone garantit que le signal change même si le type est identique.
     */
    emitEvent(type: VenteDepotEventType, payload?: any): void {
      const nextSeq = store._eventSeq() + 1;
      patchState(store, { lastEvent: { type, payload, seq: nextSeq }, _eventSeq: nextSeq });
    },

    /**
     * Réinitialise tout l'état (nouvelle session de vente dépôt).
     * Conserve depots et quantityMax pour éviter des appels API inutiles.
     */
    resetForNewSession(): void {
      patchState(store, {
        currentSale: null,
        selectedDepot: null,
        selectedProductData: null,
        cashier: null,
        seller: null,
        error: null,
        errorDetails: null,
        loading: false,
        lastEvent: null,
        _eventSeq: 0,
      });
    },

    /** Réinitialisation complète (y compris depots et quantityMax). */
    reset(): void {
      patchState(store, initialState);
    },

    resetCurrentSale(): void {
      patchState(store, { currentSale: null, error: null, errorDetails: null });
    },
  })),
);
