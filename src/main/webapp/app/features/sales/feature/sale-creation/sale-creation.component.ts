import { Component, OnInit, inject, signal, DestroyRef, effect, HostListener, viewChild, output, input, model, computed } from '@angular/core';
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
  CustomerSelectionModalComponent,
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
import { CashRegisterService } from '../../../../entities/cash-register/cash-register.service';
import { createSalesLineFromProduct } from '../../data-access/utils/sales-line.utils';

/**
 * Seuil de tolérance pour considérer qu'il y a un reste à payer
 * Si le reste est <= 5 FCFA, on considère que le paiement est complet
 */
const PAYMENT_TOLERANCE_THRESHOLD = 5;

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
  // ✅ AJOUT Phase 2.1: ViewChild pour gestion du focus
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

  // Modal and responsive state
  readonly isCashRegisterOpen = model(false);
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
  private cashRegisterService = inject(CashRegisterService);
  private messageService = inject(MessageService); // Instance locale pour le toast du composant

  // State depuis le store (signals computed)
  currentSale = this.facade.currentSale;
  salesLines = this.facade.salesLines;
  selectedCustomer = this.facade.selectedCustomer;
  selectedProduct = this.facade.selectedProduct;
  canSave = this.facade.canSave;
  isSaving = this.facade.isSaving;
  loading = this.facade.loading;
  lastError = this.facade.lastError;
  remises = signal<any[]>([]); // TODO: charger les remises depuis le service

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
  waitingForForceStockSuccess = signal<boolean>(false); // Flag pour détecter le succès après force stock
  previousLoadingState = signal<boolean>(false); // Track l'état précédent de loading
  forceStockContext = signal<'addProduct' | 'editCell' | null>(null); // Contexte du force stock: ajout vs modification

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
      const errorDetails = this.facade.errorDetails();
      const waiting = this.waitingForForceStockSuccess();
      
      // Si on est en train d'attendre le succès du force stock, ne pas réafficher le dialog
      if (waiting) {
        return;
      }
      
      if (errorMsg) {
        // Si erreur de stock ET l'utilisateur peut forcer le stock
        if (errorDetails?.errorKey === 'stock' && this.authorizationService.canForceStock()) {
          // Détecter le contexte via le flag explicite:
          // - isFromTableCellEdit = true → modification cellule tableau
          // - isFromTableCellEdit = false → ajout depuis recherche
          const isFromTableEdit = errorDetails.isFromTableCellEdit === true;
          const detectedContext = isFromTableEdit ? 'editCell' : 'addProduct';
          
          // Stocker le contexte pour l'utiliser après succès
          this.forceStockContext.set(detectedContext);
          
          // Afficher dialog de confirmation pour forcer le stock
          this.confirmDialog().onConfirm(
            () => {
              // L'utilisateur a confirmé, réessayer avec forceStock = true
              if (errorDetails.attemptedLine) {
                errorDetails.attemptedLine.forceStock = true;
                
                // Marquer qu'on attend le succès du force stock (AVANT l'appel API)
                this.waitingForForceStockSuccess.set(true);
                
                // Appeler la bonne méthode selon le contexte
                if (detectedContext === 'editCell') {
                  // Modification cellule → SET endpoint
                  this.facade.updateItemQtyRequestedWithSet(errorDetails.attemptedLine);
                } else if (errorDetails.attemptedLine.id) {
                  // Ajout produit existant → INCREMENT endpoint
                  this.facade.updateItemQtyRequested(errorDetails.attemptedLine);
                } else {
                  // Ajout nouveau produit
                  const currentSale = this.facade.currentSale();
                  if (!currentSale || !currentSale.saleId) {
                    // Pas de vente → Création de vente avec force stock
                    this.facade.createComptantSale(errorDetails.attemptedLine);
                  } else {
                    // Vente existe → ADD endpoint
                    this.facade.onAddProduit(errorDetails.attemptedLine);
                  }
                }
              }
            },
            'Forcer le stock',
            'La quantité saisie est supérieure à la quantité stock du produit. Voulez-vous continuer ?',
            undefined,
            () => {
              // Annulation
              this.facade.clearError();
              
              const context = this.forceStockContext();
              this.forceStockContext.set(null); // Clear le contexte
              
              if (context === 'editCell') {
                // Contexte: modification cellule → Recharger la vente pour restaurer l'ancienne valeur
                const currentSale = this.facade.currentSale();
                if (currentSale?.saleId) {
                  this.facade.loadSaleForEdit(currentSale.saleId);
                }
                
                // Remettre le focus sur le champ de recherche
                setTimeout(() => {
                  this.resetProductSelection();
                }, 200);
              } else {
                // Contexte: ajout produit → Vider le champ de recherche et refocus
                this.resetProductSelection();
              }
            }
          );
        } else {
          // Afficher le message d'erreur normal
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: errorMsg,
            life: 5000,
          });
        }
      }
    });
    
    // Effect pour détecter le succès après force stock
    effect(() => {
      const loading = this.loading();
      const previousLoading = this.previousLoadingState();
      const waiting = this.waitingForForceStockSuccess();
      const errorDetails = this.facade.errorDetails();
      
      // Ne rien faire si on n'attend pas de succès
      if (!waiting) {
        // Mettre à jour l'état précédent seulement si différent pour éviter re-déclenchements
        if (previousLoading !== loading) {
          this.previousLoadingState.set(loading);
        }
        return;
      }
      
      // Mettre à jour l'état précédent
      this.previousLoadingState.set(loading);
      
      // Si on attendait le succès ET loading vient de passer de true à false ET pas d'erreur
      if (previousLoading && !loading && !errorDetails) {
        this.waitingForForceStockSuccess.set(false);
        
        // Clear l'erreur maintenant que c'est réussi
        this.facade.clearError();
        
        // Comportement après succès: TOUJOURS reset complet + focus
        // (Que ce soit ajout produit OU modification cellule)
        const context = this.forceStockContext();
        this.forceStockContext.set(null); // Clear le contexte
        
        setTimeout(() => {
          this.resetProductSelection();
        }, 200);
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
    // Vérifier si une caisse est ouverte pour l'utilisateur connecté
    this.hasCashRegisterOpen();

    // Initialize customer display
    this.customerDisplay.initialize('PHARMA SMART');

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
   * ✅ MODIFIÉ Phase 2.2: Gère la sélection manuelle d'un produit depuis l'autocomplete
   * Focus automatique sur quantité après sélection
   */
  onProductSelected(product: ProduitSearch | null): void {
    if (!product) {
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
    this.customerDisplay.updateDisplayForProduct(product.libelle || '', quantity, product.regularUnitPrice || 0);

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
        // ✅ MODIFIÉ Phase 2.3: Clear selection et reset focus
        this.resetProductSelection();

        // Notifier le container que l'ajout est réussi
        this.productAddedSuccess.emit();
      } else {
        // En cas d'erreur, le produit reste sélectionné
        // L'utilisateur peut réessayer ou corriger
        console.error('Échec ajout produit:', currentError);
      }
    }, 200);
  }

  /**
   * ✅ AJOUT Phase 2.3: Réinitialiser la sélection produit et focus
   * Appelée après ajout réussi d'un produit
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

  /**
   * ✅ AJOUT Phase 4: Gestion Enter dans champ produit vide
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

    // Retour du focus sur le champ produit
    setTimeout(() => {
      this.productSearchComponent()?.getFocus();
    }, 100);
  }

  onLineQuantityRequestedChanged(data: { line: ISalesLine; newQty: number }): void {
    if (data.line.id) {
      this.facade.updateLineQuantityRequested(data.line.id, data.newQty);
    }
    
    // NOTE: Le focus sera géré après succès ou dans le dialog de force stock
    // Ne pas ramener le focus ici pour permettre au dialog de gérer correctement le contexte
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
      this.authorizationService.requestDeleteProductAuthorization(saleId, saleType).subscribe(authorized => {
        if (authorized && event.line.saleLineId) {
          this.facade.removeLine(event.line.saleLineId);
        }
      });
    } else if (event.action === 'discount') {
      this.authorizationService.requestDiscountAuthorization(saleId, saleType).subscribe(authorized => {
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

  onLinePriceChanged(data: { line: ISalesLine; newPrice: number }): void {
    if (data.line.id) {
      this.facade.updateLinePrice(data.line.id, data.newPrice);
    }

    // Retour du focus sur le champ produit
    setTimeout(() => {
      this.productSearchComponent()?.getFocus();
    }, 100);
  }

  // ===== Handlers pour remise globale (depuis ProductListComponent caption) =====

  onRemiseSelected(remise: any): void {
    const currentSale = this.currentSale();
    if (!currentSale) {
      this.notificationService.error('Aucune vente en cours');
      return;
    }

    // TODO: Implémenter l'application de remise globale
    // this.facade.applyGlobalDiscount(remise);
    console.log('Remise sélectionnée:', remise);
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

    this.authorizationService.requestDiscountAuthorization(saleId, saleType).subscribe(authorized => {
      if (authorized) {
        this.openRemiseSelectionModal();
      }
    });
  }

  private requestRemiseRemovalAuthorization(): void {
    const saleId = this.currentSale()?.id;
    const saleType = this.selectedSaleType();

    this.authorizationService.requestDiscountAuthorization(saleId, saleType).subscribe(authorized => {
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
      },
    );
  }

  // ===== Handlers pour SaleActionsComponent =====

  onSave(): void {
    const sale = this.facade.currentSale();
    if (!sale || !this.canSave()) {
      this.notificationService.warning('Vente invalide', "Veuillez ajouter au moins un produit avant d'enregistrer la vente");
      return;
    }

    // Récupérer les paiements depuis le composant payment-mode
    const paymentModeComp = this.paymentMode();
    if (!paymentModeComp) {
      this.notificationService.error('Erreur', 'Composant de paiement non disponible');
      return;
    }

    // Construire l'événement de paiement
    const paymentEvent: PaymentCompleteEvent = {
      payments: paymentModeComp.selectedModes().map(entry => ({
        mode: entry.mode,
        amount: entry.amount || 0,
        amountEntered: entry.amountEntered
      })),
      totalPaid: paymentModeComp.totalPaid(),
      change: paymentModeComp.changeAmount(),
      changeExact: paymentModeComp.changeExact(),
      comment: undefined,
      printReceipt: false,
      printInvoice: false,
    };

    // Utiliser la méthode commune de validation paiement
    this.processPaymentValidation(paymentEvent);
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
      this.notificationService.warning('Client requis', 'Un client est obligatoire pour un avoir');
      return;
    }

    // Sauvegarder la vente (utiliser saveSale, pas createComptantSale)
    this.facade.saveSale();

    // Clear customer display after sale
    this.customerDisplay.clear();

    // Réinitialiser pour nouvelle vente après sauvegarde
    setTimeout(() => {
      if (!this.facade.isSaving() && !this.facade.lastError()) {
        this.resetForNewSale();
      }
    }, 500);
  }

  /**
   * Appelé quand l'utilisateur valide le paiement depuis le composant payment-mode (Enter dans input)
   * Utilise la méthode commune de validation paiement
   */
  onPaymentComplete(event: PaymentCompleteEvent): void {
    this.processPaymentValidation(event);
  }

  /**
   * Méthode commune pour valider et traiter le paiement
   * Appelée par onSave() (bouton Enregistrer) ET onPaymentComplete() (Enter dans input)
   * Gère la vente différée si montant insuffisant
   */
  private processPaymentValidation(event: PaymentCompleteEvent): void {
    const currentSale = this.facade.currentSale();
    if (!currentSale) {
      this.notificationService.warning('Erreur', 'Aucune vente en cours');
      return;
    }

    console.log('🔍 processPaymentValidation - currentSale.differe:', currentSale.differe);
    console.log('🔍 processPaymentValidation - currentSale.customerId:', currentSale.customerId);
    console.log('🔍 processPaymentValidation - isDiffere():', this.isDiffere());

    // Appliquer les paiements et montants de l'événement à la vente
    currentSale.montantVerse = event.totalPaid || 0;
    currentSale.payments = this.convertPayments(event.payments);
    
    // Stocker les deux montants de monnaie (exact pour comptabilité, arrondi pour affichage)
    currentSale.montantRendu = event.changeExact || 0; // Montant exact
    currentSale.montantRenduArrondi = event.change || 0; // Montant arrondi

    // Gérer vente différée si montant insuffisant
    const amountToBePaid = currentSale.amountToBePaid || 0;
    const restToPay = amountToBePaid - currentSale.montantVerse;

    // Seuil de tolérance: on considère qu'il y a un reste seulement si > 5 FCFA
    console.log('🔍 Vérification vente différée - restToPay:', restToPay, 'differe:', currentSale.differe);
    if (restToPay > PAYMENT_TOLERANCE_THRESHOLD && !currentSale.differe) {
      console.log('✅ Entre dans le dialog de vente différée');
      // Montant insuffisant → Proposer vente différée
      const message = `
        <div>Le montant versé est inférieur au montant dû.</div><br>
        <div class="text-end mb-2">
          Montant dû : <span class="fs-5 badge rounded-pill bg-danger-subtle text-danger-emphasis"><b>${amountToBePaid.toLocaleString()} FCFA</b></span>
        </div>
        <div class="text-end mb-2">
          Montant versé : <span class="fs-5 badge rounded-pill bg-primary-subtle text-primary-emphasis"><b>${currentSale.montantVerse.toLocaleString()} FCFA</b></span>
        </div>
        <div class="text-end mb-2">
          Reste à payer : <span class="fs-5 badge rounded-pill bg-warning-subtle text-warning-emphasis"><b>${restToPay.toLocaleString()} FCFA</b></span>
        </div><br>
        <div>Voulez-vous régler le reste en différé ?</div>
      `;
      
      this.confirmDialog().onConfirm(
        () => {
          console.log('✅ User clique OUI sur dialog différé');
          // OUI → Finaliser en différé avec sélection client
          // Vérifier si un client est déjà associé
          if (!currentSale.customerId) {
            console.log('🔍 Pas de client, ouverture modal');
            // Pas de client → Ouvrir modal sélection client
            // On ne fait RIEN après la sélection, on laisse l'utilisateur saisir le commentaire et valider
            this.openCustomerModalForDiffere();
          } else {
            console.log('✅ Client déjà présent:', currentSale.customerId);
            // Client déjà présent, marquer directement en différé et finaliser
            currentSale.differe = true;
            this.isDiffere.set(true); // Mettre à jour le signal pour afficher le champ commentaire
            console.log('✅ Marqué comme différé - isDiffere():', this.isDiffere());
            // On ne finalise PAS automatiquement, on laisse l'utilisateur saisir le commentaire et valider
          }
        },
        'Vente différée',
        message,
        undefined,
        () => {
          // NON → Remettre focus sur input règlement pour ajuster
          setTimeout(() => {
            this.paymentMode()?.focusFirstMode();
          }, 100);
        }
      );
    } else {
      console.log('✅ Passe directement à finalizeSale (montant OK ou déjà différé)');
      // Montant suffisant (ou différence <= 5 FCFA) ou déjà différé → Finaliser
      this.finalizeSale();
    }
  }

  /**
   * Convertit les paiements de PaymentCompleteEvent au format attendu par le backend
   */
  private convertPayments(eventPayments: Array<{ mode: IPaymentMode; amount: number; amountEntered?: number }>): any[] {
    return eventPayments.map(p => ({
      paymentMode: p.mode,
      paidAmount: p.amount,
      montantVerse: p.amountEntered || p.amount,
      netAmount: p.amount,
    }));
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
          this.facade.cancelSale(); // Annuler la vente AVANT de reset l'UI
          setTimeout(() => this.resetForNewSale(), 100);
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
   * Ouvre le modal de sélection client pour les ventes différées
   */
  private openCustomerModalForDiffere(): void {
    const modalRef = this.modalService.open(CustomerSelectionModalComponent, {
      size: 'lg',
      backdrop: 'static',
      centered: true,
    });
    modalRef.componentInstance.modalTitle = 'SÉLECTION CLIENT - Vente différée';

    modalRef.result.then(
      (customer: ICustomer) => {
        console.log('🔍 Modal fermé - customer reçu du modal:', customer?.id);
        
        if (customer && customer.id) {
          console.log('✅ Client sélectionné depuis modal:', customer.id);
          
          // Synchroniser avec la facade
          this.facade.setCustomer(customer);
          
          // Assigner le customerId à la vente
          const currentSale = this.facade.currentSale();
          if (currentSale) {
            currentSale.customerId = customer.id;
            currentSale.differe = true; // S'assurer que differe est bien à true
            this.isDiffere.set(true); // Mettre à jour le signal pour forcer la détection de changement
            console.log('✅ Mise à jour: customerId =', currentSale.customerId, 'differe =', currentSale.differe, 'isDiffere() =', this.isDiffere());
            
            // Mettre focus sur le champ commentaire après sélection client
            setTimeout(() => {
              console.log('✅ Appel focusCommentInput()');
              this.paymentMode()?.focusCommentInput();
            }, 100);
          }
          
          // Ne PAS appeler finalizeSale() ici, laisser l'utilisateur saisir le commentaire et valider
        } else {
          this.notificationService.warning(
            'Client requis',
            'Un client est obligatoire pour une vente différée'
          );
        }
      },
      () => {
        // Modal fermé sans sélection
        this.notificationService.warning(
          'Vente annulée',
          'Un client est obligatoire pour une vente différée'
        );
      }
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

    // Reset product search component
    setTimeout(() => {
      this.productSearchComponent()?.reset();
      this.productSearchComponent()?.getFocus();
    }, 100);
  }

  /**
   * Vérifie si l'utilisateur a une caisse ouverte
   */
  private hasCashRegisterOpen(): void {
    this.cashRegisterService
      .getConnectedUserHasOpenCashRegister()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          this.isCashRegisterOpen.set(res.body ?? false);
        },
        error: () => {
          this.isCashRegisterOpen.set(false);
        },
      });
  }

  /**
   * Ouvre le modal de formulaire de caisse
   * Utilisé pour enregistrer le montant en caisse avant de finaliser la vente
   */
  private openCashRegister(): void {
    showCommonModal(this.modalService, CashRegisterFormComponent, {}, (resp: boolean) => {
      if (resp) {
        this.isCashRegisterOpen.set(resp);
        // Réessayer la finalisation après ouverture de la caisse
        this.finalizeSale();
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

    // Retour du focus sur le champ produit
    setTimeout(() => {
      this.productSearchComponent()?.getFocus();
    }, 100);
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
