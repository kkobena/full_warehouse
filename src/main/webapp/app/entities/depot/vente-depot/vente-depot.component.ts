import {
  AfterViewInit,
  Component,
  DestroyRef,
  HostListener,
  inject,
  OnDestroy,
  OnInit,
  PLATFORM_ID,
  viewChild
} from '@angular/core';
import { MagasinService } from '../../magasin/magasin.service';
import { DepotService } from '../depot.service';
import { Button } from 'primeng/button';
import { ComptantComponent } from '../../sales/selling-home/comptant/comptant.component';
import { ConfirmDialogComponent } from '../../../shared/dialog/confirm-dialog/confirm-dialog.component';
import { DecimalPipe } from '@angular/common';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import {
  ProduitSearchAutocompleteScannerComponent
} from '../../../shared/produit-search-autocomplete-scanner/produit-search-autocomplete-scanner.component';
import { QuantiteProdutSaisieComponent } from '../../../shared/quantite-produt-saisie/quantite-produt-saisie.component';
import { Select } from 'primeng/select';
import { ToastAlertComponent } from '../../../shared/toast-alert/toast-alert.component';
import { Tooltip } from 'primeng/tooltip';
import { AccountService } from '../../../core/auth/account.service';
import { RemiseCacheService } from '../../sales/service/remise-cache.service';
import { GroupRemise, IRemise } from '../../../shared/model/remise.model';
import { IMagasin } from '../../../shared/model/magasin.model';
import { IUser } from '../../../core/user/user.model';
import { ProduitSearch } from '../../../shared/model/produit.model';
import { UserVendeurService } from '../../sales/service/user-vendeur.service';
import { HasAuthorityService } from '../../sales/service/has-authority.service';
import { BaseSaleService } from '../../sales/service/base-sale.service';
import { ProduitService } from '../../produit/produit.service';
import { ActivatedRoute, Router } from '@angular/router';
import { ErrorService } from '../../../shared/error.service';
import { TranslateService } from '@ngx-translate/core';
import { SaleEventSignal } from '../../sales/selling-home/sale-event';
import { SaleStockValidator } from '../../sales/validator/sale-stock-validator.service';
import { DeconditionnementService } from '../../sales/validator/deconditionnement.service';
import { ForceStockService } from '../../sales/validator/force-stock.service';
import { SellingHomeShortcutsService } from '../../sales/selling-home/racourci/selling-home-shortcuts.service';
import { KeyboardShortcutsService } from '../../sales/selling-home/racourci/keyboard-shortcuts.service';
import { Authority } from '../../../shared/constants/authority.constants';
import { handleSaleEvents } from '../../sales/selling-home/sale-event-helper';
import {
  FinalyseSale,
  InputToFocus,
  ISales,
  SaleId,
  SaveResponse,
  StockError
} from '../../../shared/model/sales.model';
import { SalesStatut } from '../../../shared/model/enumerations/sales-statut.model';
import { showCommonError, translateSalesLabel } from '../../sales/selling-home/sale-helper';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpResponse } from '@angular/common/http';
import { ISalesLine, SalesLine } from '../../../shared/model/sales-line.model';
import { SaleEvent } from '../../sales/service/sale-event-manager.service';
import { Observable } from 'rxjs';
import { PRODUIT_COMBO_RESULT_SIZE } from '../../../shared/constants/pagination.constants';
import { DepotFacadeService } from './depot-facade.service';
import { TauriPrinterService } from '../../../shared/services/tauri-printer.service';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'jhi-vente-depot',
  imports: [
    Button,
    ConfirmDialogComponent,
    DecimalPipe,
    ProduitSearchAutocompleteScannerComponent,
    QuantiteProdutSaisieComponent,
    Select,
    ToastAlertComponent,
    Tooltip,
    FormsModule
  ],
  templateUrl: './vente-depot.component.html',
  styleUrl: './vente-depot.component.scss'
})
export class VenteDepotComponent implements OnInit, AfterViewInit, OnDestroy {
  private readonly magasinService = inject(MagasinService);
  protected readonly depotFacadeService = inject(DepotFacadeService);
  protected readonly userVendeurService = inject(UserVendeurService);
  protected readonly depotService = inject(DepotService);
  protected readonly baseSaleService = inject(BaseSaleService);
  private readonly tauriPrinterService = inject(TauriPrinterService);
  comptantComponent = viewChild(ComptantComponent);

  userBox = viewChild<any>('userBox');
  depotBox = viewChild<any>('depotBox');
  accountService = inject(AccountService);
  currentAccount = this.accountService.trackCurrentAccount();
  remiseCacheService = inject(RemiseCacheService);
  remises: GroupRemise[] = this.remiseCacheService.remises();
  protected selectedDepot?: IMagasin | null;

  protected isLargeScreen = true;
  protected canForceStock: boolean;


  protected userCaissier?: IUser | null;
  protected userSeller?: IUser;
  protected produitSelected?: ProduitSearch | null = null;
  protected appendTo = 'body';
  protected remise: IRemise[] = [];
  protected base64 = ';base64,';
  protected event: any;
  protected stockSeverity = 'success';
  protected isSaving = false;
  protected showStock = true;
  protected readonly PRODUIT_COMBO_RESULT_SIZE = PRODUIT_COMBO_RESULT_SIZE;
  private readonly hasAuthorityService = inject(HasAuthorityService);
  private readonly produitService = inject(ProduitService);
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly modalService = inject(NgbModal);
  private readonly errorService = inject(ErrorService);
  private readonly translate = inject(TranslateService);
  private quantityMessage = '';
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

  constructor() {
    this.canForceStock = this.hasAuthorityService.hasAuthorities(Authority.PR_FORCE_STOCK);
    this.quantityMessage = this.translateLabel('stockInsuffisant');
    handleSaleEvents(this.saleEventManager, ['saveResponse', 'completeSale', 'responseEvent', 'inputBoxFocus'], event => {
      switch (event.name) {
        case 'saveResponse':
          this.handleSaveResponse(event);

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


  onLoadPrevente(sales: ISales): void {
    if (sales.statut === SalesStatut.CLOSED) {

      this.router.navigate(['/depot', 'new-vente']);
    } else {
      this.depotService.setCurrentSale(sales);
      this.userSeller = this.userVendeurService.vendeurs().find(e => e.id === sales.sellerId) || this.userSeller;
      this.depotService.setVendeur(this.userSeller);
      this.loadPrevente();
    }
  }

  ngOnInit(): void {
    const width = window.innerWidth;
    if (width < 1800) {
      this.isLargeScreen = false;
    }
    this.depotService.setCurrentSale(null);
    this.depotService.setSelectedDepot(null);
    this.userCaissier = { ...this.currentAccount() } as IUser;
    this.depotService.setCaissier(this.userCaissier);

    // Register keyboard shortcuts
    this.registerKeyboardShortcuts();

    this.activatedRoute.data.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(({ sales }) => {
      if (sales.id) {
        this.magasinService
          .find(sales.magasin.id)
          .pipe(takeUntilDestroyed(this.destroyRef))
          .subscribe({ next: (resp: HttpResponse<IMagasin>) => this.depotService.setSelectedDepot(resp.body) });

        this.onLoadPrevente(sales);
      }
    });

  }

  ngAfterViewInit(): void {
    if (this.userBox()) {
      if (!this.userSeller) {
        this.userSeller = this.userCaissier;
      }
    }
    this.depotService.setVendeur(this.userSeller);
  }

  @HostListener('document:keydown', ['$event'])
  handleKeyboardEvent(event: KeyboardEvent): void {
    this.keyboardService.handleKeyboardEvent(event);
  }

  manageAmountDiv(): void {

  }

  previousState(): void {
    this.resetAll();
    window.history.back();
  }

  save(): void {
    if (this.depotService.currentSale()) {
      this.depotFacadeService.finalizeSale();
    }
  }


  totalItemQty(): number {
    if (this.produitSelected) {
      return this.depotService.currentSale()?.salesLines.find(e => e.produitId === this.produitSelected.id)?.quantityRequested || 0;
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
      if (this.depotService.currentSale()) {
        this.depotFacadeService.addItemToSale(this.createSalesLine(this.produitSelected, qytMvt));
      } else {
        this.depotFacadeService.create(this.createSalesLine(this.produitSelected, qytMvt));
      }

    }
  }

  print(sale: ISales | null): void {
    if (sale !== null && sale !== undefined) {
      this.depotFacadeService.printInvoice(sale.saleId);
    }
  }

  printSale(saleId: SaleId): void {
    if (this.tauriPrinterService.isRunningInTauri()) {
      this.depotFacadeService.printReceiptForTauri(saleId);
    } else {
      this.depotFacadeService.printReceipt(saleId);
    }
  }

  showError(message: string): void {
    this.alert().showError(message);
  }

  openInfoDialog(message: string): void {
    showCommonError(this.modalService, message);
  }

  resetAll(): void {
    this.depotService.reset();
    this.selectedDepot = null;
    this.userSeller = this.userCaissier;
    this.depotService.setVendeur(this.userCaissier);
    this.goToNew();
    this.updateProduitQtyBox();
  }


  onSave(saveResponse: SaveResponse): void {
    if (saveResponse.success) {
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
      if (this.depotService.canReceipt()) {
        this.printSale(finalyseSale.saleId);
      }
      if (this.depotService.canInvoice()) {
        this.onPrintInvoice();
      }

      this.resetAll();
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


  protected addQuantity(qte: number): void {
    this.onAddNewQty(qte);
  }

  protected onSelectUser(): void {
    this.produitbox().getFocus();
  }

  protected onSelectDepot(): void {
    this.depotService.setSelectedDepot(this.selectedDepot);
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
    if (saveSale && this.depotService.currentSale().salesLines.length > 0) {
      this.save();
    }
  }

  protected onBarcodeScanned(barcode: string): void {
    // Optional: Log or handle barcode scan event
    console.log('Barcode scanned:', barcode);
    // The product will be automatically selected by the autocomplete component
    // No additional action needed here unless you want to add custom logic
  }

  private handleInvalidStock(reason: string, qytMvt: number): void {
    switch (reason) {
      case 'forceStockAndQuantityExceedsMax':
        this.forceStockService.handleForceStock(
          qytMvt,
          this.translateLabel('quantityGreatherMaxCanContinue'),
          this.confimDialog(),
          this.onAddProduit.bind(this),
          this.updateProduitQtyBox.bind(this)
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
          this.updateProduitQtyBox.bind(this)
        );
        break;
      case 'forceStock':
        this.forceStockService.handleForceStock(
          qytMvt,
          this.translateLabel('quantityGreatherThanStock'),
          this.confimDialog(),
          this.onAddProduit.bind(this),
          this.updateProduitQtyBox.bind(this)
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


  private updateProduitQtyBox(): void {
    if (this.quantyBox()) {
      this.quantyBox().reset(1);
    }
    this.produitbox().getFocus();
    this.produitSelected = null;
  }

  private processQtyRequested(salesLine: ISalesLine): void {
    this.processQtyRequestedDepot(salesLine);
  }


  private subscribeToSaveResponse(result: Observable<HttpResponse<ISales>>): void {
    result.subscribe({
      next: (res: HttpResponse<ISales>) => this.onSaveSuccess(res.body),
      error: () => this.onSaveError()
    });
  }

  private onSaveSuccess(sale: ISales | null): void {
    this.isSaving = false;
    if (sale) {
      this.depotService.setCurrentSale(sale);
    }

    this.updateProduitQtyBox();
  }

  private processQtyRequestedDepot(salesLine: ISalesLine): void {
    this.depotService.updateItemQtyRequested(salesLine).subscribe({
      next: () => {
        if (this.depotService.currentSale()) {
          this.subscribeToSaveResponse(this.depotService.find(this.depotService.currentSale().saleId));
        }

      },
      error: error => {

        this.onStockError(salesLine, error);
      }
    });
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
            this.updateProduitQtyBox.bind(this)
          );
        } else {
          this.openInfoDialog(this.errorService.getErrorMessage(error));
          this.subscribeToSaveResponse(this.depotService.find(this.depotService.currentSale().saleId));

        }
      } else if (error.error.errorKey === 'stockChInsufisant') {
        this.produitService.find(Number(error.error.payload)).subscribe(res => {
          const prod = res.body;
          if (prod && prod.totalQuantity > 0) {
            // si quantite CH
            this.deconditionnementService.handleDeconditionnement(
              salesLine.quantityRequested,
              prod,
              this.confimDialog(),
              salesLine,
              this.onAddProduit.bind(this),
              this.processQtyRequested.bind(this),
              this.updateProduitQtyBox.bind(this)
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

    } else {
      this.openInfoDialog(this.errorService.getErrorMessage(error.error));
    }
  }


  private loadPrevente(): void {
    queueMicrotask(() => {

      this.produitbox().getFocus();
    });
  }

  private createSalesLine(produit: ProduitSearch, quantityRequested: number): ISalesLine {
    const quantitySold = Math.min(produit.totalQuantity, quantityRequested);
    const currentSale = this.depotService.currentSale();
    return {
      ...new SalesLine(),
      produitId: produit.id,
      regularUnitPrice: produit.regularUnitPrice,
      saleId: currentSale.id,
      quantitySold: quantitySold > 0 ? quantitySold : 0,
      quantityRequested,
      sales: currentSale
    };
  }

  private onPrintInvoice(): void {
    const currentSale = this.depotService.currentSale();
    if (currentSale) {
      this.depotFacadeService.printInvoice(currentSale.saleId);
    }
  }

  private translateLabel(label: string): string {
    return translateSalesLabel(this.translate, label);
  }

  private goToNew() {
    this.router.navigate(['/depot', 'new-vente']);
  }

  ngOnDestroy(): void {
    this.shortcutsService.unregisterAll();
  }

  private registerKeyboardShortcuts(): void {
    this.shortcutsService.registerAll({
      // Navigation
      focusProductSearch: () => this.produitbox()?.getFocus(),
      focusQuantity: () => this.quantyBox()?.focusProduitControl(),
      focusCustomer: () => {

      },
      focusVendor: () => this.userBox()?.nativeElement?.focus(),

      // Product actions
      //addProduct: () => this.onAddProduit(),
      addProduct: () => {
      },
      removeSelectedLine: () => {

      },
      clearProduct: () => {
        this.produitSelected = null;
        this.produitbox()?.getFocus();
      },
      viewProductStock: () => {
        if (this.produitSelected) {
          // Stock modal would be triggered here if available
          console.log('View stock for product:', this.produitSelected);
        }
      },

      // Sale types
      switchToComptant: () => {

      },
      switchToAssurance: () => {

      },
      switchToCarnet: () => {

      },
      switchToDepotAgree: () => {

      },

      // Payment & Finalization
      finalizeSale: () => this.manageAmountDiv(),
      savePending: () => {

      },
      viewPendingSales: () => {
      },
      cancelSale: () => this.resetAll(),

      // Quantity
      incrementQuantity: (amount: number) => {
        this.quantyBox()?.incrementQuantity(amount);
      },
      decrementQuantity: (amount: number) => {
        this.quantyBox()?.decrementQuantity(amount);
      },

      // Discounts
      applyDiscount: () => {

      },
      removeDiscount: () => {

      },

      // Printing
      printInvoice: () => this.onPrintInvoice(),
      printReceipt: () => this.printSale(this.depotService.currentSale().saleId),

      // Tauri-specific (optional, only if user has permission)
      forceStock: this.canForceStock ? () => {
        console.log('Force stock activated');
        // Force stock logic would go here
      } : undefined
    });
  }


}
