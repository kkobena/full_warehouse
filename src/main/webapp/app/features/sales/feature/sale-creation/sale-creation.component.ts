import { Component, OnInit, inject, signal, DestroyRef, effect, HostListener, viewChild, output, input, model } from '@angular/core';
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
import { NgxSpinnerService } from 'ngx-spinner';
import { ConfirmDialogComponent } from '../../../../shared/dialog/confirm-dialog/confirm-dialog.component';
import {
  ProductListComponent,
  SaleSummaryComponent,
  CustomerSelectorComponent,
  SaleActionsComponent,
  PendingSalesListComponent,
  ProductSearchComponent,
} from '../../ui';
import { PaymentModeComponent, PaymentCompleteEvent } from '../../ui/payment-mode/payment-mode.component';
import { SaleTypeSelectorComponent, SaleType } from '../../ui/sale-type-selector/sale-type-selector.component';
import { CustomerOverlayPanelComponent } from '../../../../entities/sales/customer-overlay-panel/customer-overlay-panel.component';
import { CashRegisterFormComponent } from '../../../../entities/cash-register/user-cash-register/cash-register-form/cash-register-form.component';
import { UninsuredCustomerListComponent } from '../../../../entities/sales/uninsured-customer-list/uninsured-customer-list.component';
import { UninsuredCustomerFormComponent } from '../../../../entities/customer/uninsured-customer-form/uninsured-customer-form.component';
import { QuantiteProdutSaisieComponent } from '../../../../shared/quantite-produt-saisie/quantite-produt-saisie.component';
import { showCommonModal } from '../../../../entities/sales/selling-home/sale-helper';
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
export class SaleCreationComponent implements OnInit {
  // ViewChild pour accéder au composant produit search
  productSearchComponent = viewChild<ProductSearchComponent>('produitbox');
  confirmDialog = viewChild.required<ConfirmDialogComponent>('confirmDialog');
  
  // Output pour notifier le container du succès de l'ajout (règle métier: reset après succès)
  productAddedSuccess = output<void>();
  
  // Modal and responsive state
  readonly isCashRegisterOpen = model(false);
  readonly isSmallScreen = input(false);

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
  salesLines = this.facade.salesLines;
  selectedCustomer = this.facade.selectedCustomer;
  selectedProduct = this.facade.selectedProduct;
  canSave = this.facade.canSave;
  isSaving = this.facade.isSaving;
  loading = this.facade.loading;
  lastError = this.facade.lastError;

  // Local UI state
  customers = signal<ICustomer[]>([]);
  selectedLineId = signal<number | null>(null);
  showPaymentDetails = signal(false);
  showPaymentModal = signal(false);
  selectedSaleType = signal<SaleType>('COMPTANT');
  
  // UI state for sidebar and pending sales
  sidebarCollapsed = signal(false);
  pendingSalesSidebar = signal(false);
  pendingSalesCount = signal(0);
  
  // Vendeur sélectionné
  selectedSeller = signal<IUser | null>(null);

  constructor() {
    // Observer les erreurs du store
    effect(() => {
      const errorMsg = this.lastError();
      if (errorMsg) {
        console.error('Sale error:', errorMsg);
        this.notificationService.error('Erreur', errorMsg);
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

    // Initialize customer display
    this.customerDisplay.initialize('PHARMA SMART');

    // Charger le nombre de ventes en attente
    this.loadPendingSalesCount();

    // Initialiser le vendeur avec celui du store
    const currentSeller = this.facade.seller();
    if (currentSeller) {
      this.selectedSeller.set(currentSeller);
    }
  }

  // ===== Handlers pour ProductSearchComponent =====

  /**
   * Gère la sélection manuelle d'un produit depuis l'autocomplete
   */
  onProductSelected(product: ProduitSearch | null): void {
    if (!product) {
      return;
    }

    this.facade.setSelectedProduct(product);
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
    
    // Le reset sera fait via productAddedSuccess depuis le parent
    // (Seulement si l'ajout réussit)
  }

  /**
   * Gère le scan d'un code-barres
   */
  onProductScanned(product: ProduitSearch): void {
    this.facade.setSelectedProduct(product);
    this.addProductToSale(product, 1);
    
    // Le reset sera fait via productAddedSuccess depuis le parent
    // (Seulement si l'ajout réussit)
  }

  /**
   * Ajoute un produit à la vente
   * Ne clear la sélection et n'émet l'event qu'en cas de succès
   */
  private addProductToSale(product: ProduitSearch, quantity: number): void {
    const currentSale = this.facade.currentSale();
    const salesLine = createSalesLineFromProduct(product, quantity, currentSale);

    // Update customer display
    this.customerDisplay.updateDisplayForProduct(
      product.libelle || '',
      quantity,
      product.regularUnitPrice || 0
    );

    // Observer l'état d'erreur pour détecter les échecs
    const initialError = this.lastError();

    // Si pas de vente en cours, créer avec ce premier produit
    if (!currentSale || !currentSale.saleId) {
      this.facade.createComptant(salesLine);
    } else {
      // Sinon ajouter à la vente existante
      this.facade.onAddProduit(salesLine);
    }

    // Attendre le résultat de l'opération avant de clear
    // Vérifier si une erreur s'est produite
    setTimeout(() => {
      const currentError = this.lastError();
      
      // Si pas d'erreur OU l'erreur n'a pas changé → Succès
      if (!currentError || currentError === initialError) {
        // Clear selection seulement en cas de succès
        this.facade.setSelectedProduct(null);
        
        // Notifier le container que l'ajout est réussi
        this.productAddedSuccess.emit();
      } else {
        // En cas d'erreur, le produit reste sélectionné
        // L'utilisateur peut réessayer ou corriger
        console.error('Échec ajout produit:', currentError);
      }
    }, 200);
  }

  // ===== Handlers pour ProductListComponent =====

  onLineQuantityChanged(data: { line: ISalesLine; newQty: number }): void {
    if (data.line.id) {
      this.facade.updateLineQuantity(data.line.id, data.newQty);
    }
    
    // Retour du focus sur le champ produit
    setTimeout(() => {
      this.productSearchComponent()?.getFocus();
    }, 100);
  }

  onLineQuantityRequestedChanged(data: { line: ISalesLine; newQty: number }): void {
    if (data.line.id) {
      this.facade.updateLineQuantityRequested(data.line.id, data.newQty);
    }
    
    // Retour du focus sur le champ produit
    setTimeout(() => {
      this.productSearchComponent()?.getFocus();
    }, 100);
  }

  onLineRemoved(line: ISalesLine): void {
    if (line.saleLineId) {
      this.facade.removeLine(line.saleLineId);
    }
    
    // Retour du focus sur le champ produit
    setTimeout(() => {
      this.productSearchComponent()?.getFocus();
    }, 100);
  }

  onAuthorizationRequired(event: { line: ISalesLine; action: 'delete' | 'discount' }): void {
    const saleId = this.currentSale()?.id;
    const saleType = this.selectedSaleType();

    if (event.action === 'delete') {
      this.authorizationService.requestDeleteProductAuthorization(saleId, saleType)
        .subscribe(authorized => {
          if (authorized && event.line.saleLineId) {
            this.facade.removeLine(event.line.saleLineId);
          }
        });
    } else if (event.action === 'discount') {
      this.authorizationService.requestDiscountAuthorization(saleId, saleType)
        .subscribe(authorized => {
          if (authorized) {
            // TODO: Ouvrir modal de saisie remise
            console.log('Authorization granted for discount');
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
        'Voulez-vous vraiment supprimer la remise appliquée?'
      );
    } else {
      // Demander autorisation pour supprimer
      this.requestRemiseRemovalAuthorization();
    }
  }

  private requestRemiseAuthorization(): void {
    const saleId = this.currentSale()?.id;
    const saleType = this.selectedSaleType();

    this.authorizationService.requestDiscountAuthorization(saleId, saleType)
      .subscribe(authorized => {
        if (authorized) {
          this.openRemiseSelectionModal();
        }
      });
  }

  private requestRemiseRemovalAuthorization(): void {
    const saleId = this.currentSale()?.id;
    const saleType = this.selectedSaleType();

    this.authorizationService.requestDiscountAuthorization(saleId, saleType)
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

  onCustomerSearchChange(searchTerm: string): void {
    if (searchTerm && searchTerm.length >= 2) {
      this.customerSearchService
        .search(searchTerm, 10)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe(customers => {
          this.customers.set(customers);
        });
    } else {
      this.customers.set([]);
    }
  }

  onCustomerSelected(customer: ICustomer): void {
    this.facade.setCustomer(customer);
    
    // Retour du focus sur le champ produit
    setTimeout(() => {
      this.productSearchComponent()?.getFocus();
    }, 100);
  }

  onCustomerRemoved(): void {
    this.facade.removeCustomer();
    
    // Retour du focus sur le champ produit
    setTimeout(() => {
      this.productSearchComponent()?.getFocus();
    }, 100);
  }

  onCustomerAdd(): void {
    // Ouvrir formulaire de création client standard (non assuré)
    this.openUninsuredCustomerForm();
  }

  /**
   * Ouvre le formulaire de création d'un nouveau client standard
   */
  private openUninsuredCustomerForm(): void {
    const modalRef = this.modalService.open(UninsuredCustomerFormComponent, {
      size: 'lg',
      backdrop: 'static',
      centered: true,
    });

    modalRef.componentInstance.entity = null;
    modalRef.componentInstance.title = 'CRÉATION CLIENT STANDARD';

    modalRef.result.then(
      (customer: ICustomer) => {
        if (customer && customer.id) {
          this.facade.setCustomer(customer);
          
          // Retour du focus sur le champ produit
          setTimeout(() => {
            this.productSearchComponent()?.getFocus();
          }, 100);
        }
      },
      () => {
        // Modal fermée sans création - pas de problème
      }
    );
  }

  // ===== Handlers pour SaleActionsComponent =====

  onSave(): void {
    const sale = this.facade.currentSale();
    if (!sale || !this.canSave()) {
      this.notificationService.warning(
        'Vente invalide',
        'Veuillez ajouter au moins un produit avant d\'enregistrer la vente'
      );
      return;
    }

    // Ouvrir le drawer de paiement
    this.showPaymentModal.set(true);
  }

  onPaymentComplete(event: PaymentCompleteEvent): void {
    console.log('Payment complete:', event);
    
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
        `Le montant versé (${entryAmount} FCFA) est inférieur au montant dû (${amountToBePaid} FCFA).\n\nVoulez-vous régler le reste (${restToPay} FCFA) en différé ?`
      );
    } else {
      // Montant suffisant ou déjà différé → Finaliser normalement
      this.finalizeSale(event);
    }
  }

  onPaymentError(error: string): void {
    this.notificationService.error('Erreur de paiement', error);
  }

  onSaveAndPrint(): void {
    const sale = this.facade.currentSale();
    if (!sale || !this.canSave()) {
      this.notificationService.warning(
        'Vente invalide',
        'Veuillez ajouter au moins un produit avant d\'enregistrer la vente'
      );
      return;
    }

    // Activer l'impression automatique après la sauvegarde
    this.facade.setPrintReceipt(true);
    
    // Sauvegarder (l'impression sera déclenchée après succès)
    this.facade.saveSale();
    
    setTimeout(() => {
      if (!this.facade.isSaving() && !this.facade.lastError() && sale.saleId) {
        this.facade.printReceipt(sale.saleId);
        this.resetForNewSale();
      }
    }, 500);
  }

  onPrint(): void {
    const sale = this.currentSale();
    if (sale?.saleId) {
      this.facade.printCurrentSale();
    } else {
      this.notificationService.warning(
        'Impression impossible',
        'La vente doit être enregistrée avant impression'
      );
    }
  }

  onCancel(): void {
    const hasLines = this.salesLines().length > 0;
    
    if (hasLines) {
      // Si la vente a des lignes, demander confirmation
      this.confirmDialog().onConfirm(
        () => this.resetForNewSale(),
        'Annulation de la vente',
        'Voulez-vous vraiment annuler cette vente ? Toutes les données seront perdues.'
      );
    } else {
      // Si pas de lignes, annuler directement
      this.resetForNewSale();
    }
  }

  onSaveAsPresale(): void {
    const sale = this.currentSale();
    if (!sale || this.salesLines().length === 0) {
      this.notificationService.warning(
        'Vente vide',
        'Ajoutez au moins un produit avant de mettre en attente'
      );
      return;
    }

    // Mettre la vente en attente via le facade
    this.facade.putOnStandby();
  }

  // ===== Navigation et utilitaires =====

  /**
   * Retour à l'écran précédent (quitter le point de vente)
   * Utilisé uniquement par le bouton Retour dans le header
   */
  onNavigateBack(): void {
    this.router.navigate(['/']);
  }

  /**
   * Réinitialiser pour une nouvelle vente
   * Appelé après sauvegarde ou annulation pour rester sur l'écran de vente
   */
  private resetForNewSale(): void {
    this.facade.cancelSale();
    this.customerDisplay.clear();
    this.customers.set([]);
    this.selectedLineId.set(null);
    this.showPaymentModal.set(false);
    
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
        this.isCashRegisterOpen.set(resp);
        // Finaliser la vente après ouverture de la caisse
        this.completeSaleAfterCashRegister();
      }
    });
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

  /**
   * Confirme la vente en différé
   */
  private confirmDiffereSale(event: PaymentCompleteEvent): void {
    const currentSale = this.facade.currentSale();
    if (!currentSale) return;

    currentSale.differe = true;

    // Vérifier si un client est associé
    if (!currentSale.customerId) {
      // Ouvrir modal client (obligatoire pour vente différée)
      this.openCustomerModalForDiffere(() => {
        this.finalizeSale(event);
      });
    } else {
      this.finalizeSale(event);
    }
  }

  /**
   * Ouvre la modal de sélection client pour vente différée
   */
  private openCustomerModalForDiffere(onSuccess: () => void): void {
    const modalRef = this.modalService.open(UninsuredCustomerListComponent, {
      size: 'lg',
      backdrop: 'static',
      centered: true,
    });
    modalRef.componentInstance.header = 'SÉLECTION CLIENT - Vente différée';

    modalRef.result.then(
      (customer) => {
        if (customer && customer.id) {
          this.facade.setCustomer(customer);
          const currentSale = this.facade.currentSale();
          if (currentSale) {
            currentSale.customerId = customer.id;
          }
          onSuccess();
        } else {
          this.notificationService.warning(
            'Client requis',
            'Un client est obligatoire pour une vente différée'
          );
          this.showPaymentModal.set(false);
        }
      },
      () => {
        // Modal fermée sans sélection
        this.notificationService.warning(
          'Vente annulée',
          'Un client est obligatoire pour une vente différée'
        );
        this.showPaymentModal.set(false);
      }
    );
  }

  /**
   * Finalise la vente (avec ou sans différé)
   */
  private finalizeSale(event: PaymentCompleteEvent): void {
    // Vérifier si la caisse est ouverte avant de finaliser
    if (!this.isCashRegisterOpen()) {
      this.openCashRegister();
    } else {
      this.completeSaleAfterCashRegister();
    }
  }

  /**
   * Finalise la vente après l'ouverture de la caisse
   */
  private completeSaleAfterCashRegister(): void {
    // Vérifier si avoir (livraison partielle) sans client
    const isAvoir = this.facade.isAvoir();
    const hasCustomer = this.facade.hasCustomer();
    
    if (isAvoir && !hasCustomer) {
      // Client obligatoire pour avoir - ouvrir modal
      this.openCustomerModalForAvoir(() => {
        // Après sélection client, réessayer sauvegarde
        this.saveAndComplete();
      });
      return;
    }
    
    // Sinon, sauvegarder directement
    this.saveAndComplete();
  }

  /**
   * Sauvegarde et finalise la vente
   */
  private saveAndComplete(): void {
    this.facade.saveSale();
    
    // Clear customer display after sale
    this.customerDisplay.clear();
    
    // Fermer le drawer
    this.showPaymentModal.set(false);
    
    // Réinitialiser pour nouvelle vente après sauvegarde
    setTimeout(() => {
      if (!this.facade.isSaving() && !this.facade.lastError()) {
        this.resetForNewSale();
      }
    }, 500);
  }

  /**
   * Ouvre la modal de sélection client pour avoir
   */
  private openCustomerModalForAvoir(onSuccess: () => void): void {
    const modalRef = this.modalService.open(UninsuredCustomerListComponent, {
      size: 'lg',
      backdrop: 'static',
      centered: true,
    });
    modalRef.componentInstance.header = 'SÉLECTION CLIENT - Livraison partielle (Avoir)';

    modalRef.result.then(
      (customer) => {
        if (customer && customer.id) {
          this.facade.setCustomer(customer);
          const currentSale = this.facade.currentSale();
          if (currentSale) {
            currentSale.customerId = customer.id;
          }
          onSuccess();
        } else {
          this.notificationService.warning(
            'Client requis',
            'Un client est obligatoire pour une vente avec avoir (livraison partielle)'
          );
          this.showPaymentModal.set(false);
        }
      },
      () => {
        // Modal fermée sans sélection
        this.notificationService.warning(
          'Vente annulée',
          'Un client est obligatoire pour une vente avec avoir (livraison partielle)'
        );
        this.showPaymentModal.set(false);
      }
    );
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
        `Voulez-vous vraiment changer le type de vente vers ${saleType}?\n\nAttention: Les données actuelles seront perdues.`
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
        this.notificationService.info(
          `Type de vente: ${saleType}`,
          'Fonctionnalité disponible en Phase 8'
        );
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
    
    // Retour du focus sur le champ produit
    setTimeout(() => {
      this.productSearchComponent()?.getFocus();
    }, 100);
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
    setTimeout(() => {
      this.productSearchComponent()?.getFocus();
    }, 100);
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

  @HostListener('window:keydown', ['$event'])
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
        this.notificationService.warning('Aucun produit', 'Veuillez sélectionner un produit d\'abord');
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
        this.notificationService.warning('Vente invalide', 'Impossible d\'enregistrer la vente');
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
        this.showPaymentModal.set(true);
      } else {
        this.notificationService.warning('Vente vide', 'Ajoutez des produits avant d\'ouvrir le paiement');
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
