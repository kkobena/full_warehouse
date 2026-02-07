import { Component, OnInit, inject, signal, DestroyRef, viewChild, output, input, computed } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { take } from 'rxjs/operators';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { MessageService } from 'primeng/api';
import { Toast } from 'primeng/toast';

import { TooltipModule } from 'primeng/tooltip';
import { NgxSpinnerService } from 'ngx-spinner';
import { ConfirmDialogComponent } from '../../../../shared/dialog/confirm-dialog/confirm-dialog.component';
import {
  ProductListComponent,
  SaleSummaryComponent,
  SaleActionsComponent,
  ProductSearchComponent,
  CustomerSelectionModalComponent,
  SaleType,
} from '../../ui';
import { PaymentModeComponent, PaymentCompleteEvent } from '../../ui/payment-mode/payment-mode.component';

import { CashRegisterFormComponent } from '../../../../entities/cash-register/user-cash-register/cash-register-form/cash-register-form.component';
import { UninsuredCustomerFormComponent } from '../../../../entities/customer/uninsured-customer-form/uninsured-customer-form.component';
import { QuantiteProdutSaisieComponent } from '../../../../shared/quantite-produt-saisie/quantite-produt-saisie.component';
import { showCommonModal } from '../../../../entities/sales/selling-home/sale-helper';
import { SalesFacade } from '../../data-access/facades/sales.facade';
import { CustomerSearchService } from '../../data-access/services/customer-search.service';
import { AuthorizationService } from '../../data-access/services/authorization.service';
import { CustomerDisplayService } from '../../data-access/services/customer-display.service';
import { NotificationService } from '../../../../shared/services/notification.service';
import { ISalesLine } from '../../../../shared/model';
import { ICustomer } from '../../../../shared/model';
import { ProduitSearch } from '../../../../shared/model';
import { ISales } from '../../../../shared/model';
import { IRemise } from '../../../../shared/model';
import { IUser } from '../../../../core/user/user.model';
import { UserVendeurService } from '../../../../entities/sales/service/user-vendeur.service';
import {
  createProductHandling,
  ProductSearchHost,
  createForceStockHandling,
  createPaymentHandling,
  createCustomerHandling,
} from '../../shared/mixins';

/**
 * Composant Container : Création de vente (Comptant)
 *
 * Responsabilités :
 * - Orchestrer les composants UI
 * - Gérer la logique métier via SalesFacade
 * - Gérer la navigation et les erreurs
 * - Gérer les raccourcis clavier
 *
 * Architecture : Container/Presentation pattern
 */
@Component({
  selector: 'app-sale-creation',
  templateUrl: './sale-creation.component.html',
  styleUrls: ['./sale-creation.component.scss'],
  host: {
    '(window:keydown)': 'handleKeyboardEvent($event)',
  },
  imports: [
    CommonModule,
    FormsModule,
    TooltipModule,
    Toast,
    ProductSearchComponent,
    ProductListComponent,
    SaleSummaryComponent,
    SaleActionsComponent,
    PaymentModeComponent,
    ConfirmDialogComponent,
    QuantiteProdutSaisieComponent,
  ],
  providers: [MessageService], // Instance locale pour ce composant
})
export class SaleCreationComponent implements OnInit, ProductSearchHost {
  productSearchComponent = viewChild<ProductSearchComponent>('produitbox');
  quantityComponent = viewChild<QuantiteProdutSaisieComponent>('quantityBox');
  confirmDialog = viewChild.required<ConfirmDialogComponent>('confirmDialog');
  paymentMode = viewChild<PaymentModeComponent>('paymentMode');

  /**
   * Méthode publique pour mettre le focus sur la recherche produit
   * Appelée par le composant parent lors du changement de tab
   */
  public focusProductSearch(): void {
    setTimeout(() => {
      this.productSearchComponent()?.getFocus();
    }, 100);
  }

  // Output pour notifier le container du succès de l'ajout (règle métier: reset après succès)
  productAddedSuccess = output<void>();
  cashRegisterOpened = output<void>();

  // Modal and responsive state
  readonly isCashRegisterOpen = input(false);
  readonly isSmallScreen = input(false);

  // Services
  protected facade = inject(SalesFacade);
  private customerSearchService = inject(CustomerSearchService);
  private authorizationService = inject(AuthorizationService);
  private notificationService = inject(NotificationService);
  private customerDisplay = inject(CustomerDisplayService);
  private router = inject(Router);
  private translate = inject(TranslateService);
  private modalService = inject(NgbModal);
  private destroyRef = inject(DestroyRef);
  private spinner = inject(NgxSpinnerService);
  protected userVendeurService = inject(UserVendeurService);

  // State depuis le store (signals computed)
  currentSale = this.facade.currentSale;
  salesLines = this.facade.salesLines;
  selectedCustomer = this.facade.selectedCustomer;
  selectedProduct = this.facade.selectedProduct;
  canSave = this.facade.canSave;
  isSaving = this.facade.isSaving;
  loading = this.facade.loading;
  lastError = this.facade.lastError;
  remises = signal<IRemise[]>([]); // TODO: charger les remises depuis le service

  // Local UI state
  customers = signal<ICustomer[]>([]);
  selectedLineId = signal<number | null>(null);
  selectedSaleType = signal<SaleType>('COMPTANT');
  isDiffere = signal<boolean>(false); // Signal local pour forcer la détection de changement

  // Événement de paiement en attente (pour flux avoir avec sélection client)
  private pendingPaymentEvent = signal<PaymentCompleteEvent | null>(null);

  // Flag pour éviter de réafficher le dialogue avoir après confirmation
  private avoirConfirmed = signal<boolean>(false);

  // ===== Product Handling Mixin =====
  private productHandling = createProductHandling({
    facade: this.facade,
    customerDisplay: this.customerDisplay,
    notificationService: this.notificationService,
    host: this,
    config: {
      requiresCustomer: false, // Client optionnel pour vente comptant
      saleType: 'COMPTANT',
    },
    selectedProduct: this.facade.selectedProduct,
    currentSale: this.facade.currentSale,
    createSale: (line: ISalesLine) => this.facade.createComptant(line),
    addProduct: (line: ISalesLine) => this.facade.onAddProduit(line),
  });

  // Monnaie calculée en temps réel depuis le composant payment-mode
  currentChange = computed(() => {
    const change = this.paymentMode()?.changeAmount() || 0;
    return change > 0 ? change : null;
  });
  canEditPrice = signal<boolean>(false);

  // Computed pour convertir l'input isCashRegisterOpen en Signal<boolean>
  private isCashRegisterOpenSignal = computed(() => this.isCashRegisterOpen() ?? false);

  // Force Stock state signals
  waitingForForceStockSuccess = signal<boolean>(false);
  previousLoadingState = signal<boolean>(false);
  forceStockContext = signal<'addProduct' | 'editCell' | null>(null);

  // ===== Force Stock Handling Mixin =====
  private forceStockHandling = createForceStockHandling({
    facade: this.facade,
    authorizationService: this.authorizationService,
    spinner: this.spinner,
    config: { saleType: 'COMPTANT' },
    currentSale: this.facade.currentSale,
    loading: this.facade.loading,
    lastError: this.facade.lastError,
    waitingForForceStockSuccess: this.waitingForForceStockSuccess,
    previousLoadingState: this.previousLoadingState,
    forceStockContext: this.forceStockContext,
    getConfirmDialog: () => this.confirmDialog(),
    resetProductSelection: () => this.productHandling.resetProductSelection(),
    operations: {
      createSale: (line: ISalesLine) => this.facade.createComptantSale(line),
      addProduct: (line: ISalesLine) => this.facade.onAddProduit(line),
    },
  });

  // UI state for sidebar and pending sales
  sidebarCollapsed = signal(false);
  pendingSalesSidebar = signal(false);
  pendingSalesCount = signal(0);

  // Vendeur sélectionné
  selectedSeller = signal<IUser | null>(null);

  // ===== Payment Handling Mixin =====
  private paymentHandling = createPaymentHandling({
    facade: this.facade,
    notificationService: this.notificationService,
    customerDisplay: this.customerDisplay,
    config: {
      saleType: 'COMPTANT',
      toleranceThreshold: 5, // Seuil de tolérance pour le reste à payer
      allowDiffere: true,
    },
    currentSale: this.facade.currentSale,
    salesLines: this.facade.salesLines,
    canSave: this.canSave,
    isCashRegisterOpen: this.isCashRegisterOpenSignal,
    getPaymentModeComponent: () => {
      const comp = this.paymentMode();
      if (!comp) return undefined;
      // Adapter le type pour le mixin
      return {
        selectedModes: () =>
          comp.selectedModes().map(m => ({
            mode: m.mode,
            amount: m.amount ?? 0,
            amountEntered: m.amountEntered,
          })),
        totalPaid: () => comp.totalPaid(),
        changeAmount: () => comp.changeAmount(),
        changeExact: () => comp.changeExact(),
        focusFirstMode: () => comp.focusFirstMode(),
      };
    },
    openCashRegister: () => this.openCashRegister(),
    resetForNewSale: () => this.resetForNewSale(),
    showConfirmDialog: (onConfirm, title, message, onCancel) =>
      this.confirmDialog().onConfirm(onConfirm, title, message, undefined, onCancel),
    onDiffereConfirmed: () => this.handleDiffereConfirmed(),
  });

  // ===== Customer Handling Mixin =====
  private customerHandling = createCustomerHandling({
    facade: this.facade,
    customerSearchService: this.customerSearchService,
    notificationService: this.notificationService,
    modalService: this.modalService,
    config: {
      saleType: 'COMPTANT',
      customerRequired: false, // Client optionnel pour vente comptant
    },
    selectedCustomer: this.facade.selectedCustomer,
    customers: this.customers,
    customerFormComponent: UninsuredCustomerFormComponent,
    onCustomerSelectedCallback: () => this.focusProductSearch(),
  });

  constructor() {
    // Initialiser les effects de gestion du forçage de stock via le mixin
    this.forceStockHandling.initializeEffects();
  }

  ngOnInit(): void {
    // Initialize customer display
    this.customerDisplay.initialize('PHARMA SMART');

    // S'abonner à l'événement de succès de mise en attente
    this.facade.standbySuccess$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      this.resetForNewSale();
    });

    // S'abonner aux événements de succès pour gérer le focus et reset
    this.facade.productAddedSuccess$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      // Utiliser le mixin pour l'affichage client et le reset
      this.productHandling.updatePendingDisplay();
      this.productHandling.resetProductSelection();
      this.productAddedSuccess.emit();
    });

    this.facade.lineUpdatedSuccess$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      this.productHandling.focusProductSearch();
    });

    this.facade.lineRemovedSuccess$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      this.productHandling.focusProductSearch();
    });

    // S'abonner au rechargement de vente (après annulation forçage stock)
    this.facade.saleReloadedSuccess$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      this.productHandling.resetProductSelection();
    });

    // S'abonner à la suppression du client (après succès API)
    this.facade.customerRemovedSuccess$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      this.productHandling.focusProductSearch();
    });

    // S'abonner à l'annulation de la vente (après succès API)
    this.facade.cancelSaleSuccess$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      this.resetForNewSale();
    });

    // Charger le nombre de ventes en attente
    this.loadPendingSalesCount();

    // Initialiser le vendeur avec celui du store
    const currentSeller = this.facade.seller();
    if (currentSeller) {
      this.selectedSeller.set(currentSeller);
    }

    // Vérifier si l'utilisateur peut modifier les prix
    this.canEditPrice.set(this.authorizationService.canModifyPrice());
  }

  // ===== Handlers pour ProductSearchComponent =====

  /**
   * Délègue au mixin productHandling
   * Focus automatique sur quantité après sélection
   */
  onProductSelected(product: ProduitSearch | null): void {
    this.productHandling.onProductSelected(product);
  }

  /**
   * Délègue au mixin productHandling
   * Gère l'ajout de quantité depuis le composant QuantiteProdutSaisieComponent
   */
  onAddQuantity(quantity: number): void {
    this.productHandling.onAddQuantity(quantity);
  }

  /**
   * Délègue au mixin productHandling
   * Gère le scan d'un code-barres
   */
  onProductScanned(product: ProduitSearch): void {
    this.productHandling.onProductScanned(product);
  }

  /**
   *  Gestion Enter dans champ produit vide
   * Si vente en cours → ouvre modal paiement avec focus sur CASH
   * Si montant = 0 → sauvegarde directe
   */
  onProductSearchEnter(shouldSave: boolean): void {
    if (!shouldSave) return;

    const currentSale = this.currentSale();

    // Si vente en cours avec des lignes
    if (currentSale && currentSale.salesLines && currentSale.salesLines.length > 0) {
      // Le composant payment est INLINE (pas de modal), juste focus dessus
      setTimeout(() => {
        this.paymentMode()?.focusFirstMode();
      }, 100);
    }
  }

  // ===== Handlers pour ProductListComponent =====

  onLineQuantityChanged(data: { line: ISalesLine; newQty: number }): void {
    if (data.line.id) {
      this.facade.updateLineQuantity(data.line.id, data.newQty);
    }
    // Focus géré via souscription à lineUpdatedSuccess$
  }

  onLineQuantityRequestedChanged(data: { line: ISalesLine; newQty: number }): void {
    if (data.line.id) {
      this.facade.updateLineQuantityRequested(data.line.id, data.newQty);
    }
    // Focus géré via souscription à lineUpdatedSuccess$
  }

  onLineRemoved(line: ISalesLine): void {
    if (line.saleLineId) {
      this.facade.removeLine(line.saleLineId);
    }
    // Focus géré via souscription à lineRemovedSuccess$
  }

  onAuthorizationRequired(event: { line: ISalesLine; action: 'delete' | 'discount' }): void {
    const saleId = this.currentSale()?.id;
    const saleType = this.selectedSaleType();

    if (event.action === 'delete') {
      this.authorizationService
        .requestDeleteProductAuthorization(saleId, saleType)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe(authorized => {
          if (authorized && event.line.saleLineId) {
            this.facade.removeLine(event.line.saleLineId);
          }
        });
    } else if (event.action === 'discount') {
      this.authorizationService
        .requestDiscountAuthorization(saleId, saleType)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe(authorized => {
          if (authorized) {
            // TODO: Ouvrir modal de saisie remise
          }
        });
    }
  }

  onLineSelected(line: ISalesLine): void {
    this.selectedLineId.set(line.id || null);
  }

  onLineDiscountChanged(data: { line: ISalesLine; newDiscount: number }): void {
    if (data.line.id) {
      this.facade.applyLineDiscount(data.line.id, data.newDiscount);
    }
  }

  onLinePriceChanged(data: { line: ISalesLine; newPrice: number }): void {
    if (data.line.id) {
      this.facade.updateLinePrice(data.line.id, data.newPrice);
    }
    // Focus géré via souscription à lineUpdatedSuccess$
  }

  // ===== Handlers pour remise globale (depuis ProductListComponent caption) =====

  onRemiseSelected(remise: IRemise): void {
    const currentSale = this.currentSale();
    if (!currentSale) {
      this.notificationService.error('Aucune vente en cours');
      return;
    }

    // TODO: Implémenter l'application de remise globale
    // this.facade.applyGlobalDiscount(remise);
  }

  onAddRemise(): void {
    const currentSale = this.currentSale();
    if (!currentSale) {
      this.notificationService.error('Aucune vente en cours');
      return;
    }

    // Vérifier si l'utilisateur a l'autorisation
    if (this.authorizationService.canApplyDiscount()) {
      // Ouvrir modal de sélection de remise
      this.openRemiseSelectionModal();
    } else {
      // Demander autorisation
      this.requestRemiseAuthorization();
    }
  }

  onRemoveRemise(): void {
    const currentSale = this.currentSale();
    if (!currentSale || !currentSale.remise) {
      return;
    }

    // Vérifier si l'utilisateur a l'autorisation pour supprimer
    if (this.authorizationService.canApplyDiscount()) {
      this.confirmDialog().onConfirm(
        () => this.facade.updateRemise(undefined),
        'Supprimer la remise',
        'Voulez-vous vraiment supprimer la remise appliquée?',
      );
    } else {
      // Demander autorisation pour supprimer
      this.requestRemiseRemovalAuthorization();
    }
  }

  private requestRemiseAuthorization(): void {
    const saleId = this.currentSale()?.id;
    const saleType = this.selectedSaleType();

    this.authorizationService
      .requestDiscountAuthorization(saleId, saleType)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(authorized => {
        if (authorized) {
          this.openRemiseSelectionModal();
        }
      });
  }

  private requestRemiseRemovalAuthorization(): void {
    const saleId = this.currentSale()?.id;
    const saleType = this.selectedSaleType();

    this.authorizationService
      .requestDiscountAuthorization(saleId, saleType)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(authorized => {
        if (authorized) {
          this.facade.updateRemise(undefined);
        }
      });
  }

  private openRemiseSelectionModal(): void {
    // TODO: Implémenter modal de sélection de remise
    // Pour l'instant, utilisons une remise par défaut pour tester
    this.notificationService.info('Sélection de remise', 'Fonctionnalité de sélection de remise à venir');

    // Exemple de remise (à remplacer par une vraie modal)
    // const remise: IRemise = { id: 1, valeur: 10, remiseValue: 10, type: 'POURCENTAGE', typeLibelle: '10%' };
    // this.facade.updateRemise(remise);
  }

  // ===== Handlers pour CustomerSelectorComponent =====

  /**
   * Délègue au mixin customerHandling
   */
  onCustomerSearchChange(searchTerm: string): void {
    this.customerHandling.searchCustomers(searchTerm);
  }

  /**
   * Délègue au mixin customerHandling
   */
  onCustomerSelected(customer: ICustomer): void {
    this.customerHandling.selectCustomer(customer);
  }

  onCustomerRemoved(): void {
    const hasSale = !!this.facade.currentSale()?.saleId;
    this.customerHandling.removeCustomer();

    // Si pas de vente en cours, focus immédiat (pas d'appel API)
    // Sinon, le focus est géré via souscription à customerRemovedSuccess$
    if (!hasSale) {
      this.focusProductSearch();
    }
  }

  /**
   * Délègue au mixin customerHandling
   */
  onCustomerAdd(): void {
    this.customerHandling.openCustomerFormModal(null, {
      title: 'CRÉATION CLIENT STANDARD',
    });
  }

  // ===== Handlers pour SaleActionsComponent =====

  /**
   * Appelé par le bouton Save
   * Construit l'événement de paiement et délègue à la validation commune
   */
  onSave(): void {
    // Construire l'événement de paiement depuis le composant payment-mode
    const event = this.paymentHandling.buildPaymentEvent();
    if (event) {
      this.validateAndProcessPayment(event);
    }
  }

  /**
   * Appelé quand l'utilisateur valide le paiement depuis le composant payment-mode (Enter dans input)
   * Délègue à la validation commune
   */
  onPaymentComplete(event: PaymentCompleteEvent): void {
    this.validateAndProcessPayment(event);
  }

  /**
   * Méthode unifiée pour la validation et le traitement du paiement
   * Gère le flux avoir avec vérification client, puis délègue au mixin paymentHandling
   * Utilisée par onSave() et onPaymentComplete() pour avoir le même comportement
   */
  private validateAndProcessPayment(event: PaymentCompleteEvent): void {
    // Vérifier si la vente peut être sauvegardée
    if (!this.paymentHandling.validateSaleForSave()) {
      return;
    }

    const isAvoir = this.facade.isAvoir();
    const hasCustomer = this.facade.hasCustomer();

    // Si c'est un avoir ET pas encore confirmé, afficher un dialogue de confirmation
    if (isAvoir && !this.avoirConfirmed()) {
      this.confirmDialog().onConfirm(
        () => {
          // Marquer l'avoir comme confirmé pour ne plus afficher le dialogue
          this.avoirConfirmed.set(true);

          // Après confirmation avoir, vérifier si client associé
          if (!hasCustomer) {
            // Stocker l'événement de paiement pour le récupérer après sélection client
            this.pendingPaymentEvent.set(event);
            // Ouvrir modal sélection client pour avoir
            this.openCustomerModal(false);
          } else {
            this.paymentHandling.processPayment(event);
          }
        },
        'Avoir détecté',
        'Cette vente sera enregistrée comme AVOIR (quantité demandée ≠ quantité servie). Confirmer ?',
      );
    } else {
      // Avoir déjà confirmé ou pas un avoir → traiter directement
      this.paymentHandling.processPayment(event);
    }
  }

  finalizeSale(): void {
    const sale = this.facade.currentSale();
    if (!sale) return;

    // Vérifier si la caisse est ouverte
    if (!this.isCashRegisterOpen()) {
      this.openCashRegister();
      return;
    }

    // Vérifier si avoir (livraison partielle) sans client
    const isAvoir = this.facade.isAvoir();
    const hasCustomer = this.facade.hasCustomer();

    if (isAvoir && !hasCustomer) {
      // Ouvrir le modal de sélection client pour l'avoir
      // Si c'est aussi un différé, c'est déjà marqué dans currentSale.differe
      this.openCustomerModal(false); // false = pour avoir (pas initialement différé)
      return;
    }

    // Sauvegarder la vente (utiliser saveSale, pas createComptantSale)
    this.facade
      .saveSale()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: result => {
          if (result) {
            // Clear customer display after sale
            this.customerDisplay.clear();
            // Réinitialiser pour nouvelle vente après sauvegarde
            this.resetForNewSale();
          }
        },
        error: () => {
          // L'erreur est déjà gérée par le facade
        },
      });
  }

  /**
   * Callback appelé par le mixin quand une vente différée est confirmée
   * Gère la sélection client si nécessaire
   */
  private handleDiffereConfirmed(): void {
    const currentSale = this.facade.currentSale();
    if (!currentSale) return;

    // Vérifier si un client est déjà associé
    if (!currentSale.customerId) {
      // Pas de client → Ouvrir modal sélection client
      this.openCustomerModal(true); // true = pour différé
    } else {
      // Client déjà présent, marquer directement en différé
      currentSale.differe = true;
      this.isDiffere.set(true); // Mettre à jour le signal pour afficher le champ commentaire
    }
  }

  onSaveAndPrint(): void {
    const sale = this.facade.currentSale();
    if (!sale || !this.canSave()) {
      this.notificationService.warning('Vente invalide', "Veuillez ajouter au moins un produit avant d'enregistrer la vente");
      return;
    }

    // Activer l'impression automatique après la sauvegarde
    this.facade.setPrintReceipt(true);

    // Sauvegarder (l'impression sera déclenchée après succès)
    this.facade
      .saveSale()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: result => {
          if (result?.saleId) {
            this.facade.printReceipt(result.saleId);
            this.resetForNewSale();
          }
        },
        error: () => {
          // L'erreur est déjà gérée par le facade
        },
      });
  }

  onPrint(): void {
    const sale = this.currentSale();
    if (sale?.saleId) {
      this.facade.printCurrentSale();
    } else {
      this.notificationService.warning('Impression impossible', 'La vente doit être enregistrée avant impression');
    }
  }

  onPrintInvoice(): void {
    const sale = this.currentSale();
    const hasCustomer = this.selectedCustomer();

    if (!sale?.saleId) {
      this.notificationService.warning('Impression impossible', 'La vente doit être enregistrée avant impression');
      return;
    }

    if (!hasCustomer) {
      this.notificationService.warning('Client requis', 'Un client doit être associé à la vente pour imprimer la facture');
      return;
    }

    this.facade.printInvoice(sale.saleId);
  }

  onCancel(): void {
    const hasLines = this.salesLines().length > 0;

    if (hasLines) {
      // Si la vente a des lignes, demander confirmation
      this.confirmDialog().onConfirm(
        () => {
          this.facade.cancelSale(); // Reset géré via souscription à cancelSaleSuccess$
        },
        'Annulation de la vente',
        'Voulez-vous vraiment annuler cette vente ? Toutes les données seront perdues.',
      );
    } else {
      // Si pas de lignes, juste reset l'UI (pas besoin de cancelSale)
      this.resetForNewSale();
    }
  }

  /**
   * Méthode unifiée pour les avoirs ET les ventes différées
   * @param isForDiffere - true si c'est pour une vente différée, false si c'est pour un avoir
   */
  private openCustomerModal(isForDiffere: boolean): void {
    const currentSale = this.facade.currentSale();
    const isAvoir = this.facade.isAvoir();

    // Déterminer le titre du modal selon le contexte
    let modalTitle = 'SÉLECTION CLIENT';
    if (isAvoir && currentSale?.differe) {
      modalTitle += ' - Avoir avec règlement différé';
    } else if (isAvoir) {
      modalTitle += ' - Avoir (livraison partielle)';
    } else if (isForDiffere) {
      modalTitle += ' - Vente différée';
    }

    const modalRef = this.modalService.open(CustomerSelectionModalComponent, {
      size: 'lg',
      backdrop: 'static',
      centered: true,
    });
    modalRef.componentInstance.modalTitle = modalTitle;

    modalRef.result.then(
      (customer: ICustomer) => {
        if (customer && customer.id) {
          // Assigner le customerId immédiatement sur la vente (évite race condition avec API async)
          if (currentSale) {
            currentSale.customerId = customer.id;

            // Si c'est pour un différé, marquer comme différé
            if (isForDiffere) {
              currentSale.differe = true;
              this.isDiffere.set(true);
            }
          }

          // Pour avoir sans différé → Attendre que le client soit vraiment associé avant de finaliser
          // Sinon l'appel API de setCustomer n'est pas terminé quand finalizeSale() est appelé
          if (isAvoir && !currentSale?.differe) {
            // S'abonner au succès de l'association client AVANT d'appeler setCustomer
            this.facade.customerSetSuccess$.pipe(take(1)).subscribe(() => {
              // Après association client réussie, continuer avec le paiement
              // Utiliser l'événement de paiement en attente si disponible (évite de reconstruire et re-montrer des dialogs)
              const pendingEvent = this.pendingPaymentEvent();
              if (pendingEvent) {
                this.pendingPaymentEvent.set(null);
                this.paymentHandling.processPayment(pendingEvent);
              } else {
                // Sinon construire un nouvel événement (cas du bouton Save)
                this.paymentHandling.onSave();
              }
            });
          }

          // Synchroniser avec la facade (appel API asynchrone)
          this.facade.setCustomer(customer);

          // Si différé → Focus sur commentaire après un délai (pas besoin d'attendre customerSetSuccess)
          if (currentSale?.differe) {
            setTimeout(() => {
              this.paymentMode()?.focusCommentInput();
            }, 100);
          }
        } else {
          const message = isAvoir
            ? 'Un client est obligatoire pour un avoir (livraison partielle)'
            : 'Un client est obligatoire pour une vente différée';
          this.notificationService.warning('Client requis', message);
        }
      },
      () => {
        // Modal fermé sans sélection
        const message = isAvoir
          ? 'Un client est obligatoire pour un avoir (livraison partielle)'
          : 'Un client est obligatoire pour une vente différée';
        this.notificationService.warning('Vente annulée', message);
      },
    );
  }

  /**
   * Sauvegarde la vente après confirmation de vente différée
   * Cette méthode ne repasse pas par processPaymentValidation pour éviter la boucle
   */
  onSaveAsPresale(): void {
    const sale = this.currentSale();
    if (!sale || this.salesLines().length === 0) {
      this.notificationService.warning('Vente vide', 'Ajoutez au moins un produit avant de mettre en attente');
      return;
    }

    // Mettre la vente en attente via le facade
    // Le composant s'abonne déjà à standbySuccess$ dans ngOnInit
    this.facade.putOnStandby();
  }

  /**
   * Réinitialiser pour une nouvelle vente
   * Appelé après sauvegarde ou annulation pour rester sur l'écran de vente
   * NOTE: Ne PAS appeler cancelSale() ici car:
   * - Après sauvegarde: la vente est déjà reset par saveSale()
   * - Après annulation: cancelSale() doit être appelé AVANT resetForNewSale()
   */
  private resetForNewSale(): void {
    // Reset UI uniquement
    this.customerDisplay.clear();
    this.customers.set([]);
    this.selectedLineId.set(null);
    this.isDiffere.set(false); // Reset signal vente différée
    this.avoirConfirmed.set(false); // Reset flag confirmation avoir
    this.pendingPaymentEvent.set(null); // Reset événement de paiement en attente
    // Reset le state du facade (sans appeler cancelSale)

    // Reset product search component
    setTimeout(() => {
      this.productSearchComponent()?.reset();
      this.productSearchComponent()?.getFocus();
    }, 100);
  }

  /**
   * Ouvre le modal de formulaire de caisse
   * Utilisé pour enregistrer le montant en caisse avant de finaliser la vente
   */
  private openCashRegister(): void {
    showCommonModal(this.modalService, CashRegisterFormComponent, {}, (resp: boolean) => {
      if (resp) {
        // Notifier le parent que la caisse est maintenant ouverte
        this.cashRegisterOpened.emit();
        // Réessayer la finalisation après ouverture de la caisse
        setTimeout(() => this.finalizeSale(), 100);
      }
    });
  }

  // ===== Getters pour le template =====

  get itemCount(): number {
    return this.salesLines().length;
  }

  get canPrint(): boolean {
    return !!this.facade.currentSale()?.id;
  }

  // ===== Handler pour le changement de type de vente =====

  onSaleTypeChange(saleType: SaleType): void {
    const currentSale = this.currentSale();
    const hasLines = this.salesLines().length > 0;

    // Si le type de vente change et qu'il y a déjà des lignes, demander confirmation
    if (hasLines && saleType !== this.selectedSaleType()) {
      this.confirmDialog().onConfirm(
        () => this.proceedWithSaleTypeChange(saleType, currentSale),
        'Changement de type de vente',
        `Voulez-vous vraiment changer le type de vente vers ${saleType}?\n\nAttention: Les données actuelles seront perdues.`,
      );
      return;
    }

    this.proceedWithSaleTypeChange(saleType, currentSale);
  }

  private proceedWithSaleTypeChange(saleType: SaleType, currentSale: any): void {
    // Changer le type sélectionné
    this.selectedSaleType.set(saleType);

    // Annuler la vente actuelle
    if (currentSale) {
      this.facade.cancelSale();
    }

    // Note: La vente sera créée au premier produit ajouté (pas de vente vide)
    // Créer une nouvelle vente selon le type
    switch (saleType) {
      case 'COMPTANT':
        // Vente créée au premier produit ajouté
        break;

      case 'ASSURANCE':
      case 'CARNET':
        // Ces types seront implémentés en Phase 8
        this.notificationService.info(`Type de vente: ${saleType}`, 'Fonctionnalité disponible en Phase 8');
        // Revenir au type COMPTANT
        this.selectedSaleType.set('COMPTANT');
        break;
    }
  }

  // ===== Gestion de la sidebar =====

  toggleSidebar(): void {
    this.sidebarCollapsed.update(collapsed => !collapsed);
  }

  // ===== Gestion des ventes en attente =====

  openPendingSales(): void {
    this.pendingSalesSidebar.set(true);
  }

  closePendingSales(): void {
    this.pendingSalesSidebar.set(false);
  }

  onSaleResumed(sale: ISales): void {
    this.pendingSalesSidebar.set(false);
    // The sale is already loaded by PendingSalesListComponent via CurrentSaleService

    // Charger les données de la vente en attente
    this.onLoadPrevente(sale);
  }

  /**
   * Charge une vente en attente (prevente)
   * Restaure l'état de la vente: client, remise, etc.
   */
  onLoadPrevente(sale: ISales): void {
    // La vente est déjà chargée dans le store par le facade
    // Cette méthode sert à restaurer l'état UI spécifique

    // Si la vente a un client, le charger
    if (sale.customer) {
      this.facade.setCustomer(sale.customer);
    }

    // Si la vente a une remise, l'afficher
    if (sale.remise) {
      // La remise est déjà dans la vente, pas besoin de la réappliquer
      console.log('Remise chargée:', sale.remise);
    }

    // Restaurer le type de vente si différent
    if (sale.type && sale.type !== 'VNO') {
      console.log('Type de vente chargé:', sale.type);
    } else {
      // Retour du focus sur le champ produit
      this.focusProductSearch();
    }
  }

  onClosePendingSalesDrawer(): void {
    this.pendingSalesSidebar.set(false);
  }

  // ===== Gestion du vendeur =====

  onSellerChange(seller: IUser): void {
    this.selectedSeller.set(seller);
    // Utiliser le facade pour définir le vendeur dans le store centralisé
    this.facade.setSeller(seller);
    // Le vendeur sera associé à la vente lors de la sauvegarde

    // Retour du focus sur le champ produit
    this.focusProductSearch();
  }

  loadPendingSalesCount(): void {
    // Charger le nombre de ventes en attente depuis le backend
    // Note: Le service SalesApiService.countPendingSales() sera ajouté en Phase 8
    // Pour l'instant, on initialise à 0 (pas de ventes en attente)
    this.pendingSalesCount.set(0);

    // Future implementation:
    // this.salesApiService.countPendingSales()
    //   .pipe(takeUntilDestroyed(this.destroyRef))
    //   .subscribe(count => this.pendingSalesCount.set(count));
  }

  // ===== Raccourcis clavier =====

  handleKeyboardEvent(event: KeyboardEvent): void {
    // F2: Focus recherche produit
    if (event.key === 'F2') {
      event.preventDefault();
      // Le focus sera géré par le composant ProductSearchComponent
      this.notificationService.info('Raccourci', 'F2: Focus recherche produit');
    }

    // F5: Ajouter produit sélectionné
    if (event.key === 'F5') {
      event.preventDefault();
      const product = this.selectedProduct();
      if (product) {
        this.facade.addProductToSale(product, 1);
      } else {
        this.notificationService.warning('Aucun produit', "Veuillez sélectionner un produit d'abord");
      }
    }

    // F6: Mettre en attente (Put on standby)
    if (event.key === 'F6') {
      event.preventDefault();
      this.onSaveAsPresale();
    }

    // F7: Annuler la vente
    if (event.key === 'F7') {
      event.preventDefault();
      this.onCancel();
    }

    // F8: Enregistrer la vente
    if (event.key === 'F8') {
      event.preventDefault();
      if (this.canSave()) {
        this.onSave();
      } else {
        this.notificationService.warning('Vente invalide', "Impossible d'enregistrer la vente");
      }
    }

    // F9: Ventes en attente
    if (event.key === 'F9') {
      event.preventDefault();
      this.openPendingSales();
    }

    // F10: Focus mode de paiement
    if (event.key === 'F10') {
      event.preventDefault();
      if (this.currentSale() && this.salesLines().length > 0) {
        // Le composant payment est déjà visible inline, on peut simplement mettre le focus
        // TODO: Ajouter une méthode focus() au PaymentModeComponent si nécessaire
        console.log('F10: Focus mode de paiement');
      } else {
        this.notificationService.warning('Vente vide', 'Ajoutez des produits avant de payer');
      }
    }

    // F11: Focus sélection client (COMPTANT uniquement)
    if (event.key === 'F11') {
      event.preventDefault();
      if (this.selectedSaleType() === 'COMPTANT') {
        // Le customer overlay panel s'ouvrira via un clic programmé ou un signal
        this.notificationService.info('Sélection client', 'Cliquez sur le bouton client dans le header');
      } else {
        this.notificationService.info('Non disponible', 'La sélection client est uniquement pour les ventes COMPTANT');
      }
    }

    // Escape: Annuler
    if (event.key === 'Escape') {
      event.preventDefault();
      this.onCancel();
    }

    // Ctrl+S: Enregistrer
    if ((event.ctrlKey || event.metaKey) && event.key === 's') {
      event.preventDefault();
      if (this.canSave()) {
        this.onSave();
      }
    }

    // Ctrl+P: Enregistrer et imprimer
    if ((event.ctrlKey || event.metaKey) && event.key === 'p') {
      event.preventDefault();
      if (this.canSave()) {
        this.onSaveAndPrint();
      }
    }
  }
}
