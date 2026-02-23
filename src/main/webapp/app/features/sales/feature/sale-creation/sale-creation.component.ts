import {Component, computed, DestroyRef, inject, input, model, OnInit, output, signal, viewChild} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {take} from 'rxjs/operators';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {NgbModal} from '@ng-bootstrap/ng-bootstrap';
import {TooltipModule} from 'primeng/tooltip';
import {Toast} from 'primeng/toast';
import {NgxSpinnerModule, NgxSpinnerService} from 'ngx-spinner';
import {ConfirmDialogComponent} from '../../../../shared/dialog/confirm-dialog/confirm-dialog.component';
import {
  CustomerSelectionModalComponent,
  ProductListComponent,
  ProductSearchSectionComponent,
  SaleActionsComponent,
  SaleSummaryComponent,
  SaleType,
} from '../../ui';
import {PaymentCompleteEvent, PaymentModeComponent} from '../../ui/payment-mode/payment-mode.component';

import {
  CashRegisterFormComponent
} from '../../../../entities/cash-register/user-cash-register/cash-register-form/cash-register-form.component';
import {showCommonModal} from '../../../../entities/sales/selling-home/sale-helper';
import {SalesFacade} from '../../data-access/facades/sales.facade';
import {AuthorizationService} from '../../data-access/services/authorization.service';
import {CustomerDisplayService} from '../../data-access/services/customer-display.service';
import {NotificationService} from '../../../../shared/services/notification.service';
import {ICustomer, IRemise, ISalesLine, ProduitSearch} from '../../../../shared/model';
import {IUser} from '../../../../core/user/user.model';
import {UserVendeurService} from '../../../../entities/sales/service/user-vendeur.service';
import {
  createDeconditionnementHandling,
  createForceStockHandling,
  createKeyboardShortcuts,
  createPaymentHandling,
  createProductHandling,
  createSaleLifecycle,
  ProductSearchHost
} from '../../shared/mixins';
import {SaleForEditInfo} from '../../../../shared/model/sales.model';

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
    ProductSearchSectionComponent,
    ProductListComponent,
    SaleSummaryComponent,
    SaleActionsComponent,
    PaymentModeComponent,
    ConfirmDialogComponent,
    NgxSpinnerModule,
  ],
})
export class SaleCreationComponent implements OnInit, ProductSearchHost {
  productSearchComponent = viewChild<ProductSearchSectionComponent>('produitbox');
  readonly quantityComponent = computed(() => {
    const section = this.productSearchComponent();
    if (!section) {
      return undefined;
    }
    return {
      focusProduitControl: () => section.focusProduitControl(),
      reset: (qty: number) => section.resetQuantity(qty),
    };
  });
  confirmDialog = viewChild.required<ConfirmDialogComponent>('confirmDialog');
  paymentMode = viewChild<PaymentModeComponent>('paymentMode');
  initSaleForEditInfo = model<SaleForEditInfo>(null);
  // Output pour notifier le container du succès de l'ajout (règle métier: reset après succès)
  productAddedSuccess = output<void>();
  cashRegisterOpened = output<void>();
  // Modal and responsive state
  readonly isCashRegisterOpen = input(false);
  readonly isSmallScreen = input(false);
  readonly isPresale = input(false);
  remises = input<IRemise[]>([]);
  showStock = input(false);
  // Local UI state
  customers = signal<ICustomer[]>([]);
  selectedLineId = signal<number | null>(null);
  selectedSaleType = signal<SaleType>('COMPTANT');
  isDiffere = signal<boolean>(false); // Signal local pour forcer la détection de changement
  // Monnaie calculée en temps réel depuis le composant payment-mode
  currentChange = computed(() => {
    const change = this.paymentMode()?.changeAmount() || 0;
    return change > 0 ? change : null;
  });
  canEditPrice = signal<boolean>(false);
  // Force Stock state signals
  waitingForForceStockSuccess = signal<boolean>(false);
  forceStockContext = signal<'addProduct' | 'editCell' | null>(null);
  // UI state for sidebar and pending sales
  sidebarCollapsed = signal(false);
  pendingSalesSidebar = signal(false);
  // Vendeur sélectionné
  selectedSeller = signal<IUser | null>(null);
  // Services
  protected facade = inject(SalesFacade);
  // State depuis le store (signals computed)
  currentSale = this.facade.currentSale;
  salesLines = this.facade.salesLines;
  selectedCustomer = this.facade.selectedCustomer;
  selectedProduct = this.facade.selectedProduct;
  canSave = this.facade.canSave;
  isSaving = this.facade.isSaving;
  loading = this.facade.loading;
  protected userVendeurService = inject(UserVendeurService);
  private authorizationService = inject(AuthorizationService);
  private notificationService = inject(NotificationService);
  private customerDisplay = inject(CustomerDisplayService);
  private modalService = inject(NgbModal);
  private destroyRef = inject(DestroyRef);
  private spinner = inject(NgxSpinnerService);
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
  // Computed pour convertir l'input isCashRegisterOpen en Signal<boolean>
  private isCashRegisterOpenSignal = computed(() => this.isCashRegisterOpen() ?? false);
  // ===== Force Stock Handling Mixin =====
  private forceStockHandling = createForceStockHandling({
    facade: this.facade,
    authorizationService: this.authorizationService,
    spinner: this.spinner,
    config: {saleType: 'COMPTANT'},
    currentSale: this.facade.currentSale,
    loading: this.facade.loading,
    lastError: this.facade.lastError,
    waitingForForceStockSuccess: this.waitingForForceStockSuccess,
    forceStockContext: this.forceStockContext,
    getConfirmDialog: () => this.confirmDialog(),
    resetProductSelection: () => this.productHandling.resetProductSelection(),
    operations: {
      createSale: (line: ISalesLine) => this.facade.createComptantSale(line),
      addProduct: (line: ISalesLine) => this.facade.onAddProduit(line),
    },
  });
  // ===== Deconditionnement Handling Mixin =====
  private deconditionnementHandling = createDeconditionnementHandling({
    facade: this.facade,
    waitingForForceStockSuccess: this.waitingForForceStockSuccess,
    getConfirmDialog: () => this.confirmDialog(),
    resetProductSelection: () => this.productHandling.resetProductSelection(),
    operations: {
      createSale: (line: ISalesLine) => this.facade.createComptantSale(line),
      addProduct: (line: ISalesLine) => this.facade.onAddProduit(line),
    },
  });
  // ===== Keyboard Shortcuts Mixin =====
  private keyboardShortcuts = createKeyboardShortcuts(
    {saleType: 'COMPTANT', isPresale: () => this.isPresale()},
    {
      focusProductSearch: () => this.productHandling.focusProductSearch(),
      focusQuantity: () => this.productSearchComponent()?.focusProduitControl(),
      focusCustomer: () => {
        // TODO: ouvrir overlay client si nécessaire
      },
      addProduct: () => {
        const product = this.selectedProduct();
        if (product) {
          this.facade.addProductToSale(product, 1);
        }
      },
      clearProduct: () => this.productHandling.resetProductSelection(),
      finalizeSale: () => this.onSave(),
      putOnStandby: () => this.onPutOnHold(),
      cancelSale: () => this.onCancel(),
      focusPayment: () => this.paymentMode()?.focusFirstMode(),
      printReceipt: () => this.onPrint(),
      saveAsPresale: () => this.onSaveAsPresale(true),
      savePresale: () => this.onSaveAsPresale(false),
    },
  );
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
      if (!comp) {
        return undefined;
      }
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
  // ===== Sale Lifecycle Mixin =====
  private lifecycle = createSaleLifecycle({
    facade: this.facade,
    destroyRef: this.destroyRef,
    productHandling: this.productHandling,
    resetForNewSale: () => this.resetForNewSale(),
    onProductAddedExtra: () => this.productAddedSuccess.emit(),
  });

  constructor() {
    // Initialiser les effects de gestion du forçage de stock via le mixin
    this.forceStockHandling.initializeEffects();
    // Initialiser les effects de déconditionnement (après force-stock)
    this.deconditionnementHandling.initializeEffects();
  }

  /**
   * Méthode publique pour mettre le focus sur la recherche produit
   * Appelée par le composant parent lors du changement de tab
   */
  public focusProductSearch(): void {
    setTimeout(() => {
      this.productSearchComponent()?.getFocus();
    }, 100);
  }

  ngOnInit(): void {
    // Initialize customer display
    this.customerDisplay.initialize('PHARMA SMART');

    // Initialiser les souscriptions communes via le mixin lifecycle
    this.lifecycle.initializeSubscriptions();

    // Souscription spécifique: suppression du client (uniquement vente comptant)
    this.facade.customerRemovedSuccess$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      this.productHandling.focusProductSearch();
    });

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
    if (!shouldSave) {
      return;
    }

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
      this.facade.updateLineQuantitySold(data.line.id, data.newQty);
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
      if (this.authorizationService.canDeleteProduct()) {
        this.facade.removeLine(event.line.saleLineId);
      } else {
        this.authorizationService
          .requestDeleteProductAuthorization(saleId, saleType)
          .pipe(takeUntilDestroyed(this.destroyRef))
          .subscribe(authorized => {
            if (authorized && event.line.saleLineId) {
              this.facade.removeLine(event.line.saleLineId);
            }
          });
      }
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

    if (this.authorizationService.canApplyDiscount()) {
      this.facade.updateRemise(remise);
    } else {
      this.authorizationService
        .requestDiscountAuthorization(currentSale.id, this.selectedSaleType())
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe(authorized => {
          if (authorized) {
            this.facade.updateRemise(remise);
          }
        });
    }
  }

  onRemoveRemise(): void {
    const currentSale = this.currentSale();
    if (!currentSale || !currentSale.remise) {
      return;
    }

    const doRemove = () => {
      this.confirmDialog().onConfirm(
        () => this.facade.updateRemise(undefined),
        'Supprimer la remise',
        'Voulez-vous vraiment supprimer la remise appliquée ?',
        undefined,
        () => this.productHandling.focusProductSearch(),
      );
    };

    if (this.authorizationService.canApplyDiscount()) {
      doRemove();
    } else {
      this.authorizationService
        .requestDiscountAuthorization(currentSale.id, this.selectedSaleType())
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe(authorized => {
          if (authorized) {
            doRemove();
          }
        });
    }
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

  finalizeSale(): void {
    const sale = this.facade.currentSale();
    if (!sale) {
      return;
    }

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
   * Sauvegarde la vente après confirmation de vente différée
   * Cette méthode ne repasse pas par processPaymentValidation pour éviter la boucle
   */
  onPutOnHold(): void {
    const sale = this.currentSale();
    if (!sale || this.salesLines().length === 0) {
      this.notificationService.warning('Vente vide', 'Ajoutez au moins un produit avant de mettre en attente');
      return;
    }

    // Mettre la vente en attente via le facade
    // Le composant s'abonne déjà à standbySuccess$ dans ngOnInit
    this.facade.putOnStandby();
  }

  onSaveAsPresale(transform: boolean = true): void {
    const sale = this.currentSale();
    if (!sale) {
      this.notificationService.warning('Aucune vente à enregistrer', 'Vente vide');
      return;
    }
    if (this.salesLines().length === 0) {
      this.notificationService.warning('Ajoutez au moins un produit', 'Vente vide');
      return;
    }

    const isAvoir = this.facade.isAvoir();
    const hasCustomer = this.facade.hasCustomer();

    if (isAvoir && !this.avoirConfirmed()) {
      this.confirmDialog().onConfirm(
        () => {
          this.avoirConfirmed.set(true);
          if (!hasCustomer) {
            this.openCustomerModalForPresale();
          } else {
            this.doFinalizePresale(transform);
          }
        },
        'Avoir détecté',
        'Cette vente sera enregistrée comme AVOIR (quantité demandée ≠ quantité servie). Confirmer ?',
      );
    } else {
      this.doFinalizePresale(transform);
    }
  }


  onSellerChange(seller: IUser): void {
    this.selectedSeller.set(seller);
    // Utiliser le facade pour définir le vendeur dans le store centralisé
    this.facade.setSeller(seller);
    // Le vendeur sera associé à la vente lors de la sauvegarde

    // Retour du focus sur le champ produit
    this.focusProductSearch();
  }

  handleKeyboardEvent(event: KeyboardEvent): void {
    this.keyboardShortcuts.handleKeyboardEvent(event);
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

  /**
   * Callback appelé par le mixin quand une vente différée est confirmée
   * Gère la sélection client si nécessaire
   */
  private handleDiffereConfirmed(): void {
    const currentSale = this.facade.currentSale();
    if (!currentSale) {
      return;
    }

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

  // ===== Gestion des ventes en attente =====

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
            this.facade.customerSetSuccess$.pipe(take(1), takeUntilDestroyed(this.destroyRef)).subscribe(() => {
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
   * Ouvre le modal de sélection client pour le flux avoir en mode prévente
   * Après sélection, attend l'association client puis finalise la prévente
   */
  private openCustomerModalForPresale(): void {
    const currentSale = this.facade.currentSale();

    const modalRef = this.modalService.open(CustomerSelectionModalComponent, {
      size: 'lg',
      backdrop: 'static',
      centered: true,
    });
    modalRef.componentInstance.modalTitle = 'SÉLECTION CLIENT - Avoir (livraison partielle)';

    modalRef.result.then(
      (customer: ICustomer) => {
        if (customer && customer.id) {
          if (currentSale) {
            currentSale.customerId = customer.id;
          }
          this.facade.customerSetSuccess$.pipe(take(1), takeUntilDestroyed(this.destroyRef)).subscribe(() => {
            this.doFinalizePresale();
          });
          this.facade.setCustomer(customer);
        } else {
          this.notificationService.warning('Client requis', 'Un client est obligatoire pour un avoir (livraison partielle)');
        }
      },
      () => {
        this.notificationService.warning('Vente annulée', 'Un client est obligatoire pour un avoir (livraison partielle)');
      },
    );
  }

  private doFinalizePresale(transform: boolean = true): void {
    const sale = this.currentSale();
    if (!sale) {
      return;
    }

    this.facade
      .finalizePresale(sale, transform)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: result => {
          if (result) {
            this.resetForNewSale();
          }
        },
      });
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
    this.facade.resetCurrentSale();
    this.initSaleForEditInfo.set(null);
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
    }, 150);
  }

  // ===== Gestion du vendeur =====

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

  // ===== Raccourcis clavier =====

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
}
