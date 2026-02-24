import { computed, inject, Injectable } from '@angular/core';
import { merge } from 'rxjs';
import { SalesStore } from '../store/sales.store';
import { ISalesLine } from '../../../../shared/model';
import { SaleLifecycleFacade } from './sale-lifecycle.facade';
import { SaleProductFacade } from './sale-product.facade';
import { SaleCustomerFacade } from './sale-customer.facade';
import { SalePaymentFacade } from './sale-payment.facade';

/**
 * Sales Facade — Orchestrateur mince
 * Délègue aux sous-facades et ré-expose leur API publique.
 * Les 13 consommateurs continuent d'injecter SalesFacade sans modification.
 *
 * @example
 * // In component:
 * facade = inject(SalesFacade);
 *
 * // Read state (montants calculés côté backend)
 * currentSale = this.facade.currentSale;
 * totalAmount = this.facade.currentSale()?.salesAmount;
 * netAmount = this.facade.currentSale()?.netAmount;
 *
 * // Execute actions
 * this.facade.createComptantSale();
 * this.facade.addProductToSale(product, quantity);
 * this.facade.saveSale();
 */
@Injectable({ providedIn: 'root' })
export class SalesFacade {
  // ── Sub-facades ────────────────────────────────────────────
  private readonly lifecycleFacade = inject(SaleLifecycleFacade);
  private readonly productFacade = inject(SaleProductFacade);
  private readonly customerFacade = inject(SaleCustomerFacade);
  private readonly paymentFacade = inject(SalePaymentFacade);

  // ── Store (exposed for direct access) ──────────────────────
  readonly store = inject(SalesStore);

  // ============================================
  // EXPOSE STORE STATE (Read-only)
  // ============================================

  readonly canSave = this.store.canSave;
  readonly canSaveSale = this.canSave;
  readonly currentSale = this.store.currentSale;
  readonly totalAmount = computed(() => this.currentSale()?.salesAmount || 0);
  readonly discountAmount = computed(() => this.currentSale()?.discountAmount || 0);
  readonly taxAmount = computed(() => this.currentSale()?.taxAmount || 0);
  readonly netAmount = computed(() => this.currentSale()?.netAmount || 0);
  readonly amountToBePaid = computed(() => this.currentSale()?.amountToBePaid || 0);
  readonly selectedCustomer = this.store.selectedCustomer;
  readonly cashier = this.store.cashier;
  readonly seller = this.store.seller;
  readonly saleType = this.store.saleType;
  readonly isPresale = this.store.isPresale;
  readonly isDevis = this.store.isDevis;
  readonly pendingTiersPayants = this.store.pendingTiersPayants;
  readonly isEdit = this.store.isEdit;
  readonly isSaving = this.store.isSaving;
  readonly loading = this.store.loading;
  readonly error = this.store.error;
  readonly errorDetails = this.store.errorDetails;
  readonly lastError = this.store.error;
  readonly plafondIsReached = this.store.plafondIsReached;
  readonly plafondMessage = this.store.plafondMessage;
  readonly pendingSales = this.store.pendingSales;
  readonly pendingSalesLoading = this.store.pendingSalesLoading;
  readonly selectedProduct = this.store.selectedProduct;
  readonly salesLines = this.store.salesLines;
  readonly totalItems = this.store.totalItems;
  readonly isVOSale = this.store.isVOSale;
  readonly hasCustomer = this.store.hasCustomer;
  readonly salesLinesCount = this.store.salesLinesCount;
  readonly isEmpty = this.store.isEmpty;
  readonly remainingAmount = this.store.remainingAmount;
  readonly totalQuantityRequested = this.store.totalQuantityRequested;
  readonly totalQuantitySold = this.store.totalQuantitySold;
  readonly isAvoir = this.store.isAvoir;
  readonly typePrescription = computed(() => this.store.typePrescription());

  // ============================================
  // STORE BINDINGS (Pass-through)
  // ============================================

  setCurrentSale = this.store.setCurrentSale.bind(this.store);
  setSelectedCustomer = this.store.setSelectedCustomer.bind(this.store);
  setCashier = this.store.setCashier.bind(this.store);
  setSeller = this.store.setSeller.bind(this.store);
  setSaleType = this.store.setSaleType.bind(this.store);
  setIsPresale = this.store.setIsPresale.bind(this.store);
  setIsDevis = this.store.setIsDevis.bind(this.store);
  setIsEdit = this.store.setIsEdit.bind(this.store);
  setTypePrescription = this.store.setTypePrescription.bind(this.store);
  setPaymentMode = this.store.setPaymentMode.bind(this.store);
  setPrintInvoice = this.store.setPrintInvoice.bind(this.store);
  setPrintReceipt = this.store.setPrintReceipt.bind(this.store);
  addSalesLine = this.store.addSalesLine.bind(this.store);
  updateSalesLine = this.store.updateSalesLine.bind(this.store);
  removeSalesLine = this.store.removeSalesLine.bind(this.store);
  addPendingSale = this.store.addPendingSale.bind(this.store);
  removePendingSale = this.store.removePendingSale.bind(this.store);
  toggleInsuranceDataBar = this.store.toggleInsuranceDataBar.bind(this.store);
  toggleSidebar = this.store.toggleSidebar.bind(this.store);
  clearError = this.store.clearError.bind(this.store);
  reset = this.store.reset.bind(this.store);
  resetCurrentSale = this.store.resetCurrentSale.bind(this.store);

  // ============================================
  // EVENTS — Merged cross-cutting streams
  // ============================================

  /** productAddedSuccess$ is emitted by lifecycle (creation) AND product (add) */
  readonly productAddedSuccess$ = merge(this.lifecycleFacade.saleCreatedWithProductSuccess$, this.productFacade.productAddedSuccess$);

  readonly standbySuccess$ = this.paymentFacade.standbySuccess$;
  readonly lineUpdatedSuccess$ = this.productFacade.lineUpdatedSuccess$;
  readonly lineRemovedSuccess$ = this.productFacade.lineRemovedSuccess$;
  readonly saleReloadedSuccess$ = this.lifecycleFacade.saleReloadedSuccess$;
  readonly customerRemovedSuccess$ = this.customerFacade.customerRemovedSuccess$;
  readonly cancelSaleSuccess$ = this.lifecycleFacade.cancelSaleSuccess$;
  readonly customerSetSuccess$ = this.customerFacade.customerSetSuccess$;
  readonly remiseUpdatedSuccess$ = this.productFacade.remiseUpdatedSuccess$;
  readonly tiersPayantAddedSuccess$ = this.customerFacade.tiersPayantAddedSuccess$;
  readonly saleReloadedToEditSuccess$ = this.lifecycleFacade.saleReloadedToEditSuccess$;
  readonly resumePendingSaleSuccess$ = this.lifecycleFacade.resumePendingSaleSuccess$;

  // ============================================
  // LIFECYCLE — Delegate to SaleLifecycleFacade
  // ============================================

  createComptantSale = this.lifecycleFacade.createComptantSale;
  createDevisSale = this.lifecycleFacade.createDevisSale;
  createAssuranceSale = this.lifecycleFacade.createAssuranceSale;
  createCarnetSale = this.lifecycleFacade.createCarnetSale;
  loadSaleForEdit = this.lifecycleFacade.loadSaleForEdit;
  loadSale = this.lifecycleFacade.loadSale;
  resumePendingSale = this.lifecycleFacade.resumePendingSale;
  cancelSale = this.lifecycleFacade.cancelSale;

  initializeComptantSale = this.lifecycleFacade.initializeComptantSale.bind(this.lifecycleFacade);
  initializeAssuranceSale = this.lifecycleFacade.initializeAssuranceSale.bind(this.lifecycleFacade);
  initializeCarnetSale = this.lifecycleFacade.initializeCarnetSale.bind(this.lifecycleFacade);
  initializeDevisSale = this.lifecycleFacade.initializeDevisSale.bind(this.lifecycleFacade);
  initializeDevisCarnetSale = this.lifecycleFacade.initializeDevisCarnetSale.bind(this.lifecycleFacade);
  transformCashSaleToAssurance = this.lifecycleFacade.transformCashSaleToAssurance.bind(this.lifecycleFacade);
  transformCashSaleToCarnet = this.lifecycleFacade.transformCashSaleToCarnet.bind(this.lifecycleFacade);

  // ============================================
  // PRODUCT — Delegate to SaleProductFacade
  // ============================================

  addProductToSale = this.productFacade.addProductToSale.bind(this.productFacade);
  onAddProduit = this.productFacade.onAddProduit.bind(this.productFacade);
  onAddProduitCarnet = this.productFacade.onAddProduitCarnet.bind(this.productFacade);
  onAddProduitDevis = this.productFacade.onAddProduitDevis.bind(this.productFacade);
  updateItemQtyRequested = this.productFacade.updateItemQtyRequested.bind(this.productFacade);
  updateItemQtyRequestedWithSet = this.productFacade.updateItemQtyRequestedWithSet.bind(this.productFacade);
  removeLine = this.productFacade.removeLine.bind(this.productFacade);
  setSelectedProduct = this.productFacade.setSelectedProduct.bind(this.productFacade);
  updateLineQuantitySold = this.productFacade.updateLineQuantitySold.bind(this.productFacade);
  updateLineQuantityRequested = this.productFacade.updateLineQuantityRequested.bind(this.productFacade);
  updateLinePrice = this.productFacade.updateLinePrice.bind(this.productFacade);
  applyLineDiscount = this.productFacade.applyLineDiscount.bind(this.productFacade);
  updateRemise = this.productFacade.updateRemise.bind(this.productFacade);

  // ============================================
  // CUSTOMER — Delegate to SaleCustomerFacade
  // ============================================

  setCustomer = this.customerFacade.setCustomer.bind(this.customerFacade);
  removeCustomer = this.customerFacade.removeCustomer.bind(this.customerFacade);
  updateSaleTiersPayants = this.customerFacade.updateSaleTiersPayants.bind(this.customerFacade);
  removeTiersPayantFromSale = this.customerFacade.removeTiersPayantFromSale.bind(this.customerFacade);
  addTiersPayantToSale = this.customerFacade.addTiersPayantToSale.bind(this.customerFacade);

  // ============================================
  // PAYMENT — Delegate to SalePaymentFacade
  // ============================================

  saveSale = this.paymentFacade.saveSale.bind(this.paymentFacade);
  saveAssuranceSale = this.paymentFacade.saveAssuranceSale.bind(this.paymentFacade);
  putOnStandby = this.paymentFacade.putOnStandby.bind(this.paymentFacade);
  finalizePresale = this.paymentFacade.finalizePresale.bind(this.paymentFacade);
  saveDevis = this.paymentFacade.saveDevis.bind(this.paymentFacade);
  saveDevisCarnet = this.paymentFacade.saveDevisCarnet.bind(this.paymentFacade);
  loadPendingSales = this.paymentFacade.loadPendingSales.bind(this.paymentFacade);
  deletePendingSale = this.paymentFacade.deletePendingSale.bind(this.paymentFacade);
  printInvoice = this.paymentFacade.printInvoice.bind(this.paymentFacade);
  printReceipt = this.paymentFacade.printReceipt.bind(this.paymentFacade);
  printCurrentSale = this.paymentFacade.printCurrentSale.bind(this.paymentFacade);

  /**
   * Create comptant sale with first product (matches original createComptant)
   */
  createComptant(salesLine: ISalesLine): void {
    this.initializeComptantSale(salesLine);
  }
}
