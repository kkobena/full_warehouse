import { computed, inject } from '@angular/core';
import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { pipe, switchMap, tap, catchError, of } from 'rxjs';
import { ISales, SaleId } from '../../../../shared/model/sales.model';
import { ICustomer } from '../../../../shared/model/customer.model';
import { ISalesLine } from '../../../../shared/model/sales-line.model';
import { IUser } from '../../../../core/user/user.model';

/**
 * Sales State Interface
 * Centralizes all state previously scattered across 14+ singleton services
 */
interface SalesState {
  // Current Sale
  currentSale: ISales | null;
  isEdit: boolean;
  isSaving: boolean;
  voFromCashSale: boolean;
  isVenteSansBon: boolean;
  typeVo: string | null;
  printInvoice: boolean;
  printReceipt: boolean;
  plafondIsReached: boolean;

  // Customer
  selectedCustomer: ICustomer | null;

  // Product Search
  selectedProductData: any | null;

  // Users
  cashier: IUser | null;
  seller: IUser | null;

  // Sale Configuration
  saleType: 'COMPTANT' | 'ASSURANCE' | 'CARNET';
  typePrescription: string | null;

  // Payment
  selectedPaymentMode: string | null;
  lastCurrencyGiven: number;

  // UI State
  isLargeScreen: boolean;
  showInsuranceDataBar: boolean;
  sidebarCollapsed: boolean;

  // Lists
  pendingSales: ISales[];

  // Error Handling
  error: string | null;
  loading: boolean;
}

/**
 * Initial state for Sales Store
 */
const initialState: SalesState = {
  // Current Sale
  currentSale: null,
  isEdit: false,
  isSaving: false,
  voFromCashSale: false,
  isVenteSansBon: false,
  typeVo: null,
  printInvoice: false,
  printReceipt: true,
  plafondIsReached: false,

  // Customer
  selectedCustomer: null,

  // Product Search
  selectedProductData: null,

  // Users
  cashier: null,
  seller: null,

  // Sale Configuration
  saleType: 'COMPTANT',
  typePrescription: null,

  // Payment
  selectedPaymentMode: null,
  lastCurrencyGiven: 0,

  // UI State
  isLargeScreen: true,
  showInsuranceDataBar: true,
  sidebarCollapsed: false,

  // Lists
  pendingSales: [],

  // Error Handling
  error: null,
  loading: false,
};

/**
 * Sales Store
 * Centralized state management for all sales-related data and operations
 * Replaces 14+ singleton services with a single, predictable store
 * 
 * @example
 * // In component:
 * store = inject(SalesStore);
 * 
 * // Read state
 * currentSale = this.store.currentSale;
 * salesLines = this.store.salesLines;
 * 
 * // Update state
 * this.store.setCurrentSale(sale);
 * this.store.addSalesLine(salesLine);
 */
export const SalesStore = signalStore(
  { providedIn: 'root' },

  // State definition
  withState(initialState),

  // Computed selectors
  withComputed((store) => ({
    /**
     * Calculate total items quantity in current sale
     */
    totalItems: computed(() => {
      const sale = store.currentSale();
      if (!sale?.salesLines) return 0;
      return sale.salesLines.reduce((sum, line) => sum + (line.quantityRequested || 0), 0);
    }),

    /**
     * Check if current sale can be saved
     */
    canSave: computed(() => {
      const sale = store.currentSale();
      return sale && 
             sale.salesLines && 
             sale.salesLines.length > 0 && 
             !store.isSaving() &&
             !store.loading();
    }),

    /**
     * Check if current sale is a VO (Vente avec Ordonnance) sale
     */
    isVOSale: computed(() => {
      const type = store.saleType();
      return type === 'ASSURANCE' || type === 'CARNET';
    }),

    /**
     * Check if sale has a customer
     */
    hasCustomer: computed(() => {
      return store.selectedCustomer() !== null;
    }),

    /**
     * Get number of lines in current sale
     */
    salesLinesCount: computed(() => {
      const sale = store.currentSale();
      return sale?.salesLines?.length || 0;
    }),

    /**
     * Check if sale is empty
     */
    isEmpty: computed(() => {
      const sale = store.currentSale();
      return !sale || !sale.salesLines || sale.salesLines.length === 0;
    }),

    /**
     * Get remaining amount to pay (restToPay from backend)
     */
    remainingAmount: computed(() => {
      const sale = store.currentSale();
      return sale?.restToPay || 0;
    }),

    /**
     * Get sales lines array from current sale
     */
    salesLines: computed(() => {
      const sale = store.currentSale();
      return sale?.salesLines || [];
    }),

    /**
     * Get currently selected product for search
     */
    selectedProduct: computed(() => {
      return store.selectedProductData();
    }),

    /**
     * Calculate total quantity requested (quantité demandée)
     */
    totalQuantityRequested: computed(() => {
      const sale = store.currentSale();
      if (!sale?.salesLines) return 0;
      return sale.salesLines.reduce((sum, line) => sum + (line.quantityRequested || 0), 0);
    }),

    /**
     * Calculate total quantity sold (quantité servie)
     */
    totalQuantitySold: computed(() => {
      const sale = store.currentSale();
      if (!sale?.salesLines) return 0;
      return sale.salesLines.reduce((sum, line) => sum + (line.quantitySold || 0), 0);
    }),

    /**
     * Determine if current sale requires an avoir (credit note)
     * Avoir automatique si quantityRequested ≠ quantitySold
     */
    isAvoir: computed(() => {
      const sale = store.currentSale();
      if (!sale?.salesLines || sale.salesLines.length === 0) return false;
      
      const totalRequested = sale.salesLines.reduce((sum, line) => sum + (line.quantityRequested || 0), 0);
      const totalSold = sale.salesLines.reduce((sum, line) => sum + (line.quantitySold || 0), 0);
      
      return totalRequested !== totalSold;
    }),
  })),

  // Methods for state updates
  withMethods((store) => ({
    /**
     * Set current sale
     */
    setCurrentSale(sale: ISales | null): void {
      patchState(store, { currentSale: sale, error: null });
    },

    /**
     * Set selected customer
     */
    setSelectedCustomer(customer: ICustomer | null): void {
      patchState(store, { selectedCustomer: customer });
    },

    /**
     * Set selected product for search
     */
    setSelectedProductData(product: any | null): void {
      patchState(store, { selectedProductData: product });
    },

    /**
     * Set cashier user
     */
    setCashier(cashier: IUser | null): void {
      patchState(store, { cashier });
    },

    /**
     * Set seller user
     */
    setSeller(seller: IUser | null): void {
      patchState(store, { seller });
    },

    /**
     * Set sale type (COMPTANT, ASSURANCE, CARNET)
     */
    setSaleType(saleType: 'COMPTANT' | 'ASSURANCE' | 'CARNET'): void {
      patchState(store, { saleType });
    },

    /**
     * Set prescription type
     */
    setTypePrescription(typePrescription: string | null): void {
      patchState(store, { typePrescription });
    },

    /**
     * Set selected payment mode
     */
    setPaymentMode(paymentMode: string | null): void {
      patchState(store, { selectedPaymentMode: paymentMode });
    },

    /**
     * Set last currency given
     */
    setLastCurrencyGiven(amount: number): void {
      patchState(store, { lastCurrencyGiven: amount });
    },

    /**
     * Set loading state
     */
    setLoading(loading: boolean): void {
      patchState(store, { loading });
    },

    /**
     * Set error message
     */
    setError(error: string | null): void {
      patchState(store, { error });
    },

    /**
     * Set saving state
     */
    setIsSaving(isSaving: boolean): void {
      patchState(store, { isSaving });
    },

    /**
     * Set edit mode
     */
    setIsEdit(isEdit: boolean): void {
      patchState(store, { isEdit });
    },

    /**
     * Set VO from cash sale flag
     */
    setVoFromCashSale(voFromCashSale: boolean): void {
      patchState(store, { voFromCashSale });
    },

    /**
     * Set vente sans bon flag
     */
    setVenteSansBon(isVenteSansBon: boolean): void {
      patchState(store, { isVenteSansBon });
    },

    /**
     * Set type VO
     */
    setTypeVo(typeVo: string | null): void {
      patchState(store, { typeVo });
    },

    /**
     * Set print invoice flag
     */
    setPrintInvoice(printInvoice: boolean): void {
      patchState(store, { printInvoice });
    },

    /**
     * Set print receipt flag
     */
    setPrintReceipt(printReceipt: boolean): void {
      patchState(store, { printReceipt });
    },

    /**
     * Set plafond reached flag
     */
    setPlafondIsReached(isReached: boolean): void {
      patchState(store, { plafondIsReached: isReached });
    },

    /**
     * Toggle insurance data bar visibility
     */
    toggleInsuranceDataBar(): void {
      patchState(store, { 
        showInsuranceDataBar: !store.showInsuranceDataBar() 
      });
    },

    /**
     * Toggle sidebar collapsed state
     */
    toggleSidebar(): void {
      patchState(store, { 
        sidebarCollapsed: !store.sidebarCollapsed() 
      });
    },

    /**
     * Set screen size
     */
    setScreenSize(isLarge: boolean): void {
      patchState(store, { isLargeScreen: isLarge });
    },

    /**
     * Add sales line to current sale
     */
    addSalesLine(salesLine: ISalesLine): void {
      const currentSale = store.currentSale();
      if (!currentSale) return;

      const updatedSale: ISales = {
        ...currentSale,
        salesLines: [...(currentSale.salesLines || []), salesLine],
      };

      patchState(store, { currentSale: updatedSale });
    },

    /**
     * Update sales line in current sale
     */
    updateSalesLine(lineId: number, updates: Partial<ISalesLine>): void {
      const currentSale = store.currentSale();
      if (!currentSale?.salesLines) return;

      const updatedLines = currentSale.salesLines.map(line =>
        line.id === lineId ? { ...line, ...updates } : line
      );

      const updatedSale: ISales = {
        ...currentSale,
        salesLines: updatedLines,
      };

      patchState(store, { currentSale: updatedSale });
    },

    /**
     * Remove sales line from current sale
     */
    removeSalesLine(lineId: number): void {
      const currentSale = store.currentSale();
      if (!currentSale?.salesLines) return;

      const updatedLines = currentSale.salesLines.filter(line => line.id !== lineId);

      const updatedSale: ISales = {
        ...currentSale,
        salesLines: updatedLines,
      };

      patchState(store, { currentSale: updatedSale });
    },

    /**
     * Add pending sale to list
     */
    addPendingSale(sale: ISales): void {
      const pendingSales = [...store.pendingSales(), sale];
      patchState(store, { pendingSales });
    },

    /**
     * Remove pending sale from list
     */
    removePendingSale(saleId: SaleId): void {
      const pendingSales = store.pendingSales().filter(s => 
        s.saleId?.id !== saleId.id || s.saleId?.saleDate !== saleId.saleDate
      );
      patchState(store, { pendingSales });
    },

    /**
     * Clear error message
     */
    clearError(): void {
      patchState(store, { error: null });
    },

    /**
     * Reset store to initial state
     */
    reset(): void {
      patchState(store, initialState);
    },

    /**
     * Reset only current sale
     */
    resetCurrentSale(): void {
      patchState(store, {
        currentSale: null,
        isEdit: false,
        isSaving: false,
        voFromCashSale: false,
        isVenteSansBon: false,
        typeVo: null,
        printInvoice: false,
        printReceipt: true,
        plafondIsReached: false,
        selectedCustomer: null,
        error: null,
      });
    },
  })),
);
