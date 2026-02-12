import { AfterViewInit, Component, computed, DestroyRef, effect, inject, input, OnInit, output, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { Toast } from 'primeng/toast';
import { TooltipModule } from 'primeng/tooltip';
import { MessageService } from 'primeng/api';
import { NgxSpinnerModule, NgxSpinnerService } from 'ngx-spinner';
import { ConfirmDialogComponent } from '../../../../shared/dialog/confirm-dialog/confirm-dialog.component';
import { AssuredCustomerListComponent } from '../../../../entities/sales/assured-customer-list/assured-customer-list.component';
import { AssureFormStepComponent } from '../../../../entities/customer/assure-form-step/assure-form-step.component';
import { FormAyantDroitComponent } from '../../../../entities/customer/form-ayant-droit/form-ayant-droit.component';
import { AyantDroitCustomerListComponent } from '../../../../entities/sales/ayant-droit-customer-list/ayant-droit-customer-list.component';
import { AddComplementaireComponent } from '../../../../entities/sales/selling-home/assurance/add-complementaire/add-complementaire.component';
import { QuantiteProdutSaisieComponent } from '../../../../shared/quantite-produt-saisie/quantite-produt-saisie.component';
import { showCommonModal } from '../../../../entities/sales/selling-home/sale-helper';
import {
  InsuranceDataBarComponent,
  PendingSalesListComponent,
  ProductListComponent,
  ProductSearchComponent,
  SaleActionsComponent,
  SaleSummaryComponent,
} from '../../ui';
import { PaymentCompleteEvent, PaymentModeComponent } from '../../ui/payment-mode/payment-mode.component';
import { SalesFacade } from '../../data-access/facades/sales.facade';
import { CustomerSearchService } from '../../data-access/services/customer-search.service';
import { AuthorizationService } from '../../data-access/services/authorization.service';
import { CustomerDisplayService } from '../../data-access/services/customer-display.service';
import { NotificationService } from '../../../../shared/services/notification.service';
import { IClientTiersPayant, ICustomer, ISales, ISalesLine, ProduitSearch } from '../../../../shared/model';
import { UserVendeurService } from '../../../../entities/sales/service/user-vendeur.service';
import { CashRegisterFormComponent } from '../../../../entities/cash-register/user-cash-register/cash-register-form/cash-register-form.component';
import {
  createCustomerHandling,
  createForceStockHandling,
  createPaymentHandling,
  createProductHandling,
  ProductSearchHost,
} from '../../shared/mixins';
import { Drawer } from 'primeng/drawer';

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
    ProductSearchComponent,
    ProductListComponent,
    SaleSummaryComponent,
    SaleActionsComponent,
    PaymentModeComponent,
    InsuranceDataBarComponent,
    QuantiteProdutSaisieComponent,
    ConfirmDialogComponent,
    NgxSpinnerModule,
    Drawer,
    PendingSalesListComponent,
  ],
})
export class SaleAssuranceComponent implements OnInit, AfterViewInit, ProductSearchHost {
  productSearchComponent = viewChild<ProductSearchComponent>('produitbox');
  quantityComponent = viewChild<QuantiteProdutSaisieComponent>('quantityBox');
  insuranceDataBar = viewChild<InsuranceDataBarComponent>('insuranceDataBar');
  paymentModeComponent = viewChild<PaymentModeComponent>('paymentMode');
  private confirmDialog = viewChild.required<ConfirmDialogComponent>('confirmDialog');

  // Inputs
  readonly isSmallScreen = input(false);
  readonly isCashRegisterOpen = input(false);

  // Outputs
  productAddedSuccess = output<void>();
  switchToComptant = output<void>();

  // Modal and responsive state

  // Services
  private facade = inject(SalesFacade);
  private customerSearchService = inject(CustomerSearchService);
  private authorizationService = inject(AuthorizationService);
  private notificationService = inject(NotificationService);
  private messageService = inject(MessageService);
  private customerDisplay = inject(CustomerDisplayService);
  private translate = inject(TranslateService);
  private modalService = inject(NgbModal);
  private destroyRef = inject(DestroyRef);
  private spinner = inject(NgxSpinnerService);
  protected userVendeurService = inject(UserVendeurService);

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
  lastError = this.facade.lastError;
  cashier = this.facade.cashier;
  seller = this.facade.seller;

  // Local state signals
  private focusInitialized = false;
  selectedLineId = signal<number | null>(null);
  customers = signal<ICustomer[]>([]);
  showPendingSales = signal(false);
  isDiffere = signal<boolean>(false);

  // Force Stock state signals
  waitingForForceStockSuccess = signal<boolean>(false);
  previousLoadingState = signal<boolean>(false);
  forceStockContext = signal<'addProduct' | 'editCell' | null>(null);

  // Computed pour convertir l'input isCashRegisterOpen en Signal<boolean>
  private isCashRegisterOpenSignal = computed(() => this.isCashRegisterOpen() ?? false);

  // Computed pour savoir si la vente peut être sauvegardée (spécifique ASSURANCE)
  canSave = computed(() => {
    const sale = this.currentSale();
    const lines = this.salesLines();
    const customer = this.selectedCustomer();
    const tiersPayants = sale?.tiersPayants || [];
    return !!sale && lines.length > 0 && !!customer && tiersPayants.length > 0 && !this.isSaving();
  });

  // Monnaie calculée en temps réel depuis le composant payment-mode
  currentChange = computed(() => {
    const change = this.paymentModeComponent()?.changeAmount() || 0;
    return change > 0 ? change : null;
  });

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
    // createAssuranceSale pour créer une nouvelle vente ASSURANCE avec le premier produit
    createSale: (line: ISalesLine) => this.facade.createAssuranceSale(line),
    // onAddProduitCarnet utilise le même endpoint /add-item/assurance partagé avec CARNET
    addProduct: (line: ISalesLine) => this.facade.onAddProduitCarnet(line),
  });

  // ===== Force Stock Handling Mixin =====
  private forceStockHandling = createForceStockHandling({
    facade: this.facade,
    authorizationService: this.authorizationService,
    spinner: this.spinner,
    config: { saleType: 'ASSURANCE' },
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

  // ===== Payment Handling Mixin =====
  private paymentHandling = createPaymentHandling({
    facade: this.facade,
    notificationService: this.notificationService,
    customerDisplay: this.customerDisplay,
    config: {
      saleType: 'ASSURANCE',
      toleranceThreshold: 0, // Pas de tolérance pour ASSURANCE
      allowDiffere: false, // Pas de vente différée pour ASSURANCE
    },
    currentSale: this.facade.currentSale,
    salesLines: this.facade.salesLines,
    canSave: this.canSave,
    isCashRegisterOpen: this.isCashRegisterOpenSignal,
    getPaymentModeComponent: () => {
      const comp = this.paymentModeComponent();
      if (!comp) return undefined;
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
    onPaymentSuccess: () => this.switchToComptant.emit(),
    // Fonction de sauvegarde personnalisée pour ASSURANCE
    customSaveSale: payments => this.facade.saveAssuranceSale(payments),
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
    customerListComponent: AssuredCustomerListComponent,
    customerFormComponent: AssureFormStepComponent,
    onCustomerSelectedCallback: customer => {
      // Créer la vente si elle n'existe pas
      let currentSale = this.currentSale();
      if (!currentSale || !currentSale.saleId) {
        this.facade.initializeAssuranceSale();
        // Définir le client après l'initialisation
        this.facade.setCustomer(customer);
        currentSale = this.currentSale();
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

  // Keyboard shortcuts state
  private readonly keyboardShortcuts = [
    { key: 'F2', action: () => this.productHandling.focusProductSearch(), description: 'Recherche produit' },
    { key: 'F3', action: () => this.focusCustomerSearch(), description: 'Recherche client' },
    { key: 'F4', action: () => this.onSaveAsPresale(), description: 'Mise en attente' },
    { key: 'F9', action: () => this.onSave(), description: 'Finaliser' },
    { key: 'F10', action: () => this.onCancel(), description: 'Annuler' },
  ];

  constructor() {
    // Initialiser les effects de gestion du forçage de stock via le mixin
    this.forceStockHandling.initializeEffects();
    this.initializeEffects();
  }

  // ===== Effects Initialization =====

  private initializeEffects(): void {
    this.setupErrorHandlingEffect();
    this.setupSavingStateEffect();
    this.setupCustomerRequiredEffect();
  }

  /**
   * Effect pour surveiller les erreurs et les afficher
   * Note: Les erreurs de stock sont gérées par le mixin forceStockHandling
   */
  private setupErrorHandlingEffect(): void {
    effect(() => {
      const error = this.lastError();
      const errorDetails = this.facade.errorDetails();
      // Ignorer les erreurs de stock - elles sont gérées par le mixin forceStockHandling
      if (error && errorDetails?.errorKey !== 'stock') {
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: error });
      }
    });
  }

  /**
   * Effect pour contrôler le spinner selon l'état de sauvegarde
   */
  private setupSavingStateEffect(): void {
    effect(() => {
      this.isSaving() ? this.spinner.show('sale-spinner') : this.spinner.hide('sale-spinner');
    });
  }

  /**
   * Effect pour vérifier que le client est obligatoire pour ASSURANCE
   */
  private setupCustomerRequiredEffect(): void {
    effect(() => {
      const sale = this.currentSale();
      if (sale && !sale.customerId) {
        this.messageService.add({
          severity: 'warn',
          summary: 'Client requis',
          detail: 'Un client assuré est obligatoire pour une vente ASSURANCE',
        });
      }
    });
  }

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

  // ============================================
  // Gestion Produits
  // ============================================

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
    if (!shouldSave) return;

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
    if (event.action === 'delete') {
      // Supprimer la ligne directement (la confirmation est déjà faite dans product-list)
      if (event.line && event.line.id) {
        this.facade.removeSalesLine(event.line.id);
      }
    } else if (event.action === 'discount') {
      // Gérer l'autorisation de remise si nécessaire
      this.authorizationService
        .requestDiscountAuthorization(0) // TODO: récupérer le discount depuis l'event
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: authorized => {
            if (!authorized) {
              this.messageService.add({ severity: 'warn', summary: 'Non autorisé', detail: 'Remise refusée' });
            }
          },
          error: () => {
            this.messageService.add({ severity: 'error', summary: 'Erreur', detail: "Erreur lors de l'autorisation" });
          },
        });
    }
  }

  // ============================================
  // Gestion Client
  // ============================================

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

  // ============================================
  // Gestion Tiers Payants
  // ============================================

  onInsuranceDataUpdate(data: { customer: ICustomer; tiersPayants: IClientTiersPayant[] }): void {
    // Mettre à jour le client
    this.facade.setCustomer(data.customer);

    // Mettre à jour les tiers payants via la facade de manière réactive
    this.facade.updateSaleTiersPayants(data.tiersPayants);
  }

  onCustomerSelectedFromBar(customer: ICustomer): void {
    // Cloner l'objet pour forcer la réactivité
    const newCustomer = { ...customer, tiersPayants: [...(customer.tiersPayants || [])] };
    // Utiliser le mixin qui gère la logique commune + callback ASSURANCE
    this.customerHandling.selectCustomer(newCustomer);
  }

  onOpenCustomerList(): void {
    showCommonModal(
      this.modalService,
      AssuredCustomerListComponent,
      {
        searchString: '',
        headerLibelle: 'CLIENTS ASSURÉS',
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
      this.messageService.add({
        severity: 'warn',
        summary: 'Client requis',
        detail: "Sélectionnez d'abord un client assuré",
      });
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
      this.messageService.add({
        severity: 'warn',
        summary: 'Client requis',
        detail: "Sélectionnez d'abord un client assuré",
      });
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

  private removeTiersPayantLocally(tiersPayant: IClientTiersPayant): void {
    const dataBar = this.insuranceDataBar();
    if (dataBar) {
      dataBar.removeTiersPayantLocally(tiersPayant);
    }
  }

  onTiersPayantsChanged(tiersPayants: IClientTiersPayant[]): void {
    // Utiliser la facade pour mettre à jour les tiers payants de manière réactive
    this.facade.updateSaleTiersPayants(tiersPayants);
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

  private openAyantDroitForm(ayantDroit: ICustomer): void {
    const customer = this.selectedCustomer();
    if (!customer) return;

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

  onCreateNewInsuredCustomer(): void {
    this.openAssuredCustomerForm(null);
  }

  // ============================================
  // Actions de vente
  // ============================================

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
   * Validations spécifiques pour une vente ASSURANCE
   * Rebuilde les tiers payants avec les numBon des inputs et met à jour la vente
   */
  private validateAssuranceSale(): boolean {
    const sale = this.currentSale();

    if (!sale) {
      this.messageService.add({ severity: 'warn', summary: 'Vente vide', detail: 'Aucune vente à enregistrer' });
      return false;
    }

    if (this.salesLines().length === 0) {
      this.messageService.add({ severity: 'warn', summary: 'Vente vide', detail: 'Ajoutez au moins un produit' });
      return false;
    }

    if (!this.hasCustomer()) {
      this.messageService.add({ severity: 'warn', summary: 'Client requis', detail: 'Un client assuré est obligatoire' });
      return false;
    }

    // Rebuilder les tiers payants avec les numBon des inputs
    const tiersPayantsFromInputs = this.insuranceDataBar()?.buildIClientTiersPayantFromInputs() || [];

    if (tiersPayantsFromInputs.length === 0) {
      this.messageService.add({
        severity: 'warn',
        summary: 'Tiers payants requis',
        detail: 'Ajoutez au moins un tiers payant',
      });
      return false;
    }

    // Vérifier les numBon avec les données synchronisées depuis les inputs
    const missingBonNumbers = tiersPayantsFromInputs.filter(tp => !tp.numBon);
    if (missingBonNumbers.length > 0) {
      this.messageService.add({
        severity: 'warn',
        summary: 'Numéros de bon requis',
        detail: 'Veuillez renseigner tous les numéros de bon',
      });
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
      this.messageService.add({
        severity: 'warn',
        summary: 'Impression impossible',
        detail: "La vente doit être enregistrée d'abord",
      });
    }
  }

  onSaveAsPresale(): void {
    const sale = this.currentSale();
    if (!sale || this.salesLines().length === 0) {
      this.messageService.add({ severity: 'warn', summary: 'Vente vide', detail: 'Ajoutez au moins un produit' });
      return;
    }

    if (!this.hasCustomer()) {
      this.messageService.add({ severity: 'warn', summary: 'Client requis', detail: 'Un client assuré est obligatoire' });
      return;
    }

    this.facade.putOnStandby();
  }

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

  onShowPendingSales(): void {
    this.showPendingSales.set(true);
  }

  onPendingSaleResumed(sale: ISales): void {
    this.showPendingSales.set(false);
  }

  onPendingSalesClose(): void {
    this.showPendingSales.set(false);
  }

  // ============================================
  // Raccourcis clavier
  // ============================================

  handleKeyboardEvent(event: KeyboardEvent): void {
    // Ignorer si focus dans un input/textarea
    const target = event.target as HTMLElement;
    if (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA') {
      return;
    }

    const shortcut = this.keyboardShortcuts.find(s => s.key === event.key);
    if (shortcut) {
      event.preventDefault();
      shortcut.action();
    }
  }

  // ============================================
  // Helpers
  // ============================================

  /**
   * Méthode publique pour mettre le focus sur la recherche produit
   * Appelée par le composant parent lors du changement de tab
   */
  public focusProductSearch(): void {
    this.productHandling.focusProductSearch();
  }

  private focusCustomerSearch(): void {
    // Focus sur le champ de recherche client dans la barre assurance
    setTimeout(() => {
      this.insuranceDataBar()?.searchInput()?.nativeElement.focus();
    }, 100);
  }

  private resetForNewSale(): void {
    this.customerDisplay.clear();
    this.selectedLineId.set(null);
    this.customers.set([]);
    // L'insurance data bar se réinitialisera automatiquement avec la vente
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

  getCustomerDisplay(customer: ICustomer | null): string {
    if (!customer) return '';
    return `${customer.firstName || ''} ${customer.lastName || ''} ${customer.phone || ''}`;
  }
}
