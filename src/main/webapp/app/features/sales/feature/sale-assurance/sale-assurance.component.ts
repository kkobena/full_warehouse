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
} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {ActivatedRoute} from '@angular/router';
import {NgbModal} from '@ng-bootstrap/ng-bootstrap';
import {Toast} from 'primeng/toast';
import {TooltipModule} from 'primeng/tooltip';
import {NgxSpinnerModule, NgxSpinnerService} from 'ngx-spinner';
import {
  ConfirmDialogComponent
} from '../../../../shared/dialog/confirm-dialog/confirm-dialog.component';
import {
  AssuredCustomerListModalComponent,
  InsuranceDataBarComponent,
  ProductListComponent,
  ProductSearchSectionComponent,
  SaleActionsComponent,
  SaleSummaryComponent,
  SaleType,
} from '../../ui';
import {
  AssureFormStepComponent
} from '../../../../entities/customer/assure-form-step/assure-form-step.component';
import {
  FormAyantDroitComponent
} from '../../../../entities/customer/form-ayant-droit/form-ayant-droit.component';
import {
  AyantDroitCustomerListComponent
} from '../../../../entities/sales/ayant-droit-customer-list/ayant-droit-customer-list.component';
import {
  AddComplementaireComponent
} from '../../../../entities/sales/selling-home/assurance/add-complementaire/add-complementaire.component';
import {showCommonModal} from '../../../../entities/sales/selling-home/sale-helper';
import {
  PaymentCompleteEvent,
  PaymentModeComponent
} from '../../ui/payment-mode/payment-mode.component';
import {SalesFacade} from '../../data-access/facades/sales.facade';
import {CustomerSearchService} from '../../data-access/services/customer-search.service';
import {AuthorizationService} from '../../data-access/services/authorization.service';
import {CustomerDisplayService} from '../../data-access/services/customer-display.service';
import {NotificationService} from '../../../../shared/services/notification.service';
import {
  IClientTiersPayant,
  ICustomer,
  IRemise,
  ISalesLine,
  ProduitSearch
} from '../../../../shared/model';
import {UserVendeurService} from '../../../../entities/sales/service/user-vendeur.service';
import {
  CashRegisterFormComponent
} from '../../../../entities/cash-register/user-cash-register/cash-register-form/cash-register-form.component';
import {
  createCustomerHandling,
  createDeconditionnementHandling,
  createForceStockHandling,
  createKeyboardShortcuts,
  createPaymentHandling,
  createProductHandling,
  ProductSearchHost,
} from '../../shared/mixins';
import {SaleForEditInfo} from '../../../../shared/model/sales.model';

/**
 * Composant Container : Création de vente ASSURANCE
 *
 * Responsabilités :
 * - Orchestrer les composants UI pour les ventes ASSURANCE
 * - Gérer la logique métier via SalesFacade
 * - Gérer les tiers payants (part assurée/part client)
 * - CLIENT OBLIGATOIRE dès le départ
 * - Gérer la navigation et les erreurs
 *
 * Architecture : Container/Presentation pattern
 */
@Component({
  selector: 'app-sale-assurance',
  templateUrl: './sale-assurance.component.html',
  styleUrls: ['./sale-assurance.component.scss'],
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
    InsuranceDataBarComponent,
    ConfirmDialogComponent,
    NgxSpinnerModule,

  ],
})
export class SaleAssuranceComponent implements OnInit, AfterViewInit, ProductSearchHost {
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
  selectedSaleType = signal<SaleType>('ASSURANCE');
  // Inputs
  readonly isSmallScreen = input(false);
  readonly isCashRegisterOpen = input(false);
  readonly remises = input<IRemise[]>([]);
  readonly isPresale = input(false);
  initSaleForEditInfo = model<SaleForEditInfo>(null);
  showStock = input(false);
  // Outputs
  productAddedSuccess = output<void>();
  switchToComptant = output<void>();
  selectedLineId = signal<number | null>(null);

  // Modal and responsive state
  customers = signal<ICustomer[]>([]);
  isDiffere = signal<boolean>(false);
  // Force Stock state signals
  waitingForForceStockSuccess = signal<boolean>(false);
  previousLoadingState = signal<boolean>(false);
  forceStockContext = signal<'addProduct' | 'editCell' | null>(null);
  // Monnaie calculée en temps réel depuis le composant payment-mode
  currentChange = computed(() => {
    const change = this.paymentModeComponent()?.changeAmount() || 0;
    return change > 0 ? change : null;
  });
  // Computed pour savoir si la vente peut être sauvegardée (spécifique ASSURANCE)
  canSave = computed(() => {
    const sale = this.currentSale();
    const lines = this.salesLines();
    const customer = this.selectedCustomer();
    const tiersPayants = sale?.tiersPayants || [];
    return !!sale && lines.length > 0 && !!customer && tiersPayants.length > 0 && !this.isSaving();
  });
  protected userVendeurService = inject(UserVendeurService);
  private confirmDialog = viewChild.required<ConfirmDialogComponent>('confirmDialog');
  // Services
  private facade = inject(SalesFacade);
  // State depuis le store (signals computed)
  currentSale = this.facade.currentSale;
  selectedCustomer = this.facade.selectedCustomer;
  selectedProduct = this.facade.selectedProduct;
  salesLines = this.facade.salesLines;
  totalAmount = this.facade.totalAmount;
  discountAmount = this.facade.discountAmount;
  taxAmount = this.facade.taxAmount;
  netAmount = this.facade.netAmount;
  amountToBePaid = this.facade.amountToBePaid;
  canSaveSale = this.facade.canSaveSale;
  hasCustomer = this.facade.hasCustomer;
  isAvoir = this.facade.isAvoir;
  isSaving = this.facade.isSaving;
  loading = this.facade.loading;
  cashier = this.facade.cashier;
  seller = this.facade.seller;

  plafondIsReached = this.facade.plafondIsReached;
  private customerSearchService = inject(CustomerSearchService);
  private authorizationService = inject(AuthorizationService);
  private notificationService = inject(NotificationService);
  private customerDisplay = inject(CustomerDisplayService);
  private modalService = inject(NgbModal);
  private destroyRef = inject(DestroyRef);
  private spinner = inject(NgxSpinnerService);
  private route = inject(ActivatedRoute);
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
      customerRequiredMessage: "Veuillez sélectionner un client assuré avant d'ajouter des produits",
      saleType: 'ASSURANCE',
    },
    selectedProduct: this.facade.selectedProduct,
    currentSale: this.facade.currentSale,
    hasCustomer: this.hasCustomer,
    createSale: (line: ISalesLine) => this.facade.createAssuranceSale(line),
    addProduct: (line: ISalesLine) => this.facade.onAddProduitCarnet(line),
  });

  // ===== Force Stock Handling Mixin =====
  private forceStockHandling = createForceStockHandling({
    facade: this.facade,
    authorizationService: this.authorizationService,
    spinner: this.spinner,
    config: {saleType: 'ASSURANCE'},
    currentSale: this.facade.currentSale,
    loading: this.facade.loading,
    lastError: this.facade.lastError,
    waitingForForceStockSuccess: this.waitingForForceStockSuccess,
    previousLoadingState: this.previousLoadingState,
    forceStockContext: this.forceStockContext,
    getConfirmDialog: () => this.confirmDialog(),
    resetProductSelection: () => this.productHandling.resetProductSelection(),
    operations: {
      createSale: (line: ISalesLine) => this.facade.createAssuranceSale(line),
      addProduct: (line: ISalesLine) => this.facade.onAddProduitCarnet(line),
    },
  });

  // ===== Deconditionnement Handling Mixin =====
  private deconditionnementHandling = createDeconditionnementHandling({
    facade: this.facade,
    waitingForForceStockSuccess: this.waitingForForceStockSuccess,
    getConfirmDialog: () => this.confirmDialog(),
    resetProductSelection: () => this.productHandling.resetProductSelection(),
    operations: {
      createSale: (line: ISalesLine) => this.facade.createAssuranceSale(line),
      addProduct: (line: ISalesLine) => this.facade.onAddProduitCarnet(line),
    },
  });

  // ===== Payment Handling Mixin =====
  private paymentHandling = createPaymentHandling({
    facade: this.facade,
    notificationService: this.notificationService,
    customerDisplay: this.customerDisplay,
    config: {
      saleType: 'ASSURANCE',
      toleranceThreshold: 5, // Seuil de 5 FCFA pour CARNET
      allowDiffere: true,
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
      this.confirmDialog().onConfirm(onConfirm, title, message, undefined, onCancel),

    onPaymentSuccess: () => {
    },
    // Fonction de sauvegarde personnalisée pour ASSURANCE
    customSaveSale: payments => this.facade.saveAssuranceSale(payments),
    onDiffereConfirmed: () => this.handleDiffereConfirmed(),
  });

  // ===== Customer Handling Mixin =====
  private customerHandling = createCustomerHandling({
    facade: this.facade,
    customerSearchService: this.customerSearchService,
    notificationService: this.notificationService,
    modalService: this.modalService,
    config: {
      saleType: 'ASSURANCE',
      customerRequired: true, // Client obligatoire pour vente assurance
      customerRequiredMessage: 'Un client assuré est obligatoire pour une vente ASSURANCE',
    },
    selectedCustomer: this.facade.selectedCustomer,
    customers: this.customers,
    customerListComponent: AssuredCustomerListModalComponent,
    customerFormComponent: AssureFormStepComponent,
    onCustomerSelectedCallback: customer => {
      // Créer la vente si elle n'existe pas
      let currentSale = this.currentSale();
      if (!currentSale || !currentSale.saleId) {
        this.facade.initializeAssuranceSale();
        // Définir le client après l'initialisation
        this.facade.setCustomer(customer);
        //   this.currentSale();
      }

      // Mettre à jour les tiers payants via la facade
      if (customer.tiersPayants && customer.tiersPayants.length > 0) {
        this.facade.updateSaleTiersPayants(customer.tiersPayants);
      }

      // Initialiser les tiers payants dans le composant UI (remplace l'ancien effect)
      this.insuranceDataBar()?.initializeFromCustomer(customer);

      // Focus sur le premier champ de numéro de bon
      setTimeout(() => this.insuranceDataBar()?.focusFirstBon(), 100);
    },
  });

  // ===== Keyboard Shortcuts Mixin =====
  private keyboardShortcutsMixin = createKeyboardShortcuts(
    {saleType: 'ASSURANCE', isPresale: () => this.isPresale()},
    {
      focusProductSearch: () => this.productHandling.focusProductSearch(),
      focusQuantity: () => this.productSearchComponent()?.focusProduitControl(),
      focusCustomer: () => this.focusCustomerSearch(),
      addProduct: () => {
        const product = this.selectedProduct();
        if (product) {
          this.productHandling.onAddQuantity(1);
        }
      },
      clearProduct: () => this.productHandling.resetProductSelection(),
      finalizeSale: () => this.onSave(),
      putOnStandby: () => this.putOnStandby(),
      cancelSale: () => this.onCancel(),
      focusPayment: () => this.paymentModeComponent()?.focusFirstMode(),
      printReceipt: () => this.onPrint(),
      saveAsPresale: () => this.onSaveAsPresale(true),
      savePresale: () => this.onSaveAsPresale(false),
    },
  );

  constructor() {
    // Initialiser les effects de gestion du forçage de stock via le mixin
    this.forceStockHandling.initializeEffects();
    // Initialiser les effects de déconditionnement (après force-stock)
    this.deconditionnementHandling.initializeEffects();
    this.initializeEffects();
  }

  // ===== Effects Initialization =====

  ngOnInit(): void {
    // Initialiser une vente ASSURANCE

    this.facade.initializeAssuranceSale();

    // Initialize typePrescription with default value
    this.facade.setTypePrescription('PRESCRIPTION');

    // S'abonner aux événements de succès pour gérer le focus et reset
    this.facade.productAddedSuccess$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      // Utiliser le mixin pour l'affichage client et le reset
      this.productHandling.updatePendingDisplay();
      this.productHandling.resetProductSelection();
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
    this.facade.cancelSaleSuccess$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      this.resetForNewSale();
    });

    // S'abonner au succès de mise à jour de la remise
    this.facade.remiseUpdatedSuccess$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      this.productHandling.focusProductSearch();
    });
    this.facade.standbySuccess$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      this.resetForNewSale();
    });
    this.facade.resumePendingSaleSuccess$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      const currentSale = this.currentSale();
      if (currentSale?.saleId) {
        // Vente existe sur le backend: utiliser les tiers payants de la vente rechargée
        const updatedTiersPayants = currentSale.tiersPayants || [];
        this.insuranceDataBar()?.updateTiersPayants(updatedTiersPayants);

      }
    });
    // S'abonner au succès d'ajout de tiers payant complémentaire
    this.facade.tiersPayantAddedSuccess$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(newTiersPayant => {
      const currentSale = this.currentSale();
      if (currentSale?.saleId) {
        // Vente existe sur le backend: utiliser les tiers payants de la vente rechargée
        const updatedTiersPayants = currentSale.tiersPayants || [];
        this.insuranceDataBar()?.updateTiersPayants(updatedTiersPayants);
      } else {
        // Pas de vente backend: ajouter le nouveau tiers payant à la liste locale
        const currentTiersPayants = this.insuranceDataBar()?.getSelectedTiersPayants() || [];
        const updatedTiersPayants = [...currentTiersPayants, newTiersPayant];
        this.insuranceDataBar()?.updateTiersPayants(updatedTiersPayants);
      }
      setTimeout(() => this.insuranceDataBar()?.focusLastBon(), 100);
    });
  }

  ngAfterViewInit(): void {
    // Force le focus sur la recherche client au chargement initial
    setTimeout(() => {
      this.insuranceDataBar()?.searchInput()?.nativeElement.focus();
    }, 200);
  }

  /**
   * Délègue au mixin productHandling
   * Focus automatique sur quantité après sélection
   */
  onProductSelected(product: ProduitSearch | null): void {
    this.productHandling.onProductSelected(product);
  }

  /**
   * Délègue au mixin productHandling
   * Scanner → ajout automatique avec quantité 1
   */
  onProductScanned(product: ProduitSearch): void {
    this.productHandling.onProductScanned(product);
  }

  /**
   * Enter dans champ produit vide → validation si amountToBePaid <= 0
   */
  onProductSearchEnter(shouldSave: boolean): void {
    if (!shouldSave) {
      return;
    }

    const currentSale = this.currentSale();

    // Si vente en cours avec des lignes
    if (currentSale && currentSale.salesLines && currentSale.salesLines.length > 0) {
      const amountToBePaid = currentSale.amountToBePaid || 0;

      // Si montant à payer <= 0, finaliser directement sans paiement
      if (amountToBePaid <= 0) {
        this.finalizeSaleWithoutPayment();
      } else {
        // Focus sur le champ cash du payment-mode déjà affiché
        setTimeout(() => {
          this.paymentModeComponent()?.focusFirstMode();
        }, 100);
      }
    }
  }

  /**
   * Délègue au mixin productHandling
   * Gère l'ajout de quantité depuis le composant QuantiteProdutSaisieComponent
   */
  onAddQuantity(quantity: number): void {
    this.productHandling.onAddQuantity(quantity);
  }

  onLineQuantityChanged(event: { line: ISalesLine; newQty: number }): void {
    if (event.line && event.line.id) {
      this.facade.updateLineQuantitySold(event.line.id, event.newQty);
    }
  }

  onLineQuantityRequestedChanged(event: { line: ISalesLine; newQty: number }): void {
    if (event.line && event.line.id) {
      this.facade.updateLineQuantityRequested(event.line.id, event.newQty);
    }
    // Focus géré via souscription à lineUpdatedSuccess$
  }

  onLineRemoved(line: ISalesLine): void {
    if (line && line.id) {
      this.facade.removeSalesLine(line.id);
    }
  }

  onLineSelected(line: ISalesLine): void {
    if (line && line.id) {
      this.selectedLineId.set(line.id);
    }
  }

  onLineDiscountChanged(event: { line: ISalesLine; newDiscount: number }): void {
    if (event.line && event.line.id) {
      this.facade.updateSalesLine(event.line.id, {
        regularUnitPrice: event.line.regularUnitPrice! * (1 - event.newDiscount / 100),
      });
    }
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

    if (event.line && event.line.id) {
      this.facade.removeSalesLine(event.line.id);
    }
  }

  /**
   * Délègue au mixin customerHandling
   */
  onCustomerSelected(customer: ICustomer): void {
    this.customerHandling.selectCustomer(customer);
  }

  onCustomerRemoved(): void {
    this.confirmDialog().onConfirm(
      () => this.customerHandling.removeCustomer(),
      'Retirer le client',
      'Êtes-vous sûr de vouloir retirer ce client ?',
    );
  }

  // ============================================
  // Gestion Client
  // ============================================

  /**
   * Ouvre le formulaire de création client assuré
   */
  onCustomerAdd(): void {
    this.openAssuredCustomerForm(null);
  }

  /**
   * Délègue au mixin customerHandling
   */
  onCustomerSearchChange(searchTerm: string): void {
    this.customerHandling.searchCustomers(searchTerm);
  }

  onInsuranceDataUpdate(data: { customer: ICustomer; tiersPayants: IClientTiersPayant[] }): void {
    // Mettre à jour le client
    this.facade.setCustomer(data.customer);

    // Mettre à jour les tiers payants via la facade de manière réactive
    this.facade.updateSaleTiersPayants(data.tiersPayants);
  }

  onCustomerSelectedFromBar(customer: ICustomer): void {
    // Cloner l'objet pour forcer la réactivité
    const newCustomer = {...customer, tiersPayants: [...(customer.tiersPayants || [])]};
    // Utiliser le mixin qui gère la logique commune + callback ASSURANCE
    this.customerHandling.selectCustomer(newCustomer);
  }

  // ============================================
  // Gestion Tiers Payants
  // ============================================

  onOpenCustomerList(event?: { customers: ICustomer[]; searchTerm: string }): void {
    showCommonModal(
      this.modalService,
      AssuredCustomerListModalComponent,
      {
        searchString: event?.searchTerm || '',
        headerLibelle: 'CLIENTS ASSURÉS',
        typeTiersPayant: 'ASSURANCE',
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
      this.openAssuredCustomerForm(customer);
    }
  }

  onAddCustomer(): void {
    this.openAssuredCustomerForm(null);
  }

  onEditAyantDroit(): void {
    const currentSale = this.currentSale();
    const ayantDroit = currentSale?.ayantDroit;
    if (ayantDroit) {
      this.openAyantDroitForm(ayantDroit);
    }
  }

  onLoadAyantDroits(): void {
    const customer = this.selectedCustomer();
    if (!customer) {
      this.notificationService.warning("Sélectionnez d'abord un client assuré", 'Client requis');
      return;
    }

    showCommonModal(
      this.modalService,
      AyantDroitCustomerListComponent,
      {
        assure: customer,
        header: `LISTE DES AYANTS DROITS DU CLIENT [${customer.fullName}]`,
      },
      (ayantDroit: ICustomer) => {
        if (ayantDroit) {
          const currentSale = this.currentSale();
          if (currentSale) {
            if (ayantDroit.id) {
              currentSale.ayantDroit = ayantDroit;
            } else {
              // Nouveau ayant droit à créer
              this.openAyantDroitForm(ayantDroit);
            }
          }
        }
      },
      'xl',
    );
  }

  onAddComplementaire(): void {
    const customer = this.selectedCustomer();
    const currentSale = this.currentSale();

    if (!customer) {
      this.notificationService.warning("Sélectionnez d'abord un client assuré", 'Client requis');
      return;
    }

    showCommonModal(
      this.modalService,
      AddComplementaireComponent,
      {
        tiersPayantsExisting: currentSale?.tiersPayants || [],
        assure: customer,
      },
      (newTiersPayant: IClientTiersPayant) => {
        if (newTiersPayant) {
          // Utiliser la facade pour ajouter le tiers payant (appel API si vente existe)
          // L'update UI se fait dans l'abonnement à tiersPayantAddedSuccess$
          this.facade.addTiersPayantToSale(newTiersPayant);
        }
      },
      'xl',
    );
  }

  onRemoveTiersPayant(tiersPayant: IClientTiersPayant): void {
    this.confirmDialog().onConfirm(
      () => {
        const currentSale = this.currentSale();

        // Si la vente existe (saleId), utiliser la facade pour appeler l'API
        // Cela permet au backend de recalculer les montants
        if (currentSale?.saleId) {
          this.facade.removeTiersPayantFromSale(tiersPayant, () => {
            // Callback de succès: mettre à jour l'état local de l'insurance-data-bar
            // Note: La facade a déjà rechargé la vente, donc l'effect de synchronisation
            // dans InsuranceDataBarComponent mettra automatiquement à jour selectedTiersPayants
            this.removeTiersPayantLocally(tiersPayant);
          });
        } else {
          // Si pas de vente backend, retirer localement seulement
          this.removeTiersPayantLocally(tiersPayant);
        }
      },
      'Supprimer tiers payant',
      `Êtes-vous sûr de vouloir supprimer le tiers payant ${tiersPayant.tiersPayantName} ?`,
    );
  }

  onTiersPayantsChanged(tiersPayants: IClientTiersPayant[]): void {
    // Utiliser la facade pour mettre à jour les tiers payants de manière réactive
    this.facade.updateSaleTiersPayants(tiersPayants);
  }

  onCreateNewInsuredCustomer(): void {
    this.openAssuredCustomerForm(null);
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

  onSave(): void {
    // Validations spécifiques ASSURANCE avant d'appeler le mixin
    if (!this.validateAssuranceSale()) {
      return;
    }

    // Si c'est un avoir, confirmer
    if (this.isAvoir()) {
      this.confirmDialog().onConfirm(
        () => this.paymentHandling.onSave(),
        'Avoir détecté',
        'Cette vente sera enregistrée comme AVOIR (quantité demandée ≠ quantité servie). Confirmer ?',
      );
    } else {
      // Déléguer au mixin paymentHandling
      this.paymentHandling.onSave();
    }
  }

  /**
   * Appelé quand l'utilisateur valide le paiement depuis le composant payment-mode
   * Délègue au mixin paymentHandling
   */
  onPaymentComplete(event: PaymentCompleteEvent): void {
    if (!this.validateAssuranceSale()) {
      return;
    }
    this.paymentHandling.processPayment(event);
  }

  onSaveAndPrint(): void {
    this.facade.setPrintReceipt(true);
    this.onSave();
  }

  onPrint(): void {
    const sale = this.currentSale();
    if (sale?.saleId) {
      this.facade.printCurrentSale();
    } else {
      this.notificationService.error("La vente doit être enregistrée d'abord", 'Impression impossible');
    }
  }

  // ============================================
  // Handlers pour remise globale (depuis ProductListComponent caption)
  // ============================================

  putOnStandby(): void {
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
    if (!this.validateAssuranceSale()) {
      return;
    }

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

  // ============================================
  // Actions de vente
  // ============================================

  onCancel(): void {
    // Si pas de lignes, reset simple sans confirmation
    if (this.salesLines().length === 0) {
      this.resetForNewSale();
      return;
    }

    // Confirmer avant d'annuler (comportement identique à sale-carnet)
    this.confirmDialog().onConfirm(
      () => this.facade.cancelSale(),
      'Annulation de la vente',
      'Êtes-vous sûr de vouloir annuler cette vente ?',
    );
  }

  handleKeyboardEvent(event: KeyboardEvent): void {
    this.keyboardShortcutsMixin.handleKeyboardEvent(event);
  }

  /**
   * Méthode publique pour mettre le focus sur la recherche produit
   * Appelée par le composant parent lors du changement de tab
   */
  public focusProductSearch(): void {
    this.productHandling.focusProductSearch();
  }

  getCustomerDisplay(customer: ICustomer | null): string {
    if (!customer) {
      return '';
    }
    return `${customer.firstName || ''} ${customer.lastName || ''} ${customer.phone || ''}`;
  }

  private initializeEffects(): void {
    this.setupSavingStateEffect();
  }

  /**
   * Effect pour contrôler le spinner selon l'état de sauvegarde
   */
  private setupSavingStateEffect(): void {
    effect(() => {
      this.isSaving() ? this.spinner.show('sale-spinner') : this.spinner.hide('sale-spinner');
    });
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

  private removeTiersPayantLocally(tiersPayant: IClientTiersPayant): void {
    const dataBar = this.insuranceDataBar();
    if (dataBar) {
      dataBar.removeTiersPayantLocally(tiersPayant);
    }
  }

  private openAssuredCustomerForm(customer: ICustomer | null): void {
    const isEdit = !!customer;
    const header = isEdit ? 'FORMULAIRE DE MODIFICATION DE CLIENT' : "FORMULAIRE D'AJOUT DE NOUVEAU CLIENT";

    showCommonModal(
      this.modalService,
      AssureFormStepComponent,
      {
        entity: customer || undefined,
        typeAssure: 'ASSURANCE',
        header,
      },
      (updatedCustomer: ICustomer) => {
        if (updatedCustomer) {
          this.facade.setCustomer(updatedCustomer);
          // Utiliser la facade pour mettre à jour les tiers payants de manière réactive
          if (updatedCustomer.tiersPayants) {
            this.facade.updateSaleTiersPayants(updatedCustomer.tiersPayants);
          }
        }
      },
      'xl',
      'modal-dialog-80',
    );
  }


  // ============================================
  // Raccourcis clavier
  // ============================================

  private openAyantDroitForm(ayantDroit: ICustomer): void {
    const customer = this.selectedCustomer();
    if (!customer) {
      return;
    }

    showCommonModal(
      this.modalService,
      FormAyantDroitComponent,
      {
        entity: ayantDroit,
        assure: customer,
        title: 'FORMULAIRE DE MODIFICATION',
      },
      (updatedAyantDroit: ICustomer) => {
        if (updatedAyantDroit) {
          const currentSale = this.currentSale();
          if (currentSale) {
            currentSale.ayantDroit = updatedAyantDroit;
          }
        }
      },
      'xl',
    );
  }

  // ============================================
  // Helpers
  // ============================================

  /**
   * Validations spécifiques pour une vente ASSURANCE
   * Rebuilde les tiers payants avec les numBon des inputs et met à jour la vente
   */
  private validateAssuranceSale(): boolean {
    const sale = this.currentSale();

    if (!sale) {
      this.notificationService.warning('Aucune vente à enregistrer', 'Vente vide');
      return false;
    }

    if (this.salesLines().length === 0) {
      this.notificationService.warning('Ajoutez au moins un produit', 'Vente vide');

      return false;
    }

    if (!this.hasCustomer()) {
      this.notificationService.error('Un client assuré est obligatoire', 'Client requis');
      return false;
    }

    // Rebuilder les tiers payants avec les numBon des inputs
    const tiersPayantsFromInputs = this.insuranceDataBar()?.buildIClientTiersPayantFromInputs() || [];

    if (tiersPayantsFromInputs.length === 0) {
      this.notificationService.error('Ajoutez au moins un tiers payant', 'Tiers payants requis');
      return false;
    }

    // Vérifier les numBon avec les données synchronisées depuis les inputs
    const missingBonNumbers = tiersPayantsFromInputs.filter(tp => !tp.numBon);
    if (missingBonNumbers.length > 0) {
      this.notificationService.error('Veuillez renseigner tous les numéros de bon', 'Numéros de bon requis');
      return false;
    }

    // Mettre à jour les tiers payants dans la vente AVANT de sauvegarder
    // Ceci garantit que les numBon sont synchronisés même si l'événement est asynchrone

    this.facade.updateSaleTiersPayants(tiersPayantsFromInputs);

    return true;
  }

  /**
   * Finalise la vente sans paiement (amountToBePaid <= 0)
   * Délègue au mixin paymentHandling après validation
   */
  private finalizeSaleWithoutPayment(): void {
    if (!this.validateAssuranceSale()) {
      return;
    }
    this.paymentHandling.finalizeSaleWithoutPayment();
  }

  private focusCustomerSearch(): void {
    // Focus sur le champ de recherche client dans la barre assurance
    setTimeout(() => {
      this.insuranceDataBar()?.searchInput()?.nativeElement.focus();
    }, 100);
  }

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
        // Finaliser la vente via le mixin après ouverture de la caisse
        setTimeout(() => this.paymentHandling.completeSale(), 100);
      }
    });
  }
}
