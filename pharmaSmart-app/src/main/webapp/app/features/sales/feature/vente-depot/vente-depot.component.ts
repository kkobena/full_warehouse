import {Component, computed, DestroyRef, effect, inject, OnInit, signal, viewChild, ChangeDetectionStrategy} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {take} from 'rxjs/operators';

import {NgbTooltip} from '@ng-bootstrap/ng-bootstrap';
import {ButtonComponent, SelectSearchComponent} from '../../../../shared/ui';
import {NgxSpinnerComponent, NgxSpinnerService} from 'ngx-spinner';

import {VenteDepotFacade} from '../../data-access/facades/vente-depot.facade';
import {RemiseCacheService} from '../../data-access/services/remise-cache.service';
import {AuthorizationService} from '../../data-access/services/authorization.service';
import {CustomerDisplayService} from '../../data-access/services/customer-display.service';
import {
  ProductListComponent,
  ProductSearchSectionComponent,
  SaleActionsComponent,
  SaleSummaryComponent
} from '../../ui';
import {
  createDeconditionnementHandling,
  createForceStockHandling,
  createKeyboardShortcuts,
  createProductHandling,
  ProductSearchHost,
} from '../../shared/mixins';

import {AccountService} from '../../../../core/auth/account.service';
import {UserVendeurService} from '../../../../entities/sales/service/user-vendeur.service';
import {TauriPrinterService} from '../../../../shared/services/tauri-printer.service';
import {NotificationService} from '../../../../shared/services/notification.service';

import {NgbConfirmDialogService} from '../../../../shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive';

import {FinalyseSale, SaleId} from '../../../../shared/model/sales.model';
import {IMagasin, IRemise, ISalesLine, ProduitSearch} from '../../../../shared/model';
import {SaleLineId} from '../../../../shared/model/sales-line.model';
import {IUser} from '../../../../core/user/user.model';
import {showCommonError} from '../../../../entities/sales/selling-home/sale-helper';
import {NgbModal} from '@ng-bootstrap/ng-bootstrap';
import {ErrorService} from '../../../../shared/error.service';
@Component({
  selector: 'app-vente-depot',
  host: {
    '(window:keydown)': 'handleKeyboardEvent($event)',
  },
  imports: [
    CommonModule,
    FormsModule,
    ButtonComponent,
    SelectSearchComponent,
    NgbTooltip,
    ProductListComponent,
    ProductSearchSectionComponent,
    SaleActionsComponent,
    SaleSummaryComponent,
    NgxSpinnerComponent,
  ],
  templateUrl: './vente-depot.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './vente-depot.component.scss',
})
export class VenteDepotComponent implements OnInit, ProductSearchHost {
  // ── Confirm Dialog ──────────────────────────────────────────
  private readonly confirmDialog = inject(NgbConfirmDialogService);


  productSearchComponent = viewChild<ProductSearchSectionComponent>('produitbox');

  readonly quantityComponent = computed(() => {
    const section = this.productSearchComponent();
    if (!section) return undefined;
    return {
      focusProduitControl: () => section.focusProduitControl(),
      reset: (qty: number) => section.resetQuantity(qty),
    };
  });

  // ── Services ─────────────────────────────────────────────────
  protected readonly facade = inject(VenteDepotFacade);
  protected readonly userVendeurService = inject(UserVendeurService);
  protected readonly remiseCacheService = inject(RemiseCacheService);
  private readonly authorizationService = inject(AuthorizationService);
  private readonly accountService = inject(AccountService);
  private readonly tauriPrinterService = inject(TauriPrinterService);
  private readonly notificationService = inject(NotificationService);
  private readonly customerDisplay = inject(CustomerDisplayService);
  private readonly modalService = inject(NgbModal);
  private readonly errorService = inject(ErrorService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly spinner = inject(NgxSpinnerService);

  // ── Autorisations ─────────────────────────────────────────────
  readonly canApplyDiscount = signal<boolean>(false);
  readonly canModifyPrice = signal<boolean>(false);

  // ── État local ────────────────────────────────────────────────
  protected selectedDepot: IMagasin | null = null;
  protected userCaissier: IUser | null = null;
  protected userSeller: IUser | undefined;

  // Force Stock state signals (requis par les mixins)
  readonly waitingForForceStockSuccess = signal<boolean>(false);
  readonly forceStockContext = signal<'addProduct' | 'editCell' | null>(null);

  // ── Mixins ──────────────────────────────────────────────────

  private productHandling = createProductHandling({
    facade: this.facade as any,
    customerDisplay: this.customerDisplay,
    notificationService: this.notificationService,
    host: this,
    config: {
      requiresCustomer: false,
      saleType: 'COMPTANT',
    },
    selectedProduct: this.facade.selectedProduct,
    currentSale: this.facade.currentSale,
    createSale: (line: ISalesLine) => this.facade.create(line),
    addProduct: (line: ISalesLine) => this.facade.addOrIncrementProduct(line),
  });

  private forceStockHandling = createForceStockHandling({
    facade: this.facade as any,
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
      createSale: (line: ISalesLine) => this.facade.create(line),
      addProduct: (line: ISalesLine) => this.facade.addOrIncrementProduct(line),
    },
  });

  private deconditionnementHandling = createDeconditionnementHandling({
    facade: this.facade as any,
    waitingForForceStockSuccess: this.waitingForForceStockSuccess,
    getConfirmDialog: () => this.confirmDialog,
    resetProductSelection: () => this.productHandling.resetProductSelection(),
    operations: {
      createSale: (line: ISalesLine) => this.facade.create(line),
      addProduct: (line: ISalesLine) => this.facade.addOrIncrementProduct(line),
    },
  });

  private keyboardShortcuts = createKeyboardShortcuts(
    {saleType: 'COMPTANT'},
    {
      focusProductSearch: () => this.productHandling.focusProductSearch(),
      focusQuantity: () => this.productSearchComponent()?.focusProduitControl(),
      focusCustomer: () => {
      },
      addProduct: () => {
        const product = this.facade.selectedProduct();
        if (product) {
          this.addQuantity(1);
        }
      },
      clearProduct: () => this.productHandling.resetProductSelection(),
      finalizeSale: () => this.save(),
      putOnStandby: () => {
      },
      cancelSale: () => this.onCancel(),
      printReceipt: () => {
        const sale = this.facade.currentSale();
        if (sale?.saleId) this.printSale(sale.saleId);
      },
      printInvoice: () => {
        const sale = this.facade.currentSale();
        if (sale?.saleId) this.facade.printInvoice(sale.saleId);
      },
    },
  );

  constructor() {
    this.canApplyDiscount.set(this.authorizationService.canApplyDiscount());
    this.canModifyPrice.set(this.authorizationService.canModifyPrice());

    // Initialiser les effects des mixins (ordre important : force-stock avant déconditionnement)
    this.forceStockHandling.initializeEffects();
    this.deconditionnementHandling.initializeEffects();

    // Effect spinner
    effect(() => {
      this.facade.loading() ? this.spinner.show('sale-spinner') : this.spinner.hide('sale-spinner');
    });
  }

  // ── Lifecycle ─────────────────────────────────────────────────

  ngOnInit(): void {
    this.facade.resetForNewSession();
    this.facade.init();

    // Initialise le caissier
    const account = this.accountService.trackCurrentAccount();
    this.userCaissier = {...account()} as IUser;
    this.facade.setCashier(this.userCaissier);
    this.userSeller = this.userCaissier ?? undefined;
    this.facade.setSeller(this.userCaissier ?? null);

    // Souscription au succès d'ajout de produit (reset sélection + affichage client)
    this.facade.productAddedSuccess$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      this.productHandling.updatePendingDisplay();
      this.productHandling.resetProductSelection();
    });

    // Souscription aux erreurs non gérées par les mixins (erreurs non-stock)
    this.facade.productOpResult$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(res => {
      if (!res.success) {
        const errorKey = res.error?.error?.errorKey;
        // Les erreurs stock/stockChInsufisant sont gérées par les mixins
        if (errorKey !== 'stock' && errorKey !== 'stockChInsufisant') {
          this.onCommonError(res);
        }
      }
    });

    // Souscription à la finalisation
    this.facade.saleFinalized$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(res => this.onFinalyse(res));
  }

  // ── Raccourcis clavier ────────────────────────────────────────

  handleKeyboardEvent(event: KeyboardEvent): void {
    this.keyboardShortcuts.handleKeyboardEvent(event);
  }

  // ── Actions publiques ─────────────────────────────────────────

  protected save(): void {
    if (this.facade.currentSale()) {
      this.facade.finalizeSale();
    }
  }

  protected previousState(): void {
    this.facade.resetForNewSession();
    this.selectedDepot = null;
    window.history.back();
  }

  protected onCancel(): void {
    if (this.facade.currentSale()) {
      this.confirmDialog.onConfirm(
        () => {
          this.facade.cancelSale();
        },
        'Annulation de la vente',
        'Voulez-vous vraiment annuler cette vente ? Toutes les données seront perdues.',
      );
    } else {
      this.facade.resetForNewSession();
    }
  }

  // ── Handlers ProductSearchSectionComponent ────────────────────

  protected onProductSelected(product: ProduitSearch | null): void {
    if (!this.facade.selectedDepot()) {
      this.notificationService.warning('Veuillez sélectionner un dépôt', 'Dépôt requis');
      this.focusDepotBox();
      return;
    }
    this.productHandling.onProductSelected(product);
  }

  protected onProductScanned(product: ProduitSearch, codeScan?: string): void {
    if (!this.facade.selectedDepot()) {
      this.notificationService.warning('Veuillez sélectionner un dépôt', 'Dépôt requis');
      this.focusDepotBox();
      return;
    }
    this.productHandling.onProductScanned(product, codeScan);
  }

  protected onSaveKeyDown(shouldSave: boolean): void {

    if (shouldSave && (this.facade.currentSale()?.salesLines?.length ?? 0) > 0) {
      this.confirmDialog.onConfirm(
        () => {
          this.save();
        },
        'Finaliser la vente',
        'Voulez-vous vraiment finaliser la vente ?', null,
        () => this.productSearchComponent()?.getFocus()
      );

    }
  }

  protected addQuantity(qte: number): void {

    if (!this.facade.selectedDepot()) {
      this.notificationService.warning('Veuillez sélectionner un dépôt', 'Dépôt requis');
      this.focusDepotBox();
      return;
    }
    this.productHandling.onAddQuantity(qte);
  }

  // ── Handlers toolbar ──────────────────────────────────────────

  protected onSelectUser(): void {
    this.facade.setSeller(this.userSeller ?? null);
    this.productSearchComponent()?.getFocus();
  }

  protected onSelectDepot(): void {
    if (this.facade.currentSale()) {
      this.confirmDialog.onConfirm(
        () => {
          this.facade
            .changeDepot(this.facade.currentSale()!.saleId, this.selectedDepot!.id)
            .pipe(take(1))
            .subscribe({
              next: () => {
                this.facade.setSelectedDepot(this.selectedDepot);
                this.productSearchComponent()?.getFocus();
              },
              error: error => this.onCommonError(error),
            });
        },
        'Modification Dépôt',
        'Voulez-vous vraiment changer le dépôt de la vente en cours ?',
        null,
        () => {
          this.facade.setSelectedDepot(this.selectedDepot);
          this.productSearchComponent()?.getFocus();
        },
      );
    } else {
      this.facade.setSelectedDepot(this.selectedDepot);
      this.productSearchComponent()?.getFocus();
    }
  }

  // ── Handlers ProductListComponent ─────────────────────────────

  protected onQuantitySoldChanged(event: { line: ISalesLine; newQty: number }): void {
    if (event.line.id) {
      this.facade.updateLineQuantitySold(event.line.id, event.newQty);
    }
  }

  protected onQuantityRequestedChanged(event: { line: ISalesLine; newQty: number }): void {
    if (event.line.id) {
      this.facade.updateLineQuantityRequested(event.line.id, event.newQty);
    }
  }

  protected onPriceChanged(event: { line: ISalesLine; newPrice: number }): void {
    if (event.line.id) {
      this.facade.updateLinePrice(event.line.id, event.newPrice);
    }
  }


  protected onAuthorizationRequired(event: { line: ISalesLine; action: 'delete' | 'discount' }): void {
    const saleId = this.facade.currentSale()?.id;
    if (event.action === 'delete') {
      this.facade.removeItemFromSale(event.line.saleLineId as SaleLineId);
      /*  this.authorizationService
          .requestDeleteProductAuthorization(saleId)
          .pipe(take(1))
          .subscribe(authorized => {
            if (authorized) {
              this.facade.removeItemFromSale(event.line.saleLineId as SaleLineId);
            }
          });*/
    } else if (event.action === 'discount') {
      this.authorizationService
        .requestDiscountAuthorization(saleId)
        .pipe(take(1))
        .subscribe(authorized => {
          if (authorized) {
            this.productSearchComponent()?.getFocus();
          }
        });
    }
  }

  protected onRemiseSelected(remise: IRemise): void {
    if (this.canApplyDiscount()) {
      this.facade.updateRemise(remise);
    } else {
      this.authorizationService
        .requestDiscountAuthorization(this.facade.currentSale()?.id)
        .pipe(take(1))
        .subscribe(authorized => {
          if (authorized) {
            this.facade.updateRemise(remise);
          }
        });
    }
  }

  protected onRemoveRemise(): void {
    if (!this.facade.currentSale()?.remise) return;

    const doRemove = () => {
      this.confirmDialog.onConfirm(
        () => this.facade.updateRemise(undefined),
        'Supprimer la remise',
        'Voulez-vous vraiment supprimer la remise appliquée ?',
        undefined,
        () => this.productSearchComponent()?.getFocus(),
      );
    };

    if (this.canApplyDiscount()) {
      doRemove();
    } else {
      this.authorizationService
        .requestDiscountAuthorization(this.facade.currentSale()?.id)
        .pipe(take(1))
        .subscribe(authorized => {
          if (authorized) doRemove();
        });
    }
  }

  // ── Handlers de résultats ───────────────────────────────────

  private onFinalyse(finalyseSale: FinalyseSale): void {
    if (finalyseSale.success) {
      const saleId = finalyseSale.saleId!;
      if (this.facade.canReceipt()) {
        this.printSale(saleId);
      }
      if (this.facade.canInvoice()) {
        const sale = this.facade.currentSale();
        if (sale) {
          this.facade.printInvoice(sale.saleId);
        }
      }
      this.resetAll();
    } else {
      this.onCommonError(finalyseSale);
    }
  }

  private resetAll(): void {
    this.facade.resetForNewSession();
    this.selectedDepot = null;
    this.userSeller = this.userCaissier ?? undefined;
    this.facade.setSeller(this.userCaissier ?? null);
    this.productHandling.resetProductSelection();
  }

  // ── Privé ─────────────────────────────────────────────────────

  private printSale(saleId: SaleId): void {
    if (this.tauriPrinterService.isRunningInTauri()) {
      this.facade.printReceiptForTauri(saleId);
    } else {
      this.facade.printReceipt(saleId);
    }
  }

  private focusDepotBox(): void {
    setTimeout(() => {
      const depotSelect = document.querySelector('.depot-select .ng-select') as HTMLElement;
      depotSelect?.focus();
    }, 50);
  }

  private onCommonError(error: any): void {
    const status = error?.error?.status ?? error?.status;
    if (status === 412) {
      this.productHandling.resetProductSelection();
    } else {
      showCommonError(this.modalService, this.errorService.getErrorMessage(error?.error ?? error));
    }
  }
}
