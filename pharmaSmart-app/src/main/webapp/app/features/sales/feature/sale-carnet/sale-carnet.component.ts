import {
  AfterViewInit,
  Component,
  computed,
  DestroyRef,
  effect,
  inject,
  input,
  model,
  OnInit,
  output,
  signal,
  viewChild,
  ChangeDetectionStrategy
} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {NgxSpinnerModule, NgxSpinnerService} from 'ngx-spinner';
import {NgbModal} from '@ng-bootstrap/ng-bootstrap';
import {NgbConfirmDialogService} from '../../../../shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive';
import {
  AssuredCustomerListModalComponent,
  InsuranceDataBarComponent,
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
import {CustomerCarnetComponent} from '../../../../entities/customer/carnet/customer-carnet.component';
import {showCommonModal} from '../../../../entities/sales/selling-home/sale-helper';
import {SalesFacade} from '../../data-access/facades/sales.facade';
import {AuthorizationService} from '../../data-access/services/authorization.service';
import {NotificationService} from '../../../../shared/services/notification.service';
import {CustomerDisplayService} from '../../data-access/services/customer-display.service';
import {CustomerSearchService} from '../../data-access/services/customer-search.service';
import {IClientTiersPayant, ICustomer, IRemise, ISalesLine, ProduitSearch} from '../../../../shared/model';
import {
  createCustomerHandling,
  createDeconditionnementHandling,
  createForceStockHandling,
  createKeyboardShortcuts,
  createPaymentHandling,
  createProductHandling,
  createSaleLifecycle,
  ProductSearchHost,
} from '../../shared/mixins';
import {SaleForEditInfo} from '../../../../shared/model/sales.model';

/**
 * SaleCarnetComponent
 *
 * Composant pour les ventes à crédit (CARNET)
 *
 * Fonctionnalités spécifiques:
 * - Sélection client avec compte crédit
 * - Vérification solde/plafond disponible
 * - Validation limite crédit
 * - Enregistrement vente à crédit
 *
 * @example
 * <app-sale-carnet />
 */
@Component({
  selector: 'app-sale-carnet',
  templateUrl: './sale-carnet.component.html',
  styleUrls: ['./sale-carnet.component.scss'],
  host: {
    '(window:keydown)': 'handleKeyboardEvent($event)',
  },
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [
    CommonModule,
    FormsModule,
    ProductSearchSectionComponent,
    InsuranceDataBarComponent,
    ProductListComponent,
    SaleSummaryComponent,
    SaleActionsComponent,
    PaymentModeComponent,
    NgxSpinnerModule,
  ],
})
export class SaleCarnetComponent implements OnInit, AfterViewInit, ProductSearchHost {
  // Services

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
  insuranceDataBar = viewChild<InsuranceDataBarComponent>('insuranceDataBar');
  paymentModeComponent = viewChild<PaymentModeComponent>('paymentMode');
  selectedSaleType = signal<SaleType>('CARNET');
  initSaleForEditInfo = model<SaleForEditInfo>(null);
  showStock = input(false);
  // Modal and responsive state
  readonly isCashRegisterOpen = input(false);
  readonly isSmallScreen = input(false);
  readonly remises = input<IRemise[]>([]);
  readonly isPresale = input(false);
  readonly isDevis = input(false);
  // Outputs
  switchToComptant = output<void>();
  cashRegisterOpened = output<void>();
  // State signals
  readonly saleType = signal<'CARNET'>('CARNET');
  readonly selectedLineId = signal<number | null>(null);
  readonly waitingForForceStockSuccess = signal<boolean>(false);
  readonly forceStockContext = signal<'addProduct' | 'editCell' | null>(null);
  customers = signal<ICustomer[]>([]);
  isDiffere = signal<boolean>(false);
  // Monnaie calculée en temps réel depuis le composant payment-mode
  currentChange = computed(() => {
    const change = this.paymentModeComponent()?.changeAmount() || 0;
    return change > 0 ? change : null;
  });
  // Helper method pour savoir si un client est sélectionné
  hasCustomer = computed(() => !!this.selectedCustomer());
  // Computed signals
  readonly canSave = computed(() => {
    const sale = this.currentSale();
    const lines = this.salesLines();
    const customer = this.selectedCustomer();
    return !!sale && lines.length > 0 && !!customer && !this.isSaving();
  });
  private readonly confirmDialog = inject(NgbConfirmDialogService);
  // Services
  private facade = inject(SalesFacade);
  readonly currentSale = this.facade.currentSale;
  readonly salesLines = this.facade.salesLines;
  readonly selectedCustomer = this.facade.selectedCustomer;
  readonly selectedProduct = this.facade.selectedProduct;
  readonly loading = this.facade.loading;
  readonly isSaving = this.facade.isSaving;
  readonly plafondIsReached = this.facade.plafondIsReached;
  readonly isAvoir = this.facade.isAvoir;
  private authorizationService = inject(AuthorizationService);
  private notificationService = inject(NotificationService);
  private customerDisplay = inject(CustomerDisplayService);
  private customerSearchService = inject(CustomerSearchService);
  private spinner = inject(NgxSpinnerService);
  private modalService = inject(NgbModal);
  private destroyRef = inject(DestroyRef);
  // Computed pour convertir l'input isCashRegisterOpen en Signal<boolean>
  private isCashRegisterOpenSignal = computed(() => this.isCashRegisterOpen() ?? false);
  // ===== Product Handling Mixin =====
  private productHandling = createProductHandling({
    facade: this.facade,
    customerDisplay: this.customerDisplay,
    notificationService: this.notificationService,
    host: this,
    config: {
      requiresCustomer: true,
      customerRequiredMessage: "Veuillez sélectionner un client avant d'ajouter des produits",
      saleType: 'CARNET',
    },
    selectedProduct: this.facade.selectedProduct,
    currentSale: this.facade.currentSale,
    hasCustomer: this.hasCustomer,
    createSale: (line: ISalesLine) => this.facade.createCarnetSale(line),
    addProduct: (line: ISalesLine) => this.facade.onAddProduitCarnet(line),
  });
  // ===== Force Stock Handling Mixin =====
  private forceStockHandling = createForceStockHandling({
    facade: this.facade,
    authorizationService: this.authorizationService,
    config: {saleType: 'CARNET'},
    currentSale: this.facade.currentSale,
    loading: this.facade.loading,
    lastError: this.facade.lastError,
    waitingForForceStockSuccess: this.waitingForForceStockSuccess,
    forceStockContext: this.forceStockContext,
    getConfirmDialog: () => this.confirmDialog,
    resetProductSelection: () => this.productHandling.resetProductSelection(),
    operations: {
      createSale: (line: ISalesLine) => this.facade.createCarnetSale(line),
      addProduct: (line: ISalesLine) => this.facade.onAddProduitCarnet(line),
    },
  });
  // ===== Deconditionnement Handling Mixin =====
  private deconditionnementHandling = createDeconditionnementHandling({
    facade: this.facade,
    waitingForForceStockSuccess: this.waitingForForceStockSuccess,
    getConfirmDialog: () => this.confirmDialog,
    resetProductSelection: () => this.productHandling.resetProductSelection(),
    operations: {
      createSale: (line: ISalesLine) => this.facade.createCarnetSale(line),
      addProduct: (line: ISalesLine) => this.facade.onAddProduitCarnet(line),
    },
  });
  // ===== Customer Handling Mixin =====
  private customerHandling = createCustomerHandling({
    facade: this.facade,
    customerSearchService: this.customerSearchService,
    notificationService: this.notificationService,
    modalService: this.modalService,
    config: {
      saleType: 'CARNET',
      customerRequired: true,
      customerRequiredMessage: 'Un client avec compte crédit est obligatoire pour une vente CARNET',
    },
    selectedCustomer: this.facade.selectedCustomer,
    customers: this.customers,
    customerListComponent: AssuredCustomerListModalComponent,
    customerFormComponent: CustomerCarnetComponent,
    onCustomerSelectedCallback: customer => {
      // Mettre à jour les tiers payants via la facade
      if (customer.tiersPayants && customer.tiersPayants.length > 0) {
        this.facade.updateSaleTiersPayants(customer.tiersPayants);
      }

      // Initialiser les tiers payants dans le composant UI
      this.insuranceDataBar()?.initializeFromCustomer(customer);

      // Focus sur le premier champ de numéro de bon
      setTimeout(() => this.insuranceDataBar()?.focusFirstBon(), 100);
    },
  });
  // ===== Payment Handling Mixin =====
  private paymentHandling = createPaymentHandling({
    facade: this.facade,
    notificationService: this.notificationService,
    customerDisplay: this.customerDisplay,
    config: {
      saleType: 'CARNET',
      toleranceThreshold: 5, // Seuil de 5 FCFA pour CARNET
      allowDiffere: true, // Vente différée autorisée pour CARNET
    },
    currentSale: this.facade.currentSale,
    salesLines: this.facade.salesLines,
    canSave: this.canSave,
    isCashRegisterOpen: this.isCashRegisterOpenSignal,
    getPaymentModeComponent: () => {
      const comp = this.paymentModeComponent();
      if (!comp) {
        return undefined;
      }
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
      this.confirmDialog.onConfirm(onConfirm, title, message, undefined, onCancel),
    onPaymentSuccess: () => {
    },
    onDiffereConfirmed: () => this.handleDiffereConfirmed(),
  });
  // ===== Keyboard Shortcuts Mixin =====
  private keyboardShortcutsMixin = createKeyboardShortcuts(
    {saleType: 'CARNET', isPresale: () => this.isPresale()},
    {
      focusProductSearch: () => this.focusProductSearch(),
      focusQuantity: () => this.productSearchComponent()?.focusProduitControl(),
      focusCustomer: () => {
        setTimeout(() => this.insuranceDataBar()?.searchInput()?.nativeElement.focus(), 100);
      },
      addProduct: () => {
        const product = this.selectedProduct();
        if (product) {
          this.productHandling.onAddQuantity(1);
        }
      },
      clearProduct: () => this.productHandling.resetProductSelection(),
      finalizeSale: () => this.onSave(),
      putOnStandby: () => this.onPutOnHold(),
      cancelSale: () => this.onCancel(),
      focusPayment: () => this.paymentModeComponent()?.focusFirstMode(),
      saveAsPresale: () => this.onSaveAsPresale(true),
      savePresale: () => this.onSaveAsPresale(false),
    },
  );
  // ===== Sale Lifecycle Mixin =====
  private lifecycle = createSaleLifecycle({
    facade: this.facade,
    destroyRef: this.destroyRef,
    productHandling: this.productHandling,
    resetForNewSale: () => this.resetForNewSale(),
    onResumePendingSale: () => {
      const currentSale = this.currentSale();
      if (currentSale?.saleId) {
        const updatedTiersPayants = currentSale.tiersPayants || [];
        this.insuranceDataBar()?.updateTiersPayants(updatedTiersPayants);
        setTimeout(() => this.productHandling.focusProductSearch(), 200);
      }
    },
  });

  constructor() {
    // Initialiser les effects de gestion du forçage de stock via le mixin
    this.forceStockHandling.initializeEffects();
    // Initialiser les effects de déconditionnement (après force-stock)
    this.deconditionnementHandling.initializeEffects();
    this.initializeEffects();
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
    // Initialiser une vente CARNET (ou DEVIS CARNET)
    if (this.isDevis()) {
      this.facade.initializeDevisCarnetSale();
    } else {
      this.facade.initializeCarnetSale();
    }

    // Initialize typePrescription with default value
    this.facade.setTypePrescription('PRESCRIPTION');

    // Initialize customer display
    this.customerDisplay.initialize('PHARMA SMART');

    // Initialiser les souscriptions communes via le mixin lifecycle
    this.lifecycle.initializeSubscriptions();
  }

  ngAfterViewInit(): void {
    // Force le focus sur la recherche client au chargement initial
    setTimeout(() => {
      this.insuranceDataBar()?.searchInput()?.nativeElement.focus();
    }, 200);
  }

  // ===== Handlers pour InsuranceDataBarComponent =====

  onProductSearchEnter(shouldSave: boolean): void {
    if (!shouldSave) {
      return;
    }

    const currentSale = this.currentSale();

    //  Si vente en cours avec des lignes
    if (currentSale && this.salesLines().length > 0) {
      const amountToBePaid = currentSale.amountToBePaid || 0;

      //  Si montant à payer <= 0, finaliser directement sans paiement
      if (amountToBePaid <= 0) {

        this.confirmDialog.onConfirm(
          () => {
            this.finalizeSaleWithoutPayment();
          },
          'Finaliser la vente',
          'Voulez-vous vraiment finaliser la vente ?', null,
          () => this.productSearchComponent()?.getFocus()
        );
      } else {
        // Montant à payer > 0, le payment-mode est déjà affiché en inline
        // Afficher le total sur l'écran client
        const total = currentSale.salesAmount || 0;
        this.customerDisplay.updateDisplayForTotal(total);

        // Focus sur le champ cash
        setTimeout(() => {
          this.paymentModeComponent()?.focusFirstMode();
        }, 100);
      }
    }
  }

  private initializeEffects(): void {
    this.setupSavingStateEffect();
  }

  /**
   * Effect pour contrôler le spinner selon l'état de sauvegarde
   */
  private setupSavingStateEffect(): void {
    effect(() => {
      const active = this.facade.loading() || this.isSaving();
      active ? this.spinner.show('sale-spinner') : this.spinner.hide('sale-spinner');
    });
  }

  onCustomerSelectedFromBar(customer: ICustomer): void {
    // Cloner l'objet pour forcer la réactivité
    //TODO: on doit avoir un seul tiers payant pour le carnet, donc on peut simplifier en ne prenant que le premier élément de la liste
    const newCustomer = {...customer, tiersPayants: [...(customer.tiersPayants || [])]};
    this.customerHandling.selectCustomer(newCustomer);
  }

  onOpenCustomerList(event?: { customers: ICustomer[]; searchTerm: string }): void {
    showCommonModal(
      this.modalService,
      AssuredCustomerListModalComponent,
      {
        searchString: event?.searchTerm || '',
        headerLibelle: 'CLIENTS CARNET',
        typeTiersPayant: 'CARNET',
        preloadedCustomers: event?.customers?.length ? event.customers : null,
      },
      (customer: ICustomer) => {
        if (customer) {
          this.customerHandling.selectCustomer(customer);
        }
      },
      '70%',
      'modal-dialog-70',
    );
  }

  onEditCustomer(): void {
    const customer = this.selectedCustomer();
    if (customer) {
      this.openCarnetCustomerForm(customer);
    }
  }

  onAddCustomer(): void {
    this.openCarnetCustomerForm(null);
  }

  onTiersPayantsChanged(tiersPayants: IClientTiersPayant[]): void {
    this.facade.updateSaleTiersPayants(tiersPayants);
  }

  // ===== Product Management =====

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
  onProductScanned(product: ProduitSearch, codeScan?: string): void {
    this.productHandling.onProductScanned(product, codeScan);
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

  onLineSelected(line: ISalesLine): void {
    this.selectedLineId.set(line.id || null);
  }

  onAuthorizationRequired(event: { line: ISalesLine; action: 'delete' | 'discount' }): void {
    const saleId = this.currentSale()?.id;
    const saleType = this.selectedSaleType();
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
  }

  // ===== Sale Actions =====

  onSave(): void {
    // Pré-validation: rebuilder les tiers payants avant de sauvegarder
    this.rebuildTiersPayantsFromInputs();

    // Si c'est un avoir, confirmer
    if (this.isAvoir()) {
      this.confirmDialog.onConfirm(
        () => this.paymentHandling.onSave(),
        'Avoir détecté',
        'Cette vente sera enregistrée comme AVOIR (quantité demandée ≠ quantité servie). Confirmer ?',
      );
    } else {
      // Déléguer au mixin paymentHandling
      this.paymentHandling.onSave();
    }
  }

  onPutOnHold(): void {
    const sale = this.currentSale();
    if (!sale || this.salesLines().length === 0) {
      this.notificationService.error('Ajoutez au moins un produit', 'Vente vide');
      return;
    }

    if (!this.hasCustomer()) {
      this.notificationService.error('Un client assuré est obligatoire', 'Client requis');
      return;
    }

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
    if (!this.hasCustomer()) {
      this.notificationService.error('Un client est obligatoire', 'Client requis');
      return;
    }

    // Rebuilder les tiers payants avec les numBon des inputs
    this.rebuildTiersPayantsFromInputs();

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

  onSaveAsDevis(): void {
    const sale = this.currentSale();
    if (!sale) {
      this.notificationService.warning('Aucun devis à enregistrer', 'Devis vide');
      return;
    }
    if (this.salesLines().length === 0) {
      this.notificationService.warning('Ajoutez au moins un produit', 'Devis vide');
      return;
    }
    if (!this.hasCustomer()) {
      this.notificationService.error('Un client est obligatoire pour un devis', 'Client requis');
      return;
    }

    // Rebuilder les tiers payants avec les numBon des inputs
    this.rebuildTiersPayantsFromInputs();

    this.facade
      .saveDevisCarnet(sale)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: result => {
          if (result) {
            this.resetForNewSale();
          }
        },
      });
  }

  onCancel(): void {
    // Confirm before canceling
    if (this.salesLines().length > 0) {
      this.confirmDialog.onConfirm(
        () => this.facade.cancelSale(),
        'Annulation de la vente',
        'Êtes-vous sûr de vouloir annuler cette vente ?',
      );
    } else {
      this.resetForNewSale();
    }
  }

  handleKeyboardEvent(event: KeyboardEvent): void {
    this.keyboardShortcutsMixin.handleKeyboardEvent(event);
  }

  /**
   * Appelé quand l'utilisateur valide le paiement depuis le composant payment-mode
   * Délègue au mixin paymentHandling
   */
  onPaymentComplete(event: PaymentCompleteEvent): void {
    // Pré-validation: rebuilder les tiers payants avec les numBon des inputs
    this.rebuildTiersPayantsFromInputs();

    // Déléguer au mixin paymentHandling
    this.paymentHandling.processPayment(event);
  }

  onPaymentError(error: string): void {
    this.notificationService.error('Erreur', error);
  }

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
      this.confirmDialog.onConfirm(
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

  // ===== Raccourcis clavier =====

  /**
   * Rebuilde les tiers payants avec les numBon des inputs et met à jour la vente
   * Doit être appelé avant toute finalisation de vente CARNET
   */
  private rebuildTiersPayantsFromInputs(): void {
    const tiersPayantsFromInputs = this.insuranceDataBar()?.buildIClientTiersPayantFromInputs() || [];
    if (tiersPayantsFromInputs.length > 0) {
      this.facade.updateSaleTiersPayants(tiersPayantsFromInputs);
    }
  }

  // ===== Payment =====

  /**
   * Finalise la vente sans paiement (amountToBePaid <= 0)
   * Délègue au mixin paymentHandling après pré-validation
   */
  private finalizeSaleWithoutPayment(): void {
    // Rebuilder les tiers payants avec les numBon des inputs
    this.rebuildTiersPayantsFromInputs();

    // Déléguer au mixin
    this.paymentHandling.finalizeSaleWithoutPayment();
  }

  private handleDiffereConfirmed(): void {
    const currentSale = this.facade.currentSale();
    if (!currentSale) {
      return;
    }
    currentSale.differe = true;
    this.isDiffere.set(true);
    setTimeout(() => {
      this.paymentModeComponent()?.focusCommentInput();
    }, 100);
  }

  // ===== Handlers pour remise globale (depuis ProductListComponent caption) =====

  /**
   * Réinitialiser pour une nouvelle vente
   * Appelé après sauvegarde ou annulation pour rester sur l'écran de vente
   */
  private resetForNewSale(): void {
    this.customerDisplay.clear();
    this.initSaleForEditInfo.set(null);
    this.selectedLineId.set(null);
    this.customers.set([]);
    this.isDiffere.set(false);
    this.facade.resetCurrentSale();
    this.switchToComptant.emit();
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
        // Finaliser la vente via le mixin après ouverture de la caisse
        setTimeout(() => this.paymentHandling.completeSale(), 100);
      }
    });
  }

  /**
   * Ouvre le formulaire de création/édition client carnet
   */
  private openCarnetCustomerForm(customer: ICustomer | null): void {
    showCommonModal(
      this.modalService,
      CustomerCarnetComponent,
      {
        entity: customer,
        title: customer ? 'MODIFICATION CLIENT CARNET' : 'NOUVEAU CLIENT CARNET',
        categorie: 'CARNET',
      },
      (updatedCustomer: ICustomer) => {
        if (updatedCustomer) {
          this.customerHandling.selectCustomer(updatedCustomer);
        }
      },
      'xl',
      'modal-dialog-80',
    );
  }
}
