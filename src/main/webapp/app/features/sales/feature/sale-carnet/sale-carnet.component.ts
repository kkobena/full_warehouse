import { Component, inject, OnInit, AfterViewInit, signal, computed, viewChild, output, effect, input, model } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MessageService } from 'primeng/api';
import { Toast } from 'primeng/toast';
import { TooltipModule } from 'primeng/tooltip';
import { NgxSpinnerService } from 'ngx-spinner';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { ConfirmDialogComponent } from '../../../../shared/dialog/confirm-dialog/confirm-dialog.component';
import { ProductSearchComponent, ProductListComponent, SaleSummaryComponent, SaleActionsComponent } from '../../ui';
import { InsuranceDataBarComponent } from '../../ui';
import { PaymentModeComponent, PaymentCompleteEvent } from '../../ui/payment-mode/payment-mode.component';
import { RemiseSelectionModalComponent } from '../../ui/remise-selection-modal/remise-selection-modal.component';
import { CashRegisterFormComponent } from '../../../../entities/cash-register/user-cash-register/cash-register-form/cash-register-form.component';
import { CustomerCarnetComponent } from '../../../../entities/customer/carnet/customer-carnet.component';
import { UninsuredCustomerListComponent } from '../../../../entities/sales/uninsured-customer-list/uninsured-customer-list.component';
import { QuantiteProdutSaisieComponent } from '../../../../shared/quantite-produt-saisie/quantite-produt-saisie.component';
import { showCommonModal } from '../../../../entities/sales/selling-home/sale-helper';
import { SalesFacade } from '../../data-access/facades/sales.facade';
import { AuthorizationService } from '../../data-access/services/authorization.service';
import { NotificationService } from '../../../../shared/services/notification.service';
import { CustomerDisplayService } from '../../data-access/services/customer-display.service';
import { ISalesLine } from '../../../../shared/model';
import { ProduitSearch } from '../../../../shared/model';
import { IPaymentMode } from '../../../../shared/model/payment-mode.model';
import { IClientTiersPayant } from '../../../../shared/model';
import { ICustomer } from '../../../../shared/model';
import { IRemise } from '../../../../shared/model';
import { createSalesLineFromProduct } from '../../data-access/utils/sales-line.utils';

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
  imports: [
    CommonModule,
    FormsModule,
    TooltipModule,
    Toast,
    ProductSearchComponent,
    InsuranceDataBarComponent,
    ProductListComponent,
    SaleSummaryComponent,
    SaleActionsComponent,
    PaymentModeComponent,
    ConfirmDialogComponent,
    QuantiteProdutSaisieComponent,
  ],
  providers: [MessageService],
})
export class SaleCarnetComponent implements OnInit, AfterViewInit {
  // Services
  private messageService = inject(MessageService);

  productSearchComponent = viewChild<ProductSearchComponent>('produitbox');
  quantityComponent = viewChild<QuantiteProdutSaisieComponent>('quantityBox');
  insuranceDataBar = viewChild<InsuranceDataBarComponent>('insuranceDataBar');
  paymentModeComponent = viewChild<PaymentModeComponent>('paymentMode');
  private confirmDialog = viewChild.required<ConfirmDialogComponent>('confirmDialog');

  /**
   * Méthode publique pour mettre le focus sur la recherche produit
   * Appelée par le composant parent lors du changement de tab
   */
  public focusProductSearch(): void {
    setTimeout(() => {
      this.productSearchComponent()?.getFocus();
    }, 100);
  }

  // Modal and responsive state
  readonly isCashRegisterOpen = input(false);
  readonly isSmallScreen = input(false);

  // Outputs
  switchToComptant = output<void>();
  cashRegisterOpened = output<void>();

  // Services
  private facade = inject(SalesFacade);
  private authorizationService = inject(AuthorizationService);
  private router = inject(Router);
  private notificationService = inject(NotificationService);
  private customerDisplay = inject(CustomerDisplayService);
  private spinner = inject(NgxSpinnerService);
  private modalService = inject(NgbModal);

  // State signals
  readonly saleType = signal<'CARNET'>('CARNET');
  readonly currentSale = this.facade.currentSale;
  readonly salesLines = this.facade.salesLines;
  readonly selectedCustomer = this.facade.selectedCustomer;
  readonly selectedProduct = this.facade.selectedProduct;
  readonly loading = this.facade.loading;
  readonly isSaving = this.facade.isSaving;
  readonly isProcessingSale = signal(false);
  readonly selectedLineId = signal<number | null>(null);
  readonly waitingForForceStockSuccess = signal<boolean>(false);
  readonly previousLoadingState = signal<boolean>(false);
  readonly forceStockContext = signal<'addProduct' | 'editCell' | null>(null);
  readonly lastError = this.facade.lastError;

  // Focus management
  private focusInitialized = false;

  // Computed signals
  readonly canSave = computed(() => {
    const sale = this.currentSale();
    const lines = this.salesLines();
    const customer = this.selectedCustomer();
    return !!sale && lines.length > 0 && !!customer && !this.isSaving();
  });

  // Helper method pour savoir si un client est sélectionné
  hasCustomer = computed(() => !!this.selectedCustomer());

  constructor() {
    // Observer les erreurs du store pour force stock
    effect(() => {
      const errorMsg = this.lastError();
      const errorDetails = this.facade.errorDetails();
      const waiting = this.waitingForForceStockSuccess();

      if (waiting) return;

      if (errorMsg) {
        if (errorDetails?.errorKey === 'stock' && this.authorizationService.canForceStock()) {
          const isFromTableEdit = errorDetails.isFromTableCellEdit === true;
          const detectedContext = isFromTableEdit ? 'editCell' : 'addProduct';
          this.forceStockContext.set(detectedContext);

          this.confirmDialog().onConfirm(
            () => {
              if (errorDetails.attemptedLine) {
                errorDetails.attemptedLine.forceStock = true;
                this.waitingForForceStockSuccess.set(true);

                if (detectedContext === 'editCell') {
                  this.facade.updateItemQtyRequestedWithSet(errorDetails.attemptedLine);
                } else if (errorDetails.attemptedLine.id) {
                  this.facade.updateItemQtyRequested(errorDetails.attemptedLine);
                } else {
                  const currentSale = this.currentSale();
                  if (!currentSale?.saleId) {
                    this.facade.createCarnetSale(errorDetails.attemptedLine);
                  } else {
                    this.facade.onAddProduitCarnet(errorDetails.attemptedLine);
                  }
                }
              }
            },
            'Forcer le stock',
            'La quantité saisie est supérieure à la quantité stock du produit. Voulez-vous continuer ?',
            undefined,
            () => {
              this.facade.clearError();
              const context = this.forceStockContext();
              this.forceStockContext.set(null);

              if (context === 'editCell') {
                const currentSale = this.currentSale();
                if (currentSale?.saleId) {
                  this.facade.loadSaleForEdit(currentSale.saleId);
                }
                setTimeout(() => this.resetProductSelection(), 200);
              } else {
                this.resetProductSelection();
              }
            },
          );
        } else {
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: errorMsg,
            life: 5000,
          });
        }
      }
    });

    // Effect pour détecter succès après force stock
    effect(() => {
      const loading = this.loading();
      const previousLoading = this.previousLoadingState();
      const waiting = this.waitingForForceStockSuccess();

      if (!waiting) {
        if (previousLoading !== loading) {
          this.previousLoadingState.set(loading);
        }
        return;
      }

      this.previousLoadingState.set(loading);

      if (previousLoading && !loading && !this.facade.errorDetails()) {
        this.waitingForForceStockSuccess.set(false);
        this.facade.clearError();

        const context = this.forceStockContext();
        this.forceStockContext.set(null);

        setTimeout(() => this.resetProductSelection(), 200);
      }
    });

    // Contrôler le spinner global selon l'état loading
    effect(() => {
      if (this.loading()) {
        this.spinner.show();
      } else {
        this.spinner.hide();
      }
    });
  }

  ngOnInit(): void {
    // Set sale type to CARNET
    this.facade.setSaleType('CARNET');

    // Initialize typePrescription with default value
    this.facade.setTypePrescription('PRESCRIPTION');

    // Initialize customer display
    this.customerDisplay.initialize('PHARMA SMART');
  }

  ngAfterViewInit(): void {
    // Force le focus sur la recherche client au chargement initial
    setTimeout(() => {
      this.insuranceDataBar()?.searchInput()?.nativeElement.focus();
    }, 200);
  }

  // ===== Handlers pour InsuranceDataBarComponent =====

  onProductSearchEnter(shouldSave: boolean): void {
    if (!shouldSave) return;

    const currentSale = this.currentSale();

    //  Si vente en cours avec des lignes
    if (currentSale && this.salesLines().length > 0) {
      const amountToBePaid = currentSale.amountToBePaid || 0;

      //  Si montant à payer <= 0, finaliser directement sans paiement
      if (amountToBePaid <= 0) {
        this.finalizeSaleWithoutPayment();
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

  onCustomerSelected(customer: ICustomer): void {
    this.facade.setCustomer(customer);
    // Focus sur le premier champ de numéro de bon
    setTimeout(() => this.insuranceDataBar()?.focusFirstBon(), 100);
  }

  onOpenCustomerList(): void {
    this.openCarnetCustomerModal();
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
    const currentSale = this.currentSale();
    if (currentSale) {
      currentSale.tiersPayants = tiersPayants;
    }
  }

  // ===== Product Management =====

  /**
   * ✅ MODIFIÉ Phase 2.2: Gère la sélection manuelle d'un produit
   * Focus automatique sur quantité après sélection
   */
  onProductSelected(product: ProduitSearch | null): void {
    if (!product) {
      return;
    }

    if (!this.hasCustomer()) {
      this.notificationService.warning('Client requis', "Veuillez sélectionner un client avant d'ajouter des produits");
      return;
    }

    this.facade.setSelectedProduct(product);

    // ✅ AJOUT: Focus sur quantité après sélection
    setTimeout(() => {
      this.quantityComponent()?.focusProduitControl();
      this.quantityComponent()?.reset(1);
    }, 100);
  }

  /**
   * Gère l'ajout de quantité depuis le composant QuantiteProdutSaisieComponent
   */
  onAddQuantity(quantity: number): void {
    const product = this.selectedProduct();
    if (!product || !quantity || quantity <= 0) {
      return;
    }

    this.addProductToSale(product, quantity);
  }

  /**
   * Gère le scan d'un code-barres
   */
  onProductScanned(product: ProduitSearch): void {
    if (!this.hasCustomer()) {
      this.notificationService.warning('Client requis', "Veuillez sélectionner un client avant d'ajouter des produits");
      return;
    }

    this.facade.setSelectedProduct(product);
    this.addProductToSale(product, 1);
  }

  /**
   * Ajoute un produit à la vente CARNET
   * Ne clear la sélection et n'émet l'event qu'en cas de succès
   */
  private addProductToSale(product: ProduitSearch, quantity: number): void {
    const currentSale = this.currentSale();
    const salesLine = createSalesLineFromProduct(product, quantity, currentSale);

    // Update customer display
    this.customerDisplay.updateDisplayForProduct(product.libelle || '', quantity, product.regularUnitPrice || 0);

    // Observer l'état d'erreur pour détecter les échecs
    const initialError = this.lastError();

    // Si pas de vente en cours, créer avec ce premier produit
    if (!currentSale || !currentSale.saleId) {
      this.facade.createCarnetSale(salesLine);
    } else {
      // Sinon ajouter à la vente existante
      this.facade.onAddProduitCarnet(salesLine);
    }

    // Attendre le résultat de l'opération avant de clear
    setTimeout(() => {
      const currentError = this.lastError();

      // Si pas d'erreur OU l'erreur n'a pas changé → Succès
      if (!currentError || currentError === initialError) {
        this.resetProductSelection();
      }
    }, 200);
  }

  /**
   * ✅ AJOUT Phase 2.3: Réinitialiser la sélection produit et focus
   * Appelée après ajout réussi d'un produit
   */
  private resetProductSelection(): void {
    this.facade.setSelectedProduct(null);
    this.productSearchComponent()?.reset();
    this.quantityComponent()?.reset(1);
    this.focusProductSearch();
  }

  // ===== Handlers pour ProductListComponent =====

  onLineQuantityChanged(data: { line: ISalesLine; newQty: number }): void {
    if (data.line.id) {
      this.facade.updateLineQuantity(data.line.id, data.newQty);
    }

    this.focusProductSearch();
  }

  onLineQuantityRequestedChanged(data: { line: ISalesLine; newQty: number }): void {
    if (data.line.id) {
      this.facade.updateLineQuantityRequested(data.line.id, data.newQty);
    }

    // Retour du focus sur le champ produit (conforme ancien système)
    this.focusProductSearch();
  }

  onLineRemoved(line: ISalesLine): void {
    if (line.saleLineId) {
      this.facade.removeLine(line.saleLineId);
    }

    this.focusProductSearch();
  }

  onLineSelected(line: ISalesLine): void {
    this.selectedLineId.set(line.id || null);
  }

  onLineDiscountChanged(data: { line: ISalesLine; newDiscount: number }): void {
    // Handle line discount if needed
  }

  onAuthorizationRequired(event: { line: ISalesLine; action: 'delete' | 'discount' }): void {
    // Handle authorization requirements if needed
  }

  // ===== Sale Actions =====

  /**
   * Finalise la vente sans paiement (amountToBePaid <= 0)
   */
  private finalizeSaleWithoutPayment(): void {
    const currentSale = this.currentSale();
    if (!currentSale) return;

    // Montant entièrement couvert par assurance/crédit
    currentSale.montantVerse = 0;
    currentSale.payments = [];

    // Vérifier si la caisse est ouverte avant de finaliser
    if (!this.isCashRegisterOpen()) {
      this.openCashRegister();
    } else {
      this.completeSaleAfterCashRegister();
    }
  }

  onSave(): void {
    const currentSale = this.currentSale();
    if (!currentSale || !this.canSave()) {
      this.notificationService.warning('Vente invalide', "Veuillez ajouter au moins un produit avant d'enregistrer la vente");
      return;
    }

    const amountToBePaid = currentSale.amountToBePaid || 0;

    // Si montant à payer <= 0, finaliser sans paiement
    if (amountToBePaid <= 0) {
      this.finalizeSaleWithoutPayment();
      return;
    }

    // Si montant à payer > 0, récupérer les paiements du composant payment-mode
    const paymentModeComp = this.paymentModeComponent();
    if (!paymentModeComp) {
      this.notificationService.error('Erreur', 'Composant de paiement non disponible');
      return;
    }

    // Vérifier qu'il y a au moins un paiement
    const selectedModes = paymentModeComp.selectedModes();
    if (!selectedModes || selectedModes.length === 0) {
      this.notificationService.warning('Paiement requis', 'Veuillez saisir au moins un mode de paiement');
      return;
    }

    // Construire l'événement de paiement
    const paymentEvent: PaymentCompleteEvent = {
      payments: selectedModes.map(entry => ({
        mode: entry.mode,
        amount: entry.amount || 0,
        amountEntered: entry.amountEntered,
      })),
      totalPaid: paymentModeComp.totalPaid(),
      change: paymentModeComp.changeAmount(),
      changeExact: paymentModeComp.changeExact(),
      printReceipt: false,
      printInvoice: false,
    };

    // Traiter le paiement
    this.onPaymentComplete(paymentEvent);
  }

  onPutOnHold(): void {
    this.facade.saveSale().subscribe();
  }

  onCancel(): void {
    // Confirm before canceling
    if (this.salesLines().length > 0) {
      this.confirmDialog().onConfirm(
        () => this.resetForNewSale(),
        'Annulation de la vente',
        'Êtes-vous sûr de vouloir annuler cette vente ?',
      );
    } else {
      this.resetForNewSale();
    }
  }

  /**
   * Réinitialiser pour une nouvelle vente
   * Appelé après sauvegarde ou annulation pour rester sur l'écran de vente
   */
  private resetForNewSale(): void {
    this.facade.cancelSale();
    this.customerDisplay.clear();
    this.isProcessingSale.set(false);
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
        // Finaliser la vente après ouverture de la caisse
        setTimeout(() => this.completeSaleAfterCashRegister(), 100);
      }
    });
  }

  /**
   * Finalise la vente après l'ouverture de la caisse
   */
  private completeSaleAfterCashRegister(): void {
    this.isProcessingSale.set(true);

    this.facade.saveSale().subscribe({
      next: result => {
        if (result) {
          // Succès : réinitialiser et basculer vers COMPTANT
          this.resetForNewSale();
          this.switchToComptant.emit();
        } else {
          // Échec : afficher l'erreur et garder la vente
          this.isProcessingSale.set(false);
          this.notificationService.error('Erreur', 'La sauvegarde de la vente a échoué');
        }
      },
      error: err => {
        // Échec : afficher l'erreur et garder la vente
        this.isProcessingSale.set(false);
        this.notificationService.error('Erreur', 'La sauvegarde de la vente a échoué');
      },
    });
  }

  // ===== Payment =====

  onPaymentComplete(event: PaymentCompleteEvent): void {
    const currentSale = this.facade.currentSale();
    if (!currentSale) {
      this.notificationService.error('Erreur', 'Aucune vente en cours');
      return;
    }

    // Calculer le montant restant à payer
    const amountToBePaid = currentSale.amountToBePaid || 0;
    const entryAmount = event.totalPaid || 0;
    const restToPay = amountToBePaid - entryAmount;

    // Enregistrer le montant versé et les paiements
    currentSale.montantVerse = entryAmount;
    currentSale.payments = this.convertPayments(event.payments);

    // Si montant insuffisant ET pas encore marqué comme différé
    if (restToPay > 0 && !currentSale.differe) {
      this.confirmDialog().onConfirm(
        () => this.confirmDiffereSale(event),
        'Vente différée',
        `Le montant versé (${entryAmount} FCFA) est inférieur au montant dû (${amountToBePaid} FCFA).\n\nVoulez-vous régler le reste (${restToPay} FCFA) en différé ?`,
      );
    } else {
      // Montant suffisant ou déjà différé → Finaliser normalement
      this.finalizeSale(event);
    }
  }

  /**
   * Convertit les paiements de PaymentCompleteEvent au format Payment[]
   */
  private convertPayments(eventPayments: Array<{ mode: IPaymentMode; amount: number; amountEntered?: number }>): any[] {
    return eventPayments.map(p => ({
      paymentMode: p.mode,
      paidAmount: p.amount,
      montantVerse: p.amountEntered || p.amount,
      netAmount: p.amount,
    }));
  }

  private confirmDiffereSale(event: PaymentCompleteEvent): void {
    const currentSale = this.facade.currentSale();
    if (!currentSale) return;

    currentSale.differe = true;

    // Vérifier si un client est associé (normalement oui pour CARNET)
    // Mais si pas de client, ouvrir modal
    if (!currentSale.customerId) {
      this.openCustomerModalForDiffere(() => {
        this.finalizeSale(event);
      });
    } else {
      this.finalizeSale(event);
    }
  }

  private openCustomerModalForDiffere(onSuccess: () => void): void {
    const modalRef = this.modalService.open(UninsuredCustomerListComponent, {
      size: 'lg',
      backdrop: 'static',
      centered: true,
    });
    modalRef.componentInstance.header = 'SÉLECTION CLIENT - Vente différée';

    modalRef.result.then(
      customer => {
        if (customer && customer.id) {
          this.facade.setCustomer(customer);
          const currentSale = this.facade.currentSale();
          if (currentSale) {
            currentSale.customerId = customer.id;
          }
          onSuccess();
        } else {
          this.notificationService.warning('Client requis', 'Un client est obligatoire pour une vente différée');
        }
      },
      () => {
        this.notificationService.warning('Vente annulée', 'Un client est obligatoire pour une vente différée');
      },
    );
  }

  private finalizeSale(event: PaymentCompleteEvent): void {
    // Vérifier si la caisse est ouverte avant de finaliser
    if (!this.isCashRegisterOpen()) {
      this.openCashRegister();
    } else {
      this.completeSaleAfterCashRegister();
    }
  }

  onPaymentError(error: string): void {
    this.notificationService.error('Erreur', error);
  }

  // ===== Handlers pour remise globale (SaleSummaryComponent) =====

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
    const saleType = 'CARNET';

    this.authorizationService.requestDiscountAuthorization(saleId, saleType).subscribe(authorized => {
      if (authorized) {
        this.openRemiseSelectionModal();
      }
    });
  }

  private requestRemiseRemovalAuthorization(): void {
    const saleId = this.currentSale()?.id;
    const saleType = 'CARNET';

    this.authorizationService.requestDiscountAuthorization(saleId, saleType).subscribe(authorized => {
      if (authorized) {
        this.facade.updateRemise(undefined);
      }
    });
  }

  private openRemiseSelectionModal(): void {
    const modalRef = this.modalService.open(RemiseSelectionModalComponent, {
      backdrop: 'static',
      centered: true,
      size: 'md',
    });

    modalRef.result.then(
      (remise: IRemise) => {
        if (remise) {
          this.facade.updateRemise(remise);
          this.notificationService.success('Remise appliquée', `Remise ${remise.valeur} appliquée avec succès`);
        }
      },
      () => {
        // Modal fermé sans sélection
      },
    );
  }

  /**
   * Ouvre le formulaire de création/édition client carnet
   */
  private openCarnetCustomerForm(customer: ICustomer | null): void {
    const modalRef = this.modalService.open(CustomerCarnetComponent, {
      size: 'xl',
      backdrop: 'static',
      centered: true,
    });

    modalRef.componentInstance.entity = customer;
    modalRef.componentInstance.title = customer ? 'MODIFICATION CLIENT CARNET' : 'NOUVEAU CLIENT CARNET';
    modalRef.componentInstance.categorie = 'CARNET';

    modalRef.result.then(
      updatedCustomer => {
        if (updatedCustomer && updatedCustomer.id) {
          this.facade.setCustomer(updatedCustomer);
        }
      },
      () => {
        // Modal fermée sans enregistrement
      },
    );
  }

  /**
   * Ouvre la modal de sélection client carnet
   * Client obligatoire pour vente CARNET
   */
  private openCarnetCustomerModal(): void {
    const modalRef = this.modalService.open(CustomerCarnetComponent, {
      size: 'xl',
      backdrop: 'static',
      centered: true,
    });

    modalRef.result.then(
      customer => {
        if (customer && customer.id) {
          this.facade.setCustomer(customer);
        } else {
          this.notificationService.warning('Client requis', 'Un client avec compte crédit est obligatoire pour une vente CARNET');
        }
      },
      () => {
        // Modal fermée sans sélection
        this.notificationService.info('Information', 'Veuillez sélectionner un client pour continuer la vente');
      },
    );
  }
}
