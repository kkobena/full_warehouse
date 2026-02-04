import {
  Component,
  OnInit,
  AfterViewInit,
  inject,
  signal,
  DestroyRef,
  effect,
  HostListener,
  viewChild,
  output,
  input,
  model,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { MessageService } from 'primeng/api';
import { Toast } from 'primeng/toast';
import { Button } from 'primeng/button';
import { Select } from 'primeng/select';
import { Drawer } from 'primeng/drawer';
import { TooltipModule } from 'primeng/tooltip';
import { ConfirmationService } from 'primeng/api';
import { NgxSpinnerService } from 'ngx-spinner';
import { ConfirmDialogComponent } from '../../../../shared/dialog/confirm-dialog/confirm-dialog.component';
import { AssuredCustomerListComponent } from '../../../../entities/sales/assured-customer-list/assured-customer-list.component';
import { AssureFormStepComponent } from '../../../../entities/customer/assure-form-step/assure-form-step.component';
import { FormAyantDroitComponent } from '../../../../entities/customer/form-ayant-droit/form-ayant-droit.component';
import { AyantDroitCustomerListComponent } from '../../../../entities/sales/ayant-droit-customer-list/ayant-droit-customer-list.component';
import { AddComplementaireComponent } from '../../../../entities/sales/selling-home/assurance/add-complementaire/add-complementaire.component';
import { QuantiteProdutSaisieComponent } from '../../../../shared/quantite-produt-saisie/quantite-produt-saisie.component';
import { showCommonModal } from '../../../../entities/sales/selling-home/sale-helper';
import {
  ProductListComponent,
  SaleSummaryComponent,
  CustomerSelectorComponent,
  SaleActionsComponent,
  PendingSalesListComponent,
  ProductSearchComponent,
} from '../../ui';
import { PaymentModeComponent, PaymentCompleteEvent } from '../../ui/payment-mode/payment-mode.component';
import { InsuranceDataBarComponent } from '../../ui/insurance-data-bar/insurance-data-bar.component';
import { SalesFacade } from '../../data-access/facades/sales.facade';
import { CustomerSearchService } from '../../data-access/services/customer-search.service';
import { AuthorizationService } from '../../data-access/services/authorization.service';
import { CustomerDisplayService } from '../../data-access/services/customer-display.service';
import { NotificationService } from '../../../../shared/services/notification.service';
import { ISalesLine, SalesLine } from '../../../../shared/model/sales-line.model';
import { ICustomer } from '../../../../shared/model/customer.model';
import { ProduitSearch } from '../../../../shared/model/produit.model';
import { ISales } from '../../../../shared/model/sales.model';
import { IUser } from '../../../../core/user/user.model';
import { IPaymentMode } from '../../../../shared/model/payment-mode.model';
import { UserVendeurService } from '../../../../entities/sales/service/user-vendeur.service';
import { createSalesLineFromProduct } from '../../data-access/utils/sales-line.utils';
import { IClientTiersPayant } from '../../../../shared/model/client-tiers-payant.model';
import { NgxSpinnerModule } from 'ngx-spinner';

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
  standalone: true,
  templateUrl: './sale-assurance.component.html',
  styleUrls: ['./sale-assurance.component.scss'],
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
    NgxSpinnerModule,
  ],
  providers: [MessageService, ConfirmationService], // Instance locale pour ce composant
})
export class SaleAssuranceComponent implements OnInit, AfterViewInit {
  productSearchComponent = viewChild<ProductSearchComponent>('produitbox');
  quantityComponent = viewChild<QuantiteProdutSaisieComponent>('quantityBox');
  insuranceDataBar = viewChild<InsuranceDataBarComponent>('insuranceDataBar');
  paymentModeComponent = viewChild<PaymentModeComponent>('paymentMode');

  // Service de confirmation
  private confirmationService = inject(ConfirmationService);

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
  private customerDisplay = inject(CustomerDisplayService);
  private router = inject(Router);
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

  // Keyboard shortcuts state
  private readonly keyboardShortcuts = [
    { key: 'F2', action: () => this.focusProductSearch(), description: 'Recherche produit' },
    { key: 'F3', action: () => this.focusCustomerSearch(), description: 'Recherche client' },
    { key: 'F4', action: () => this.onSaveAsPresale(), description: 'Mise en attente' },
    { key: 'F9', action: () => this.onSave(), description: 'Finaliser' },
    { key: 'F10', action: () => this.onCancel(), description: 'Annuler' },
  ];

  constructor() {
    // Effect pour surveiller les erreurs
    effect(() => {
      const error = this.lastError();
      if (error) {
        this.notificationService.error('Erreur', error);
      }
    });

    // Effect pour surveiller l'état de sauvegarde
    effect(() => {
      if (this.isSaving()) {
        this.spinner.show('sale-spinner');
      } else {
        this.spinner.hide('sale-spinner');
      }
    });

    // Effect: CLIENT OBLIGATOIRE pour ASSURANCE
    effect(() => {
      const sale = this.currentSale();
      if (sale && !sale.customerId) {
        this.notificationService.warning('Client requis', 'Un client assuré est obligatoire pour une vente ASSURANCE');
      }
    });
  }

  ngOnInit(): void {
    // Initialiser une vente ASSURANCE
    this.facade.initializeAssuranceSale();

    // Initialize typePrescription with default value
    this.facade.setTypePrescription('PRESCRIPTION');
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

  onProductSelected(product: ProduitSearch | null): void {
    if (!product) {
      return;
    }

    if (!this.hasCustomer()) {
      this.notificationService.warning('Client requis', "Veuillez sélectionner un client assuré avant d'ajouter des produits");
      return;
    }

    this.facade.setSelectedProduct(product);

    // ✅ AJOUT Phase 2.2: Focus sur quantité après sélection
    setTimeout(() => {
      this.quantityComponent()?.focusProduitControl();
      this.quantityComponent()?.reset(1);
    }, 100);
  }

  /**
   * ✅ AJOUT Phase 3: Scanner → ajout automatique avec quantité 1
   */
  onProductScanned(product: ProduitSearch): void {
    if (!product || !this.hasCustomer()) {
      if (!this.hasCustomer()) {
        this.notificationService.warning('Client requis', "Veuillez sélectionner un client assuré avant d'ajouter des produits");
      }
      return;
    }

    this.facade.setSelectedProduct(product);

    // ✅ Ajout automatique avec quantité 1
    const currentSale = this.currentSale();
    if (!currentSale) return;

    const line = createSalesLineFromProduct(product, 1, currentSale);
    this.facade.addSalesLine(line);

    // ✅ Reset et focus sur recherche
    this.resetProductSelection();
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
  onAddQuantity(quantity: number): void {
    const product = this.selectedProduct();
    if (!product || !quantity || quantity <= 0) {
      return;
    }

    if (!this.hasCustomer()) {
      this.notificationService.warning('Client requis', "Veuillez sélectionner un client assuré avant d'ajouter des produits");
      return;
    }

    const currentSale = this.currentSale();
    if (!currentSale) return;

    const line = createSalesLineFromProduct(product, quantity, currentSale);
    this.facade.addSalesLine(line);

    // ✅ AJOUT Phase 2.3: Reset après succès
    this.resetProductSelection();
  }

  /**
   * ✅ AJOUT Phase 2.3: Réinitialiser et focus après ajout produit
   */
  private resetProductSelection(): void {
    // Réinitialiser le produit sélectionné
    this.facade.setSelectedProduct(null);

    // Réinitialiser le composant de recherche
    this.productSearchComponent()?.reset();

    // Réinitialiser la quantité
    this.quantityComponent()?.reset(1);

    // Focus sur recherche produit
    setTimeout(() => {
      this.productSearchComponent()?.getFocus();
    }, 100);
  }

  onLineQuantityChanged(event: { lineId: number; quantity: number }): void {
    const line = this.salesLines().find(l => l.id === event.lineId);
    if (line && line.id) {
      this.facade.updateLineQuantity(line.id, event.quantity);
    }
  }

  onLineQuantityRequestedChanged(event: { lineId: number; quantityRequested: number }): void {
    const line = this.salesLines().find(l => l.id === event.lineId);
    if (line && line.id) {
      this.facade.updateLineQuantityRequested(line.id, event.quantityRequested);
    }

    // Retour du focus sur le champ produit (conforme ancien système)
    this.focusProductSearch();
  }

  onLineRemoved(lineId: number): void {
    const line = this.salesLines().find(l => l.id === lineId);
    if (line && line.id) {
      this.facade.removeSalesLine(line.id);
    }
  }

  onLineSelected(lineId: number): void {
    this.selectedLineId.set(lineId);
  }

  onLineDiscountChanged(event: { lineId: number; discount: number }): void {
    const line = this.salesLines().find(l => l.id === event.lineId);
    if (line && line.id) {
      this.facade.updateSalesLine(line.id, {
        regularUnitPrice: line.regularUnitPrice! * (1 - event.discount / 100),
      });
    }
  }

  onAuthorizationRequired(event: { lineId: number; discount: number }): void {
    this.authorizationService
      .requestDiscountAuthorization(event.discount)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: authorized => {
          if (authorized) {
            const line = this.salesLines().find(l => l.id === event.lineId);
            if (line && line.id) {
              this.facade.updateSalesLine(line.id, {
                regularUnitPrice: line.regularUnitPrice! * (1 - event.discount / 100),
              });
            }
          } else {
            this.notificationService.warning('Non autorisé', 'Remise refusée');
          }
        },
        error: err => {
          this.notificationService.error('Erreur', "Erreur lors de l'autorisation");
        },
      });
  }

  // ============================================
  // Gestion Client
  // ============================================

  onCustomerSelected(customer: ICustomer): void {
    this.facade.setCustomer(customer);
    this.customers.set([]);
  }

  onCustomerRemoved(): void {
    this.confirmationService.confirm({
      message: 'Êtes-vous sûr de vouloir retirer ce client ?',
      header: 'Retirer le client',
      icon: 'pi pi-exclamation-triangle',
      accept: () => {
        this.facade.removeCustomer();
      },
    });
  }

  onCustomerAdd(): void {
    // TODO: Ouvrir modal de création client assuré
    this.notificationService.info('À venir', 'Fonction de création de client assuré');
  }

  onCustomerSearchChange(searchTerm: string): void {
    if (searchTerm.length >= 2) {
      this.customerSearchService
        .search(searchTerm)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: (customers: ICustomer[]) => {
            this.customers.set(customers);
          },
          error: (err: any) => {
            this.notificationService.error('Erreur', 'Erreur lors de la recherche');
          },
        });
    } else {
      this.customers.set([]);
    }
  }

  // ============================================
  // Gestion Tiers Payants
  // ============================================

  onInsuranceDataUpdate(data: { customer: ICustomer; tiersPayants: IClientTiersPayant[] }): void {
    // Mettre à jour le client
    this.facade.setCustomer(data.customer);

    // Mettre à jour les tiers payants dans la vente courante
    const currentSale = this.currentSale();
    if (currentSale) {
      currentSale.tiersPayants = data.tiersPayants;
    }
  }

  onCustomerSelectedFromBar(customer: ICustomer): void {
    // Cloner l'objet pour forcer la réactivité
    const newCustomer = { ...customer, tiersPayants: [...(customer.tiersPayants || [])] };
    this.facade.setCustomer(newCustomer);

    // ✅ AJOUT: Créer la vente si elle n'existe pas
    const currentSale = this.currentSale();
    if (!currentSale) {
      this.facade.initializeAssuranceSale();
    }

    // ✅ AJOUT: Mettre à jour les tiers payants de la vente
    if (newCustomer.tiersPayants) {
      this.facade.updateSaleTiersPayants(newCustomer.tiersPayants);
    }

    // Focus sur le premier champ de numéro de bon
    setTimeout(() => this.insuranceDataBar()?.focusFirstBon(), 100);
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
          this.facade.setCustomer(customer);
          const currentSale = this.currentSale();
          if (currentSale && customer.tiersPayants) {
            currentSale.tiersPayants = customer.tiersPayants;
          }
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
      this.notificationService.warning('Client requis', "Sélectionnez d'abord un client assuré");
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
      this.notificationService.warning('Client requis', "Sélectionnez d'abord un client assuré");
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
        if (newTiersPayant && currentSale) {
          const updatedTiersPayants = [...(currentSale.tiersPayants || []), newTiersPayant];
          currentSale.tiersPayants = updatedTiersPayants;

          // Mettre à jour via la facade si besoin
          // TODO: Ajouter méthode facade.addThirdParty si nécessaire
        }
      },
      'xl',
    );
  }

  onRemoveTiersPayant(tiersPayant: IClientTiersPayant): void {
    const currentSale = this.currentSale();
    if (!currentSale?.tiersPayants) return;

    this.confirmationService.confirm({
      message: `Êtes-vous sûr de vouloir supprimer le tiers payant ${tiersPayant.tiersPayantName} ?`,
      header: 'Supprimer tiers payant',
      icon: 'pi pi-exclamation-triangle',
      accept: () => {
        const updatedTiersPayants = currentSale.tiersPayants!.filter(tp => tp.id !== tiersPayant.id);
        currentSale.tiersPayants = updatedTiersPayants;

        // TODO: Appeler service backend pour supprimer
        // this.facade.removeThirdParty(tiersPayant.id);
      },
    });
  }

  onTiersPayantsChanged(tiersPayants: IClientTiersPayant[]): void {
    const currentSale = this.currentSale();
    if (currentSale) {
      currentSale.tiersPayants = tiersPayants;
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
          const currentSale = this.currentSale();
          if (currentSale && updatedCustomer.tiersPayants) {
            currentSale.tiersPayants = updatedCustomer.tiersPayants;
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
    const sale = this.currentSale();
    if (!sale) {
      this.notificationService.warning('Vente vide', 'Aucune vente à enregistrer');
      return;
    }

    if (this.salesLines().length === 0) {
      this.notificationService.warning('Vente vide', 'Ajoutez au moins un produit');
      return;
    }

    if (!this.hasCustomer()) {
      this.notificationService.warning('Client requis', 'Un client assuré est obligatoire');
      return;
    }

    // Vérifier que les tiers payants sont renseignés via la vente courante
    const currentSaleData = this.currentSale();
    if (!currentSaleData?.tiersPayants || currentSaleData.tiersPayants.length === 0) {
      this.notificationService.warning('Tiers payants requis', 'Ajoutez au moins un tiers payant');
      return;
    }

    // Vérifier que tous les numéros de bon sont renseignés
    const missingBonNumbers = currentSaleData.tiersPayants.filter(tp => !tp.numBon);
    if (missingBonNumbers.length > 0) {
      this.notificationService.warning('Numéros de bon requis', 'Veuillez renseigner tous les numéros de bon');
      return;
    }

    // Si c'est un avoir, confirmer
    if (this.isAvoir()) {
      this.confirmationService.confirm({
        message: 'Cette vente sera enregistrée comme AVOIR (quantité demandée ≠ quantité servie). Confirmer ?',
        header: 'Avoir détecté',
        icon: 'pi pi-exclamation-triangle',
        accept: () => this.proceedWithSave(),
      });
    } else {
      this.proceedWithSave();
    }
  }

  private proceedWithSave(): void {
    // La validation est déclenchée, le payment-mode est déjà affiché
    // Pas besoin de modal, l'utilisateur finalise via le payment-mode en bas de l'écran
  }

  /**
   * Finalise la vente sans paiement (amountToBePaid <= 0)
   */
  private finalizeSaleWithoutPayment(): void {
    const currentSale = this.currentSale();
    if (!currentSale) return;

    // Valider toutes les contraintes avant finalisation
    if (!this.hasCustomer()) {
      this.notificationService.warning('Client requis', 'Un client assuré est obligatoire');
      return;
    }

    if (!currentSale.tiersPayants || currentSale.tiersPayants.length === 0) {
      this.notificationService.warning('Tiers payants requis', 'Ajoutez au moins un tiers payant');
      return;
    }

    const missingBonNumbers = currentSale.tiersPayants.filter(tp => !tp.numBon);
    if (missingBonNumbers.length > 0) {
      this.notificationService.warning('Numéros de bon requis', 'Veuillez renseigner tous les numéros de bon');
      return;
    }

    // Montant entièrement couvert par assurance
    const emptyPaymentEvent: PaymentCompleteEvent = {
      totalPaid: 0,
      payments: [],
      change: 0,
      changeExact: 0,
      printReceipt: false,
      printInvoice: false,
    };

    this.onPaymentComplete(emptyPaymentEvent);
  }

  onPaymentComplete(event: PaymentCompleteEvent): void {
    // Enregistrer la vente via la facade
    this.facade.saveAssuranceSale(event.payments).subscribe({
      next: result => {
        if (result) {
          // Succès : impression si demandée
          if (event.printReceipt && result.saleId) {
            this.facade.printReceipt(result.saleId);
          }

          this.resetForNewSale();

          // Basculer vers COMPTANT après finalisation
          this.switchToComptant.emit();
        } else {
          // Échec : afficher l'erreur et garder la vente
          this.notificationService.error('Erreur', 'La sauvegarde de la vente a échoué');
        }
      },
      error: () => {
        // Échec : afficher l'erreur et garder la vente
        this.notificationService.error('Erreur', 'La sauvegarde de la vente a échoué');
      },
    });
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
      this.notificationService.warning('Impression impossible', "La vente doit être enregistrée d'abord");
    }
  }

  onSaveAsPresale(): void {
    const sale = this.currentSale();
    if (!sale || this.salesLines().length === 0) {
      this.notificationService.warning('Vente vide', 'Ajoutez au moins un produit');
      return;
    }

    if (!this.hasCustomer()) {
      this.notificationService.warning('Client requis', 'Un client assuré est obligatoire');
      return;
    }

    this.facade.putOnStandby();
  }

  onCancel(): void {
    if (this.salesLines().length === 0) {
      this.router.navigate(['/sales']);
      return;
    }

    this.confirmationService.confirm({
      message: 'Êtes-vous sûr de vouloir annuler cette vente ? Toutes les données seront perdues.',
      header: 'Annuler la vente',
      icon: 'pi pi-exclamation-triangle',
      accept: () => {
        this.resetForNewSale();
        this.router.navigate(['/sales']);
      },
    });
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

  @HostListener('window:keydown', ['$event'])
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
    // Appeler la méthode getFocus() du ProductSearchComponent enfant
    setTimeout(() => {
      this.productSearchComponent()?.getFocus();
    }, 100);
  }

  private focusCustomerSearch(): void {
    // Focus sur le champ de recherche client dans la barre assurance
    setTimeout(() => {
      this.insuranceDataBar()?.searchInput()?.nativeElement.focus();
    }, 100);
  }

  private resetForNewSale(): void {
    this.facade.resetCurrentSale();
    this.selectedLineId.set(null);
    this.customers.set([]);
    // L'insurance data bar se réinitialisera automatiquement avec la vente
  }

  getCustomerDisplay(customer: ICustomer | null): string {
    if (!customer) return '';
    return `${customer.firstName || ''} ${customer.lastName || ''} ${customer.phone || ''}`;
  }
}
