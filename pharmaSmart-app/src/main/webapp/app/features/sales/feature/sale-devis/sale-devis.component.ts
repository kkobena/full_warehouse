import {
  AfterViewInit,
  Component,
  computed,
  DestroyRef,
  effect,
  ElementRef,
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
import {map} from 'rxjs';
import {NgxSpinnerModule, NgxSpinnerService} from 'ngx-spinner';
import {NgbModal, NgbTooltip} from '@ng-bootstrap/ng-bootstrap';
import {ButtonComponent, IconFieldComponent} from '../../../../shared/ui';
import {NgbConfirmDialogService} from '../../../../shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive';
import {
  CustomerSelectionModalComponent,
  ProductListComponent,
  ProductSearchSectionComponent,
  SaleActionsComponent,
  SaleSummaryComponent,
  SaleType,
} from '../../ui';
import {SalesFacade} from '../../data-access/facades/sales.facade';
import {AuthorizationService} from '../../data-access/services/authorization.service';
import {NotificationService} from '../../../../shared/services/notification.service';
import {CustomerDisplayService} from '../../data-access/services/customer-display.service';
import {CustomerService} from '../../../../entities/customer/customer.service';
import {ICustomer, IRemise, ISalesLine, ProduitSearch} from '../../../../shared/model';
import {
  createCustomerHandling,
  createDeconditionnementHandling,
  createForceStockHandling,
  createKeyboardShortcuts,
  createProductHandling,
  createSaleLifecycle,
  ProductSearchHost,
} from '../../shared/mixins';
import {
  UninsuredCustomerFormComponent
} from '../../../../entities/customer/uninsured-customer-form/uninsured-customer-form.component';
import {SaleForEditInfo} from '../../../../shared/model/sales.model';

/**
 * SaleDevisComponent
 *
 * Composant pour les devis (vente comptant avec client obligatoire)
 *
 * Fonctionnalités spécifiques:
 * - Client obligatoire (comme CARNET)
 * - Pas de tiers payants (contrairement à CARNET)
 * - Pas de paiement (comme prévente)
 * - Statut DEVIS
 *
 * @example
 * <app-sale-devis />
 */
@Component({
  selector: 'app-sale-devis',
  templateUrl: './sale-devis.component.html',
  styleUrls: ['./sale-devis.component.scss'],
  host: {
    '(window:keydown)': 'handleKeyboardEvent($event)',
  },
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [
    CommonModule,
    FormsModule,
    NgbTooltip,
    IconFieldComponent,
    ButtonComponent,
    ProductSearchSectionComponent,
    ProductListComponent,
    SaleSummaryComponent,
    SaleActionsComponent,
    NgxSpinnerModule,
  ],
})
export class SaleDevisComponent implements OnInit, AfterViewInit, ProductSearchHost {
  // ViewChild references
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
  searchInput = viewChild<ElementRef>('searchInput');
  selectedSaleType = signal<SaleType>('COMPTANT');
  initSaleForEditInfo = model<SaleForEditInfo>(null);
  showStock = input(false);
  // Modal and responsive state

  readonly isSmallScreen = input(false);
  readonly remises = input<IRemise[]>([]);
  readonly isDevis = input(true);
  // Outputs
  switchToComptant = output<void>();
  cashRegisterOpened = output<void>();
  // State signals
  readonly saleType = signal<'COMPTANT'>('COMPTANT');
  readonly selectedLineId = signal<number | null>(null);
  readonly waitingForForceStockSuccess = signal<boolean>(false);
  readonly forceStockContext = signal<'addProduct' | 'editCell' | null>(null);
  customers = signal<ICustomer[]>([]);
  // Customer search
  protected search = '';
  private readonly confirmDialog = inject(NgbConfirmDialogService);
  // Services
  private facade = inject(SalesFacade);
  readonly currentSale = this.facade.currentSale;
  readonly salesLines = this.facade.salesLines;
  readonly selectedCustomer = this.facade.selectedCustomer;
  // Helper method pour savoir si un client est sélectionné
  hasCustomer = computed(() => !!this.selectedCustomer());
  readonly selectedProduct = this.facade.selectedProduct;
  readonly loading = this.facade.loading;
  readonly isSaving = this.facade.isSaving;
  // Computed signals
  readonly canSave = computed(() => {
    const sale = this.currentSale();
    const lines = this.salesLines();
    const customer = this.selectedCustomer();
    return !!sale && lines.length > 0 && !!customer && !this.isSaving();
  });
  readonly isAvoir = this.facade.isAvoir;
  private authorizationService = inject(AuthorizationService);
  private notificationService = inject(NotificationService);
  private customerDisplay = inject(CustomerDisplayService);
  private customerService = inject(CustomerService);
  private spinner = inject(NgxSpinnerService);
  private modalService = inject(NgbModal);
  private destroyRef = inject(DestroyRef);


  // ===== Product Handling Mixin =====
  private productHandling = createProductHandling({
    facade: this.facade,
    customerDisplay: this.customerDisplay,
    notificationService: this.notificationService,
    host: this,
    config: {
      requiresCustomer: true,
      customerRequiredMessage: "Veuillez sélectionner un client avant d'ajouter des produits",
      saleType: 'COMPTANT',
    },
    selectedProduct: this.facade.selectedProduct,
    currentSale: this.facade.currentSale,
    hasCustomer: this.hasCustomer,
    createSale: (line: ISalesLine) => this.facade.createDevisSale(line),
    addProduct: (line: ISalesLine) => this.facade.onAddProduitDevis(line),
  });
  // ===== Force Stock Handling Mixin =====
  private forceStockHandling = createForceStockHandling({
    facade: this.facade,
    authorizationService: this.authorizationService,
    config: {saleType: 'COMPTANT'},
    currentSale: this.facade.currentSale,
    loading: this.facade.loading,
    lastError: this.facade.lastError,
    waitingForForceStockSuccess: this.waitingForForceStockSuccess,
    forceStockContext: this.forceStockContext,
    getConfirmDialog: () => this.confirmDialog,
    resetProductSelection: () => this.productHandling.resetProductSelection(),
    operations: {
      createSale: (line: ISalesLine) => this.facade.createDevisSale(line),
      addProduct: (line: ISalesLine) => this.facade.onAddProduitDevis(line),
    },
  });
  // ===== Deconditionnement Handling Mixin =====
  private deconditionnementHandling = createDeconditionnementHandling({
    facade: this.facade,
    waitingForForceStockSuccess: this.waitingForForceStockSuccess,
    getConfirmDialog: () => this.confirmDialog,
    resetProductSelection: () => this.productHandling.resetProductSelection(),
    operations: {
      createSale: (line: ISalesLine) => this.facade.createDevisSale(line),
      addProduct: (line: ISalesLine) => this.facade.onAddProduitDevis(line),
    },
  });
  // ===== Customer Handling Mixin =====
  private customerHandling = createCustomerHandling({
    facade: this.facade,
    notificationService: this.notificationService,
    modalService: this.modalService,
    config: {
      saleType: 'COMPTANT',
      customerRequired: true,
      customerRequiredMessage: 'Un client est obligatoire pour un devis',
    },
    selectedCustomer: this.facade.selectedCustomer,
    customers: this.customers,
    customerListComponent: CustomerSelectionModalComponent,
    customerFormComponent: UninsuredCustomerFormComponent,
    selectCustomerFn: customer => this.facade.setSelectedCustomer(customer),
    searchFn: (term, limit) =>
      this.customerService.queryUninsuredCustomers({search: term, size: limit}).pipe(map(res => res.body || [])),
    smartSearch: true,
    onCustomerSelectedCallback: () => this.focusProductSearch(),
  });
  // ===== Sale Lifecycle Mixin =====
  private lifecycle = createSaleLifecycle({
    facade: this.facade,
    destroyRef: this.destroyRef,
    productHandling: this.productHandling,
    resetForNewSale: () => this.resetForNewSale(),
    onResumePendingSale: false, // Devis ne gère pas la reprise de ventes en attente
  });
  // ===== Keyboard Shortcuts Mixin =====
  private keyboardShortcutsMixin = createKeyboardShortcuts(
    {saleType: 'COMPTANT', isPresale: () => true}, // Toujours en mode "prévente" pour devis (pas de paiement)
    {
      focusProductSearch: () => this.focusProductSearch(),
      focusQuantity: () => this.productSearchComponent()?.focusProduitControl(),
      focusCustomer: () => {
        setTimeout(() => this.searchInput()?.nativeElement.focus(), 100);
      },
      addProduct: () => {
        const product = this.selectedProduct();
        if (product) {
          this.productHandling.onAddQuantity(1);
        }
      },
      clearProduct: () => this.productHandling.resetProductSelection(),
      finalizeSale: () => this.onSaveAsDevis(),
      putOnStandby: () => this.onPutOnHold(),
      cancelSale: () => this.onCancel(),
      focusPayment: () => {
      }, // Pas de paiement pour devis
      saveAsPresale: () => this.onSaveAsDevis(),
    },
  );

  constructor() {
    // Initialiser les effects de gestion du forçage de stock via le mixin
    this.forceStockHandling.initializeEffects();
    // Initialiser les effects de déconditionnement (après force-stock)
    this.deconditionnementHandling.initializeEffects();
    this.initializeEffects();
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
    // Initialiser une vente DEVIS (COMPTANT avec statut DEVIS)
    this.facade.initializeDevisSale();

    // Initialize customer display
    this.customerDisplay.initialize('PHARMA SMART');

    // Initialiser les souscriptions communes via le mixin lifecycle
    this.lifecycle.initializeSubscriptions();
  }

  ngAfterViewInit(): void {
    // Force le focus sur la recherche client au chargement initial
    setTimeout(() => {
      this.searchInput()?.nativeElement.focus();
    }, 200);
  }

  // ===== Customer Search & Selection =====

  onProductSearchEnter(shouldSave: boolean): void {
    if (!shouldSave) {
      return;
    }

    const currentSale = this.currentSale();

    // Si vente en cours avec des lignes, sauvegarder le devis
    if (currentSale && this.salesLines().length > 0) {
      this.onSaveAsDevis();
    }
  }

  onProductSelected(product: ProduitSearch | null): void {
    this.productHandling.onProductSelected(product);
  }

  onAddQuantity(quantity: number): void {
    this.productHandling.onAddQuantity(quantity);
  }

  onProductScanned(product: ProduitSearch, codeScan?: string): void {
    this.productHandling.onProductScanned(product, codeScan);
  }

  onLineQuantityChanged(data: { line: ISalesLine; newQty: number }): void {
    if (data.line.id) {
      this.facade.updateLineQuantitySold(data.line.id, data.newQty);
    }
  }

  onLineQuantityRequestedChanged(data: { line: ISalesLine; newQty: number }): void {
    if (data.line.id) {
      this.facade.updateLineQuantityRequested(data.line.id, data.newQty);
    }
  }

  onLineRemoved(line: ISalesLine): void {
    if (line.saleLineId) {
      this.facade.removeLine(line.saleLineId);
    }
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

  onSaveAsDevis(): void {
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
      this.notificationService.error('Un client est obligatoire pour un devis', 'Client requis');
      return;
    }

    this.facade
      .saveDevis(sale)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: result => {
          if (result) {
            this.resetForNewSale();
          }
        },
      });
  }

  onPutOnHold(): void {
    const sale = this.currentSale();
    if (!sale || this.salesLines().length === 0) {
      this.notificationService.error('Ajoutez au moins un produit', 'Vente vide');
      return;
    }

    if (!this.hasCustomer()) {
      this.notificationService.error('Un client est obligatoire', 'Client requis');
      return;
    }

    this.facade.putOnStandby();
  }

  onCancel(): void {
    // Confirm before canceling
    if (this.salesLines().length > 0) {
      this.confirmDialog.onConfirm(
        () => this.facade.cancelSale(),
        'Annulation du devis',
        'Êtes-vous sûr de vouloir annuler ce devis ?',
      );
    } else {
      this.resetForNewSale();
    }
  }

  // ===== Product Management =====

  handleKeyboardEvent(event: KeyboardEvent): void {
    this.keyboardShortcutsMixin.handleKeyboardEvent(event);
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

  protected onSearchCustomer(): void {
    if (!this.search) {
      return;
    }
    this.customerHandling.searchCustomers(this.search, 1, 5);
    this.search = '';
  }

  protected onOpenCustomerList(): void {
    this.customerHandling.openCustomerListModal({modalDialogClass: 'modal-dialog-70'});
  }

  protected onEditCustomer(): void {
    const customer = this.selectedCustomer();
    if (customer) {
      this.customerHandling.openCustomerFormModal(customer, {
        title: 'MODIFICATION CLIENT',
        modalDialogClass: 'modal-dialog-80',
      });
    }
  }

  protected onAddCustomer(): void {
    this.customerHandling.openCustomerFormModal(null, {
      title: 'NOUVEAU CLIENT',
      modalDialogClass: 'modal-dialog-80',
    });
  }

  /**
   * Réinitialiser pour une nouvelle vente
   */
  private resetForNewSale(): void {
    this.customerDisplay.clear();
    this.initSaleForEditInfo.set(null);
    this.selectedLineId.set(null);
    this.customers.set([]);
    this.facade.resetCurrentSale();
    this.switchToComptant.emit();
  }
}
