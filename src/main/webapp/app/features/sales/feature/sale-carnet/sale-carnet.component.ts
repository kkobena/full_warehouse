import { Component, inject, OnInit, signal, computed, viewChild, output, effect, input, model } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MessageService } from 'primeng/api';
import { Toast } from 'primeng/toast';
import { Button } from 'primeng/button';
import { Card } from 'primeng/card';
import { Drawer } from 'primeng/drawer';
import { TooltipModule } from 'primeng/tooltip';
import { NgxSpinnerService } from 'ngx-spinner';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { ConfirmDialogComponent } from '../../../../shared/dialog/confirm-dialog/confirm-dialog.component';
import {
  ProductSearchComponent,
  ProductListComponent,
  SaleSummaryComponent,
  SaleActionsComponent,
} from '../../ui';
import { InsuranceDataBarComponent } from '../../ui/insurance-data-bar/insurance-data-bar.component';
import { PaymentModeComponent, PaymentCompleteEvent } from '../../ui/payment-mode/payment-mode.component';
import { CashRegisterFormComponent } from '../../../../entities/cash-register/user-cash-register/cash-register-form/cash-register-form.component';
import { CustomerCarnetComponent } from '../../../../entities/customer/carnet/customer-carnet.component';
import { UninsuredCustomerListComponent } from '../../../../entities/sales/uninsured-customer-list/uninsured-customer-list.component';
import { QuantiteProdutSaisieComponent } from '../../../../shared/quantite-produt-saisie/quantite-produt-saisie.component';
import { showCommonModal } from '../../../../entities/sales/selling-home/sale-helper';
import { SalesFacade } from '../../data-access/facades/sales.facade';
import { AuthorizationService } from '../../data-access/services/authorization.service';
import { NotificationService } from '../../../../shared/services/notification.service';
import { CustomerDisplayService } from '../../data-access/services/customer-display.service';
import { ISalesLine } from '../../../../shared/model/sales-line.model';
import { ProduitSearch } from '../../../../shared/model/produit.model';
import { IPaymentMode } from '../../../../shared/model/payment-mode.model';
import { IClientTiersPayant } from '../../../../shared/model/client-tiers-payant.model';
import { ICustomer } from '../../../../shared/model/customer.model';

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
  standalone: true,
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
  providers: [MessageService], // Instance locale pour ce composant
})
export class SaleCarnetComponent implements OnInit {
  // ViewChild
  insuranceDataBar = viewChild<InsuranceDataBarComponent>('insuranceDataBar');
  private confirmDialog = viewChild.required<ConfirmDialogComponent>('confirmDialog');
  
  // Output pour notifier le container du succès de l'ajout (règle métier: reset après succès)
  productAddedSuccess = output<void>();
  
  // Modal and responsive state
  readonly isCashRegisterOpen = model(false);
  readonly isSmallScreen = input(false);

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
  readonly paymentDrawerVisible = signal(false);
  readonly isProcessingSale = signal(false);
  
  // Helper method pour savoir si un client est sélectionné
  hasCustomer = computed(() => !!this.selectedCustomer());
  
  constructor() {
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
    
    // Initialize customer display
    this.customerDisplay.initialize('PHARMA SMART');
  }

  // ===== Handlers pour InsuranceDataBarComponent =====

  onCustomerSelected(customer: ICustomer): void {
    this.facade.setCustomer(customer);
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

  onEditAyantDroit(): void {
    // Pas d'ayant droit pour CARNET
  }

  onLoadAyantDroits(): void {
    // Pas d'ayant droit pour CARNET
  }

  onAddComplementaire(): void {
    this.notificationService.warning(
      'Non disponible',
      'Les complémentaires ne sont pas disponibles pour les ventes CARNET'
    );
  }

  onRemoveTiersPayant(tiersPayant: IClientTiersPayant): void {
    this.notificationService.warning(
      'Non disponible',
      'Impossible de supprimer le tiers payant principal pour une vente CARNET'
    );
  }

  onTiersPayantsChanged(tiersPayants: IClientTiersPayant[]): void {
    const currentSale = this.currentSale();
    if (currentSale) {
      currentSale.tiersPayants = tiersPayants;
    }
  }

  // ===== Product Management =====

  onProductSelected(product: ProduitSearch | null): void {
    if (!product) {
      return;
    }

    if (!this.hasCustomer()) {
      this.notificationService.warning(
        'Client requis',
        'Veuillez sélectionner un client avant d\'ajouter des produits'
      );
      return;
    }

    this.facade.setSelectedProduct(product);
  }

  onAddQuantity(quantity: number): void {
    const product = this.selectedProduct();
    if (!product || !quantity || quantity <= 0) {
      return;
    }

    if (!this.hasCustomer()) {
      this.notificationService.warning(
        'Client requis',
        'Veuillez sélectionner un client avant d\'ajouter des produits'
      );
      return;
    }

    this.facade.onAddProduit(product);
    
    // Update customer display
    this.customerDisplay.updateDisplayForProduct(
      product.libelle || '',
      quantity,
      product.regularUnitPrice || 0
    );
    
    // Notifier le container que l'ajout est réussi (pour reset du formulaire)
    this.productAddedSuccess.emit();
  }

  onUpdateQuantity(update: { line: ISalesLine; quantity: number }): void {
    if (update.line.id) {
      this.facade.updateLineQuantity(update.line.id, update.quantity);
    }
  }

  onRemoveLine(line: ISalesLine): void {
    if (line.id) {
      this.facade.removeSalesLine(line.id);
    }
  }

  onApplyDiscount(): void {
    this.notificationService.info('Remise', 'Fonctionnalité à implémenter');
  }

  // ===== Handlers pour ProductListComponent =====

  onLineQuantityChanged(data: { line: ISalesLine; newQty: number }): void {
    if (data.line.id) {
      this.facade.updateLineQuantity(data.line.id, data.newQty);
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
    // Handle line selection if needed
  }

  onLineDiscountChanged(data: { line: ISalesLine; newDiscount: number }): void {
    // Handle line discount if needed
  }

  onAuthorizationRequired(event: { line: ISalesLine; action: 'delete' | 'discount' }): void {
    // Handle authorization requirements if needed
  }

  // ===== Sale Actions =====

  onValidate(): void {
    // Show total on customer display
    const total = this.currentSale()?.salesAmount || 0;
    this.customerDisplay.updateDisplayForTotal(total);
    
    this.paymentDrawerVisible.set(true);
  }

  onPutOnHold(): void {
    this.facade.saveSale();
  }

  onCancel(): void {
    // Confirm before canceling
    if (this.salesLines().length > 0) {
      this.confirmDialog().onConfirm(
        () => this.resetForNewSale(),
        'Annulation de la vente',
        'Êtes-vous sûr de vouloir annuler cette vente ?'
      );
    } else {
      this.resetForNewSale();
    }
  }

  /**
   * Retour à l'écran précédent (quitter le point de vente)
   * Utilisé uniquement par le bouton Retour dans le header
   */
  onNavigateBack(): void {
    this.customerDisplay.clear();
    this.router.navigate(['/']);
  }

  /**
   * Réinitialiser pour une nouvelle vente
   * Appelé après sauvegarde ou annulation pour rester sur l'écran de vente
   */
  private resetForNewSale(): void {
    this.facade.cancelSale();
    this.customerDisplay.clear();
    this.paymentDrawerVisible.set(false);
    this.isProcessingSale.set(false);
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
   * Finalise la vente après l'ouverture de la caisse
   */
  private completeSaleAfterCashRegister(): void {
    this.isProcessingSale.set(true);

    this.facade.saveSale();

    // Réinitialiser pour nouvelle vente
    this.resetForNewSale();
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
        `Le montant versé (${entryAmount} FCFA) est inférieur au montant dû (${amountToBePaid} FCFA).\n\nVoulez-vous régler le reste (${restToPay} FCFA) en différé ?`
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
          this.paymentDrawerVisible.set(false);
        }
      },
      () => {
        this.notificationService.warning(
          'Vente annulée',
          'Un client est obligatoire pour une vente différée'
        );
        this.paymentDrawerVisible.set(false);
      }
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

  onPaymentCancel(): void {
    this.paymentDrawerVisible.set(false);
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
        'Voulez-vous vraiment supprimer la remise appliquée?'
      );
    } else {
      // Demander autorisation pour supprimer
      this.requestRemiseRemovalAuthorization();
    }
  }

  private requestRemiseAuthorization(): void {
    const saleId = this.currentSale()?.id;
    const saleType = 'CARNET';

    this.authorizationService.requestDiscountAuthorization(saleId, saleType)
      .subscribe(authorized => {
        if (authorized) {
          this.openRemiseSelectionModal();
        }
      });
  }

  private requestRemiseRemovalAuthorization(): void {
    const saleId = this.currentSale()?.id;
    const saleType = 'CARNET';

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
      (updatedCustomer) => {
        if (updatedCustomer && updatedCustomer.id) {
          this.facade.setCustomer(updatedCustomer);
        }
      },
      () => {
        // Modal fermée sans enregistrement
      }
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
      (customer) => {
        if (customer && customer.id) {
          this.facade.setCustomer(customer);
          
          // TODO: Vérifier solde crédit et plafond
        } else {
          this.notificationService.warning(
            'Client requis',
            'Un client avec compte crédit est obligatoire pour une vente CARNET'
          );
          this.router.navigate(['/']);
        }
      },
      () => {
        // Modal fermée sans sélection - retour à l'accueil
        this.notificationService.warning(
          'Vente annulée',
          'Un client avec compte crédit est obligatoire pour une vente CARNET'
        );
        this.router.navigate(['/']);
      }
    );
  }
}
