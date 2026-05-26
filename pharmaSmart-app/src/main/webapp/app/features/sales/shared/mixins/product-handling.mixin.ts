import {Signal, signal} from '@angular/core';
import {ISales, ISalesLine, ProduitSearch} from '../../../../shared/model';
import {SalesFacade} from '../../data-access/facades/sales.facade';
import {CustomerDisplayService} from '../../data-access/services/customer-display.service';
import {NotificationService} from '../../../../shared/services/notification.service';
import {createSalesLineFromProduct} from '../../data-access/utils/sales-line.utils';

/**
 * Type pour les infos de produit en attente d'affichage
 */
export interface PendingDisplayProduct {
  libelle: string;
  quantity: number;
  price: number;
}

/**
 * Interface pour les composants utilisant le ProductSearch
 */
export interface ProductSearchHost {
  productSearchComponent(): { getFocus(): void; reset(): void } | undefined;

  quantityComponent(): { focusProduitControl(): void; reset(qty: number): void } | undefined;
}

/**
 * Type pour la fonction de création de vente (différente selon le type de vente)
 */
export type CreateSaleFunction = (line: ISalesLine) => void;
export type AddProductFunction = (line: ISalesLine) => void;

/**
 * Options pour la configuration du mixin de gestion produits
 */
export interface ProductHandlingConfig {
  requiresCustomer: boolean;
  customerRequiredMessage?: string;
  saleType: 'COMPTANT' | 'CARNET' | 'ASSURANCE';
}

/**
 * Contexte partagé pour les opérations de gestion de produits
 */
export interface ProductHandlingContext {
  facade: SalesFacade;
  customerDisplay: CustomerDisplayService;
  notificationService: NotificationService;
  host: ProductSearchHost;
  config: ProductHandlingConfig;
  // Signals
  selectedProduct: Signal<ProduitSearch | null>;
  currentSale: Signal<ISales | null>;
  hasCustomer?: Signal<boolean>;
  // Callbacks spécifiques au type de vente
  createSale: CreateSaleFunction;
  addProduct: AddProductFunction;
}

/**
 * Mixin pour la gestion des produits dans les composants de vente
 *
 * Fournit les méthodes communes pour :
 * - Sélection de produit
 * - Scan de code-barres
 * - Ajout de quantité
 * - Reset de la sélection
 * - Focus sur la recherche
 *
 * IMPORTANT: Le composant doit s'abonner à facade.productAddedSuccess$ pour gérer
 * le reset et le focus après ajout réussi d'un produit.
 *
 * @example
 * ```typescript
 * // Dans le composant
 * private productHandling = createProductHandling({
 *   facade: this.facade,
 *   customerDisplay: this.customerDisplay,
 *   notificationService: this.notificationService,
 *   host: this,
 *   config: { requiresCustomer: false, saleType: 'COMPTANT' },
 *   selectedProduct: this.facade.selectedProduct,
 *   currentSale: this.facade.currentSale,
 *   createSale: (line) => this.facade.createComptantSale(line),
 *   addProduct: (line) => this.facade.onAddProduit(line),
 * });
 *
 * // Dans ngOnInit - OBLIGATOIRE pour le reset et l'affichage après succès
 * this.facade.productAddedSuccess$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
 *   this.productHandling.updatePendingDisplay(); // Affiche le produit sur l'écran client
 *   this.productHandling.resetProductSelection();
 * });
 *
 * // Utilisation
 * onProductSelected(product: ProduitSearch | null): void {
 *   this.productHandling.onProductSelected(product);
 * }
 * ```
 */
export function createProductHandling(context: ProductHandlingContext) {
  const {
    facade,
    customerDisplay,
    notificationService,
    host,
    config,
    selectedProduct,
    currentSale,
  } = context;

  // Signal pour stocker les infos du produit en attente d'affichage (après succès API)
  const pendingDisplayProduct = signal<PendingDisplayProduct | null>(null);

  /**
   * Met le focus sur le composant de recherche produit
   */
  function focusProductSearch(): void {
    setTimeout(() => {
      host.productSearchComponent()?.getFocus();
    }, 100);
  }

  /**
   * Réinitialise la sélection produit et remet le focus sur la recherche
   */
  function resetProductSelection(): void {
    // Réinitialiser le produit sélectionné
    facade.setSelectedProduct(null);

    // Réinitialiser le composant de recherche
    host.productSearchComponent()?.reset();

    // Réinitialiser la quantité à 1
    host.quantityComponent()?.reset(1);

    // Focus sur recherche produit
    focusProductSearch();
  }

  /**
   * Vérifie si un client est requis et présent
   */
  function checkCustomerRequired(): boolean {
    if (!config.requiresCustomer) {
      return true;
    }

    const hasCustomer = context.hasCustomer?.() ?? !!facade.selectedCustomer();
    if (!hasCustomer) {
      notificationService.warning(
        config.customerRequiredMessage || "Veuillez sélectionner un client avant d'ajouter des produits",
        'Client requis',
      );
      return false;
    }
    return true;
  }

  /**
   * Gère la sélection d'un produit avec focus automatique sur la quantité
   */
  function onProductSelected(product: ProduitSearch | null): void {
    if (!product) {
      return;
    }

    // Vérifier si client requis
    if (!checkCustomerRequired()) {
      return;
    }

    facade.setSelectedProduct(product);

    // Focus sur quantité après sélection
    setTimeout(() => {
      host.quantityComponent()?.focusProduitControl();
      host.quantityComponent()?.reset(1);
    }, 100);
  }

  /**
   * Gère le scan d'un code-barres (ajout direct avec quantité 1)
   */
  function onProductScanned(product: ProduitSearch, codeScan?: string | null): void {
    // Vérifier si client requis
    if (!checkCustomerRequired()) {
      return;
    }

    facade.setSelectedProduct(product);
    addProductToSale(product, 1, codeScan);
  }

  /**
   * Gère l'ajout de quantité depuis le composant de saisie
   */
  function onAddQuantity(quantity: number): void {
    const product = selectedProduct();
    if (!product || !quantity || quantity <= 0) {
      return;
    }

    addProductToSale(product, quantity);
  }

  /**
   * Ajoute un produit à la vente en cours ou crée une nouvelle vente
   * Note: Le reset et focus sont gérés via souscription à facade.productAddedSuccess$ dans le composant
   */
  function addProductToSale(product: ProduitSearch, quantity: number, codeScan?: string | null): void {
    const sale = currentSale();
    const salesLine = createSalesLineFromProduct(product, quantity, sale, codeScan);

    // Stocker les infos pour affichage APRÈS succès API (évite les problèmes de synchro en cas d'erreur ex: stock insuffisant)
    pendingDisplayProduct.set({
      libelle: product.libelle || '',
      quantity,
      price: product.regularUnitPrice || 0,
    });

    // Si pas de vente en cours, créer avec ce premier produit
    if (!sale || !sale.saleId) {
      context.createSale(salesLine);
    } else {
      // Sinon ajouter à la vente existante
      context.addProduct(salesLine);
    }
    // Reset, focus et affichage gérés via souscription à facade.productAddedSuccess$ dans le composant
  }

  /**
   * Met à jour l'affichage client avec le produit en attente et clear le signal
   * À appeler dans la souscription à productAddedSuccess$ du composant
   */
  function updatePendingDisplay(): void {
    const pending = pendingDisplayProduct();
    if (pending) {
      customerDisplay.updateDisplayForProduct(pending.libelle, pending.quantity, pending.price);
      pendingDisplayProduct.set(null);
    }
  }

  /**
   * Retourne le signal de produit en attente (lecture seule)
   */
  function getPendingDisplayProduct(): PendingDisplayProduct | null {
    return pendingDisplayProduct();
  }

  return {
    focusProductSearch,
    resetProductSelection,
    onProductSelected,
    onProductScanned,
    onAddQuantity,
    addProductToSale,
    checkCustomerRequired,
    updatePendingDisplay,
    getPendingDisplayProduct,
  };
}

/**
 * Type retourné par createProductHandling
 */
export type ProductHandling = ReturnType<typeof createProductHandling>;
