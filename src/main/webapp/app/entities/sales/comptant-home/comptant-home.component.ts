import { isPlatformBrowser } from '@angular/common';
import {
  AfterViewInit,
  Component,
  DestroyRef,
  effect,
  HostListener,
  inject,
  OnDestroy,
  OnInit,
  PLATFORM_ID,
  signal,
  viewChild,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { PanelModule } from 'primeng/panel';
import { TooltipModule } from 'primeng/tooltip';
import { IUser } from '../../../core/user/user.model';
import { ICustomer } from '../../../shared/model/customer.model';
import { ProduitSearch } from '../../../shared/model/produit.model';
import { GroupRemise } from '../../../shared/model/remise.model';
import { FinalyseSale, InputToFocus, ISales, SaleId, SaveResponse, StockError } from '../../../shared/model/sales.model';
import { ISalesLine, SalesLine } from '../../../shared/model/sales-line.model';
import { PRODUIT_COMBO_MIN_LENGTH, PRODUIT_COMBO_RESULT_SIZE } from '../../../shared/constants/pagination.constants';
import { Observable } from 'rxjs';
import { SalesService } from '../sales.service';
import { CustomerService } from '../../customer/customer.service';
import { ProduitService } from '../../produit/produit.service';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { AccountService } from '../../../core/auth/account.service';
import { ErrorService } from '../../../shared/error.service';
import { TranslateService } from '@ngx-translate/core';
import { HttpResponse } from '@angular/common/http';
import { CardModule } from 'primeng/card';
import { ComptantComponent } from '../selling-home/comptant/comptant.component';
import { CustomerOverlayPanelComponent } from '../customer-overlay-panel/customer-overlay-panel.component';
import { SelectedCustomerService } from '../service/selected-customer.service';
import { CurrentSaleService } from '../service/current-sale.service';
import { LastCurrencyGivenService } from '../service/last-currency-given.service';
import { InputGroupModule } from 'primeng/inputgroup';
import { SalesStatut } from '../../../shared/model/enumerations/sales-statut.model';
import { UserCaissierService } from '../service/user-caissier.service';
import { UserVendeurService } from '../service/user-vendeur.service';
import { SaleEvent } from '../service/sale-event-manager.service';
import { HasAuthorityService } from '../service/has-authority.service';
import { BaseSaleService } from '../service/base-sale.service';
import { Authority } from '../../../shared/constants/authority.constants';
import { RemiseCacheService } from '../service/remise-cache.service';
import { IMagasin } from '../../../shared/model/magasin.model';
import { Select } from 'primeng/select';
import { InputGroupAddonModule } from 'primeng/inputgroupaddon';
import { DrawerModule } from 'primeng/drawer';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { ConfirmDialogComponent } from '../../../shared/dialog/confirm-dialog/confirm-dialog.component';
import { UninsuredCustomerListComponent } from '../uninsured-customer-list/uninsured-customer-list.component';
import { UninsuredCustomerFormComponent } from '../../customer/uninsured-customer-form/uninsured-customer-form.component';
import { ToastAlertComponent } from '../../../shared/toast-alert/toast-alert.component';
import { QuantiteProdutSaisieComponent } from '../../../shared/quantite-produt-saisie/quantite-produt-saisie.component';
import { isEditMode, isVno, showCommonError, showCommonModal, translateSalesLabel } from '../selling-home/sale-helper';
import { SaleEventSignal } from '../selling-home/sale-event';
import { handleSaleEvents } from '../selling-home/sale-event-helper';
import { DeconditionnementService } from '../validator/deconditionnement.service';
import { ForceStockService } from '../validator/force-stock.service';
import { SaleStockValidator } from '../validator/sale-stock-validator.service';
import { ProduitSearchAutocompleteScannerComponent } from '../../../shared/produit-search-autocomplete-scanner/produit-search-autocomplete-scanner.component';
import { SellingHomeShortcutsService } from '../selling-home/racourci/selling-home-shortcuts.service';
import { KeyboardShortcutsService } from '../selling-home/racourci/keyboard-shortcuts.service';
import { TauriPrinterService } from '../../../shared/services/tauri-printer.service';
import { MagasinService } from '../../magasin/magasin.service';
import { PreventeModalComponent } from '../prevente-modal/prevente-modal/prevente-modal.component';

@Component({
  selector: 'jhi-comptant-home',
  imports: [
    WarehouseCommonModule,
    PreventeModalComponent,
    RouterModule,
    InputTextModule,
    ButtonModule,
    FormsModule,
    PanelModule,
    TooltipModule,
    CardModule,
    ComptantComponent,
    CustomerOverlayPanelComponent,
    InputGroupModule,
    Select,
    InputGroupAddonModule,
    DrawerModule,
    ConfirmDialogComponent,
    ToastAlertComponent,
    QuantiteProdutSaisieComponent,
    ProduitSearchAutocompleteScannerComponent,
    IconField,
    InputIcon,
  ],
  templateUrl: './comptant-home.component.html',
  styleUrl: './comptant-home.component.scss',
})
export class ComptantHomeComponent implements OnInit, AfterViewInit, OnDestroy {
  readonly minLength = PRODUIT_COMBO_MIN_LENGTH;
  readonly COMPTANT = 'COMPTANT';
  comptantComponent = viewChild<ComptantComponent>('comptant');
  userBox = viewChild<any>('userBox');
  accountService = inject(AccountService);
  currentAccount = this.accountService.trackCurrentAccount();
  remiseCacheService = inject(RemiseCacheService);
  remises: GroupRemise[] = this.remiseCacheService.remises();
  protected selectedDepot?: IMagasin | null;
  protected canFocusLastModeInput = false;
  protected canForceStock: boolean;
  protected check = true;
  protected userCaissier?: IUser | null;
  protected userSeller?: IUser;
  protected produitSelected?: ProduitSearch | null = null;
  protected appendTo = 'body';
  protected stockSeverity = 'success';
  protected pendingSalesSidebar = false;
  protected isSaving = false;
  protected isPresale = false;
  protected showStock = true;
  protected printTicket = true;
  protected searchCustomer: string = null;
  protected readonly currentSaleService = inject(CurrentSaleService);
  protected readonly userVendeurService = inject(UserVendeurService);
  protected readonly PRODUIT_COMBO_RESULT_SIZE = PRODUIT_COMBO_RESULT_SIZE;
  private readonly userCaissierService = inject(UserCaissierService);
  private readonly hasAuthorityService = inject(HasAuthorityService);
  private readonly baseSaleService = inject(BaseSaleService);
   readonly selectedCustomerService = inject(SelectedCustomerService);
  private readonly lastCurrencyGivenService = inject(LastCurrencyGivenService);
  private readonly salesService = inject(SalesService);
  private readonly customerService = inject(CustomerService);
  private readonly produitService = inject(ProduitService);
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly modalService = inject(NgbModal);
  private readonly errorService = inject(ErrorService);
  private readonly translate = inject(TranslateService);
  private quantityMessage = '';
  private magasin: IMagasin | null = null;
  private readonly confimDialog = viewChild.required<ConfirmDialogComponent>('confirmDialog');
  private readonly alert = viewChild.required<ToastAlertComponent>('alert');
  private readonly quantyBox = viewChild.required<QuantiteProdutSaisieComponent>('produitQteCmpt');
  private readonly produitbox = viewChild.required<ProduitSearchAutocompleteScannerComponent>('produitbox');
  private readonly saleEventManager = inject(SaleEventSignal);
  private readonly destroyRef = inject(DestroyRef);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly saleStockValidator = inject(SaleStockValidator);
  private readonly deconditionnementService = inject(DeconditionnementService);
  private readonly forceStockService = inject(ForceStockService);
  private readonly shortcutsService = inject(SellingHomeShortcutsService);
  private readonly keyboardService = inject(KeyboardShortcutsService);
  private readonly tauriPrinterService = inject(TauriPrinterService);
  private readonly magasinService = inject(MagasinService);

  constructor() {
    this.canForceStock = this.hasAuthorityService.hasAuthorities(Authority.PR_FORCE_STOCK);

    this.magasinService.findCurrentUserMagasin().then(magasin => {
      this.magasin = magasin;
    });
    this.initCustomerEffect();
    this.quantityMessage = this.translateLabel('stockInsuffisant');
    handleSaleEvents(this.saleEventManager, ['saveResponse', 'completeSale', 'responseEvent', 'inputBoxFocus'], event => {
      switch (event.name) {
        case 'saveResponse':
          this.handleSaveResponse(event);
          break;
        case 'completeSale':
          this.handleCompleteSale(event);
          break;
        case 'responseEvent':
          this.handleResponseEvent(event);
          break;
        case 'inputBoxFocus':
          this.handleInputBoxFocus(event);
          break;
      }
    });
  }

  protected get disableButton(): boolean {
    return this.produitSelected == null || this.quantyBox().value < 1;
  }

  onRemoveCustomer(): void {
    this.salesService.removeCustommerToCashSale(this.currentSaleService.currentSale().saleId).subscribe(() => {
      this.currentSaleService.currentSale().customerId = null;
    });
  }

  onAddCustommer(): void {
    this.salesService
      .addCustommerToCashSale({
        id: this.currentSaleService.currentSale().saleId,
        value: this.selectedCustomerService.selectedCustomerSignal().id,
      })
      .subscribe(() => {
        this.currentSaleService.currentSale().customerId = this.selectedCustomerService.selectedCustomerSignal().id;
      });
  }

  onLoadPrevente(sales: ISales, toEdit = false): void {
    if (!toEdit && sales.statut !== SalesStatut.CLOSED) {
      this.router.navigate(['/sales', false, 'new']);
    } else {
      this.currentSaleService.setCurrentSale(sales);
      this.currentSaleService.setIsEdit(sales.statut === SalesStatut.CLOSED);
      this.userSeller = this.userVendeurService.vendeurs().find(e => e.id === sales.sellerId) || this.userSeller;
      this.userVendeurService.setVendeur(this.userSeller);
      this.loadPrevente();
    }
  }

  ngOnInit(): void {
    this.currentSaleService.setCurrentSale(null);
    this.selectedCustomerService.setCustomer(null);
    this.userCaissier = { ...this.currentAccount() } as IUser;
    this.userCaissierService.setCaissier(this.userCaissier);

    // Initialize customer display (Tauri only)
    this.initializeCustomerDisplay();

    // Register keyboard shortcuts
    this.registerKeyboardShortcuts();

    this.activatedRoute.data.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(({ sales, mode }) => {
      if (sales.id) {
        if (sales.customer) {
          this.customerService
            .find(sales.customer.id)
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({ next: (resp: HttpResponse<ICustomer>) => this.selectedCustomerService.setCustomer(resp.body) });
        }
        this.onLoadPrevente(sales, isEditMode(mode));
      }
    });
    this.activatedRoute.paramMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(params => {
      if (params.has('isPresale')) {
        this.isPresale = params.get('isPresale') === 'true';
      }
    });
  }

  ngAfterViewInit(): void {
    if (this.userBox()) {
      if (!this.userSeller) {
        this.userSeller = this.userCaissier;
      }
    }
    this.userVendeurService.setVendeur(this.userSeller);
  }

  @HostListener('document:keydown', ['$event'])
  handleKeyboardEvent(event: KeyboardEvent): void {
    this.keyboardService.handleKeyboardEvent(event);
  }

  manageAmountDiv(): void {
    // Show total on customer display when opening payment dialog
    const currentSale = this.currentSaleService.currentSale();
    if (currentSale?.amountToBePaid) {
      this.updateDisplayForTotal(currentSale.amountToBePaid);
    }

    this.comptantComponent().manageAmountDiv();
  }

  previousState(): void {
    this.clearCustomerDisplay();
    this.resetAll();
    window.history.back();
  }

  save(): void {
    if (this.currentSaleService.currentSale()) {
      this.comptantComponent().save();
    }
  }

  totalItemQty(): number {
    if (this.produitSelected) {
      return this.currentSaleService.currentSale()?.salesLines.find(e => e.produitId === this.produitSelected.id)?.quantityRequested || 0;
    }
    return 0;
  }

  onAddNewQty(qytMvt: number): void {
    const qtyMaxToSel = this.baseSaleService.quantityMax();
    const qtyAlreadyRequested = this.totalItemQty() + qytMvt;
    const validation = this.saleStockValidator.validate(this.produitSelected, qytMvt, qtyAlreadyRequested, this.canForceStock, qtyMaxToSel);
    if (validation.isValid) {
      this.onAddProduit(qytMvt);
    } else {
      this.handleInvalidStock(validation.reason, qytMvt);
    }
  }

  onAddProduit(qytMvt: number): void {
    if (this.produitSelected) {
      if (this.currentSaleService.currentSale()) {
        this.comptantComponent().onAddProduit(this.createSalesLine(this.produitSelected, qytMvt));
      } else {
        this.comptantComponent().createComptant(this.createSalesLine(this.produitSelected, qytMvt));
      }
    }
  }

  print(sale: ISales | null): void {
    if (sale !== null && sale !== undefined) {
      this.comptantComponent().print(sale);
      this.currentSaleService.setCurrentSale(null);
      this.selectedCustomerService.setCustomer(null);
    }
  }

  printSale(saleId: SaleId): void {
    this.comptantComponent().printSale(saleId);
  }

  showError(message: string): void {
    this.alert().showError(message);
  }

  openInfoDialog(message: string): void {
    showCommonError(this.modalService, message);
  }

  resetAll(): void {
    this.currentSaleService.reset();
    this.selectedCustomerService.setCustomer(null);
    this.selectedDepot = null;
    this.userSeller = this.userCaissier;
    this.userVendeurService.setVendeur(this.userCaissier);
    this.check = true;
    const lastCurrency = this.lastCurrencyGivenService.givenCurrency();
    this.lastCurrencyGivenService.setLastCurrency(lastCurrency);
    this.lastCurrencyGivenService.resetGivenCurrency();
    this.goToNew();
    this.updateProduitQtyBox();

    // Clear and reset customer display to welcome message
    this.clearCustomerDisplay();
    this.showWelcomeMessage();
  }

  openPindingSide(): void {
    this.pendingSalesSidebar = true;
  }

  closeSideBar(booleanValue: boolean): void {
    this.pendingSalesSidebar = booleanValue;
    if (this.currentSaleService.currentSale()) {
      this.onLoadPrevente(this.currentSaleService.currentSale(), true);
    }
  }

  onSave(saveResponse: SaveResponse): void {
    if (saveResponse.success) {
      const selectedProduit = this.produitSelected;
      this.updateDisplayForProduct(selectedProduit.libelle, this.quantyBox().value, selectedProduit.regularUnitPrice);
      this.updateProduitQtyBox();
    } else {
      if (saveResponse.error.error?.errorKey === 'stock' || saveResponse.error.error?.errorKey === 'stockChInsufisant') {
        this.onStockError(saveResponse.payload as ISalesLine, saveResponse.error);
      } else {
        this.onCommonError(saveResponse);
      }
    }
  }

  onFinalyse(finalyseSale: FinalyseSale): void {
    if (finalyseSale.success) {
      if (!finalyseSale.putOnStandBy) {
        if (this.printTicket) {
          this.printSale(finalyseSale.saleId);
        }
        if (this.currentSaleService.printInvoice()) {
          this.onPrintInvoice();
        }
      }

      const change = this.lastCurrencyGivenService.givenCurrency();
      if (change && change > 0) {
        this.updateDisplayForChange(change);
      }

      // Reset to welcome message after a delay
      setTimeout(() => {
        this.resetAll();
      }, 300);
    } else {
      this.onCommonError(finalyseSale);
    }
  }

  getControlToFocus(inputToFocusEvent: InputToFocus): void {
    if (inputToFocusEvent.control === 'produitBox') {
      this.produitbox().getFocus();
      if (this.quantyBox()) {
        this.quantyBox().reset(1);
      }
    }
  }

  onCustomerOverlay(evnt: boolean): void {
    this.produitbox().getFocus();
  }

  removeCustomer(): void {
    this.selectedCustomerService.setCustomer(null);
    this.clearCustomerSearch();
    this.produitbox().getFocus();
  }

  loadUninsuredCustomers(): void {
    if (!this.searchCustomer) {
      return;
    }

    this.customerService
      .queryUninsuredCustomers({
        search: this.searchCustomer,
      })
      .subscribe({
        next: (res: HttpResponse<ICustomer[]>) => this.handleUninsuredCustomerQueryResponse(res.body),
      });
  }

  openUninsuredCustomerListModal(): void {
    showCommonModal(
      this.modalService,
      UninsuredCustomerListComponent,
      {
        header: 'CLIENTS NON ASSURES',
      },
      (resp: ICustomer) => {
        if (resp) {
          this.handleUninsuredCustomerSelection(resp);
        }
      },
      '60%',
    );
  }

  ngOnDestroy(): void {
    this.shortcutsService.unregisterAll();
    // Clear customer display when leaving the sales page
    this.clearCustomerDisplay();
  }

  protected addQuantity(qte: number): void {
    this.onAddNewQty(qte);
  }

  protected onSelectUser(): void {
    this.produitbox().getFocus();
  }

  protected onSelectProduct(selectedProduit?: ProduitSearch): void {
    this.produitSelected = selectedProduit || null;
    this.quantyBox().reset(1);
    this.quantyBox().focusProduitControl();
    if (this.produitSelected.totalQuantity > 0) {
      this.stockSeverity = 'success';
    } else {
      this.stockSeverity = 'danger';
    }
  }

  protected onSaveKeyDown(saveSale: boolean): void {
    if (saveSale && this.currentSaleService.currentSale().salesLines.length > 0) {
      this.manageAmountDiv();
    }
  }

  protected onBarcodeScanned(barcode: string): void {
    console.log('Barcode scanned:', barcode);
  }

  private handleInvalidStock(reason: string, qytMvt: number): void {
    switch (reason) {
      case 'forceStockAndQuantityExceedsMax':
        this.forceStockService.handleForceStock(
          qytMvt,
          this.translateLabel('quantityGreatherMaxCanContinue'),
          this.confimDialog(),
          this.onAddProduit.bind(this),
          this.updateProduitQtyBox.bind(this),
        );
        break;
      case 'deconditionnement':
        this.deconditionnementService.handleDeconditionnement(
          qytMvt,
          this.produitSelected,
          this.confimDialog(),
          null,
          this.onAddProduit.bind(this),
          this.processQtyRequested.bind(this),
          this.updateProduitQtyBox.bind(this),
        );
        break;
      case 'forceStock':
        this.forceStockService.handleForceStock(
          qytMvt,
          this.translateLabel('quantityGreatherThanStock'),
          this.confimDialog(),
          this.onAddProduit.bind(this),
          this.updateProduitQtyBox.bind(this),
        );
        break;
      case 'stockInsuffisant':
        this.showError(this.quantityMessage);
        break;
      case 'quantityExceedsMax':
        this.showError(this.translateLabel('quantityGreatherMax'));
        break;
    }
  }

  private handleResponseEvent = (response: SaleEvent<unknown>): void => {
    if (response.content instanceof FinalyseSale) {
      this.onFinalyse(response.content);
    }
  };

  private handleSaveResponse = (response: SaleEvent<unknown>): void => {
    const content = response.content;
    if (content instanceof SaveResponse) {
      this.onSave(content);
    } else if (content instanceof StockError) {
      this.onStockOutError(content);
    }
  };

  private handleInputBoxFocus = (response: SaleEvent<unknown>): void => {
    if (response.content instanceof InputToFocus) {
      this.getControlToFocus(response.content);
    }
  };

  private handleCompleteSale = (response: SaleEvent<unknown>): void => {
    // Cash sales only - no special handling needed
  };

  private initCustomerEffect(): void {
    if (isPlatformBrowser(this.platformId)) {
      // Effect for handling customer changes in cash sales (VNO)
      effect(() => {
        const customer = this.selectedCustomerService.selectedCustomerSignal();
        const sale = this.currentSaleService.currentSale();

        if (!sale || !isVno(sale.type)) {
          return;
        }

        if (customer && !sale.customerId) {
          this.onAddCustommer();
        } else if (!customer && sale.customerId) {
          this.onRemoveCustomer();
        }
      });
    }
  }

  private updateProduitQtyBox(): void {
    if (this.quantyBox()) {
      this.quantyBox().reset(1);
    }
    if (this.check) {
      this.produitbox().getFocus();
    }

    this.produitSelected = null;
  }

  private processQtyRequested(salesLine: ISalesLine): void {
    this.salesService.updateItemQtyRequested(salesLine).subscribe({
      next: () => {
        if (this.currentSaleService.currentSale()) {
          this.subscribeToSaveResponse(this.salesService.find(this.currentSaleService.currentSale().saleId));
        }
        this.check = true;
      },
      error: error => {
        this.check = false;
        this.onStockError(salesLine, error);
      },
    });
  }

  private subscribeToSaveResponse(result: Observable<HttpResponse<ISales>>): void {
    result.subscribe({
      next: (res: HttpResponse<ISales>) => this.onSaveSuccess(res.body),
      error: () => this.onSaveError(),
    });
  }

  private onSaveSuccess(sale: ISales | null): void {
    this.isSaving = false;
    if (sale) {
      this.currentSaleService.setCurrentSale(sale);
    }

    this.updateProduitQtyBox();
  }

  private onSaveError(): void {
    this.isSaving = false;
    const message = 'Une erreur est survenue';
    this.openInfoDialog(message);
  }

  private onStockError(salesLine: ISalesLine, error: any): void {
    if (error.error) {
      if (error.error.errorKey === 'stock') {
        if (this.canForceStock) {
          salesLine.forceStock = true;
          this.forceStockService.onUpdateConfirmForceStock(
            salesLine,
            this.translateLabel('quantityGreatherThanStock'),
            this.confimDialog(),
            this.processQtyRequested.bind(this),
            this.updateProduitQtyBox.bind(this),
          );
        } else {
          this.openInfoDialog(this.errorService.getErrorMessage(error));
          this.subscribeToSaveResponse(this.salesService.find(this.currentSaleService.currentSale().saleId));
        }
      } else if (error.error.errorKey === 'stockChInsufisant') {
        this.produitService.find(Number(error.error.payload)).subscribe(res => {
          const prod = res.body;
          if (prod && prod.totalQuantity > 0) {
            this.deconditionnementService.handleDeconditionnement(
              salesLine.quantityRequested,
              prod,
              this.confimDialog(),
              salesLine,
              this.onAddProduit.bind(this),
              this.processQtyRequested.bind(this),
              this.updateProduitQtyBox.bind(this),
            );
          } else {
            this.openInfoDialog(this.translateLabel('stockInsuffisant'));
          }
        });
      }
    }
  }

  private onStockOutError(stockError: StockError): void {
    const salesLine = stockError.saleLine;
    this.onStockError(salesLine, stockError.err);
  }

  private onCommonError(error: any): void {
    if (error.error.status === 412) {
      this.updateProduitQtyBox();
      if (!this.currentSaleService.plafondIsReached()) {
        this.currentSaleService.setPlafondIsReached(true);
        this.openInfoDialog(this.errorService.getErrorMessage(error.error));
      }
    } else {
      this.openInfoDialog(this.errorService.getErrorMessage(error.error));
    }
  }

  private loadPrevente(): void {
    queueMicrotask(() => {
      this.comptantComponent().onLoadPrevente();
    });
  }

  private createSalesLine(produit: ProduitSearch, quantityRequested: number): ISalesLine {
    const quantitySold = Math.min(produit.totalQuantity, quantityRequested);
    return {
      ...new SalesLine(),
      produitId: produit.id,
      regularUnitPrice: produit.regularUnitPrice,
      saleId: this.currentSaleService.currentSale()?.id,
      quantitySold: quantitySold > 0 ? quantitySold : 0,
      quantityRequested,
      sales: this.currentSaleService.currentSale(),
    };
  }

  private onPrintInvoice(): void {
    this.comptantComponent().printInvoice();
  }

  private translateLabel(label: string): string {
    return translateSalesLabel(this.translate, label);
  }

  private goToNew() {
    this.router.navigate(['/sales/comptant', 'false', 'new']);
  }

  private initializeCustomerDisplay(): void {
    // TauriPrinterService handles environment detection and configuration
    // Show welcome message on initialization
    this.showWelcomeMessage();

    // Show connected user
    if (this.userCaissier) {
      this.updateDisplayForUser(this.userCaissier);
    }
  }

  private showWelcomeMessage(): void {
    const storeName = this.magasin?.name || 'PHARMA SMART';
    this.tauriPrinterService.showWelcomeMessage(storeName).catch(error => {
      console.error('Failed to show welcome message on customer display:', error);
    });
  }

  private updateDisplayForUser(user: IUser): void {
    const userName = user.firstName || user.login || 'Caissier';
    this.tauriPrinterService.updateDisplayForUser(userName).catch(error => {
      console.error('Failed to update customer display for user:', error);
    });
  }

  private updateDisplayForProduct(productName: string, qty: number, price: number): void {
    this.tauriPrinterService.updateDisplayForProduct(productName, qty, price).catch(error => {
      console.error('Failed to update customer display for product:', error);
    });
  }

  private updateDisplayForTotal(total: number): void {
    this.tauriPrinterService.updateDisplayForTotal(total).catch(error => {
      console.error('Failed to update customer display for total:', error);
    });
  }

  private updateDisplayForChange(change: number): void {
    this.tauriPrinterService.updateDisplayForChange(change).catch(error => {
      console.error('Failed to update customer display for change:', error);
    });
  }

  private clearCustomerDisplay(): void {
    this.tauriPrinterService.clearCustomerDisplay().catch(error => {
      console.error('Failed to clear customer display:', error);
    });
  }

  private handleUninsuredCustomerQueryResponse(customers: ICustomer[] | null): void {
    if (!customers?.length) {
      this.handleNoUninsuredCustomersFound();
      return;
    }

    if (customers.length === 1) {
      this.handleSingleUninsuredCustomerFound(customers[0]);
    } else {
      this.handleMultipleUninsuredCustomersFound();
    }
  }

  private handleSingleUninsuredCustomerFound(customer: ICustomer): void {
    this.handleUninsuredCustomerSelection(customer);
    this.clearCustomerSearch();
  }

  private handleMultipleUninsuredCustomersFound(): void {
    this.openUninsuredCustomerListModal();
    this.clearCustomerSearch();
  }

  private handleNoUninsuredCustomersFound(): void {
    this.addUninsuredCustomer();
    this.clearCustomerSearch();
  }

  private handleUninsuredCustomerSelection(customer: ICustomer): void {
    this.selectedCustomerService.setCustomer(customer);
    this.clearCustomerSearch();
    this.produitbox().getFocus();
  }

  private clearCustomerSearch(): void {
    this.searchCustomer = null;
  }

  private addUninsuredCustomer(): void {
    showCommonModal(
      this.modalService,
      UninsuredCustomerFormComponent,
      { title: "FORMULAIRE D'AJOUT DE NOUVEAU DE CLIENT", entity: null },
      (resp: ICustomer) => {
        if (resp) {
          this.handleUninsuredCustomerSelection(resp);
        }
      },
    );
  }

  private registerKeyboardShortcuts(): void {
    this.shortcutsService.registerAll({
      // Navigation
      focusProductSearch: () => this.produitbox()?.getFocus(),
      focusQuantity: () => this.quantyBox()?.focusProduitControl(),
      focusCustomer: () => {},
      focusVendor: () => this.userBox()?.nativeElement?.focus(),

      // Product actions
      addProduct: () => {},
      removeSelectedLine: () => {},
      clearProduct: () => {
        this.produitSelected = null;
        this.produitbox()?.getFocus();
      },
      viewProductStock: () => {
        if (this.produitSelected) {
          console.log('View stock for product:', this.produitSelected);
        }
      },

      // Sale types - all redirect to comptant (this component)
      switchToComptant: () => {},
      switchToAssurance: () => {},
      switchToCarnet: () => {},
      switchToDepotAgree: () => {},

      // Payment & Finalization
      finalizeSale: () => this.manageAmountDiv(),
      savePending: () => this.comptantComponent()?.putCurrentSaleOnStandBy(),
      viewPendingSales: () => this.openPindingSide(),
      cancelSale: () => this.resetAll(),

      // Quantity
      incrementQuantity: (amount: number) => {
        this.quantyBox()?.incrementQuantity(amount);
      },
      decrementQuantity: (amount: number) => {
        this.quantyBox()?.decrementQuantity(amount);
      },

      // Discounts
      applyDiscount: () => {},
      removeDiscount: () => {},

      // Printing
      printInvoice: () => this.onPrintInvoice(),
      printReceipt: () => {
        this.comptantComponent()?.printSale(this.currentSaleService.currentSale()?.saleId);
      },

      // Tauri-specific
      forceStock: this.canForceStock ? () => {} : undefined,
    });
  }
}
