import { AfterViewInit, Component, DestroyRef, HostListener, inject, OnDestroy, OnInit, signal, viewChild } from '@angular/core';
import { MagasinService } from '../../magasin/magasin.service';
import { DepotService } from '../depot.service';
import { Button } from 'primeng/button';
import { ConfirmDialogComponent } from '../../../shared/dialog/confirm-dialog/confirm-dialog.component';
import { CommonModule } from '@angular/common';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { ProduitSearchAutocompleteScannerComponent } from '../../../shared/produit-search-autocomplete-scanner/produit-search-autocomplete-scanner.component';
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
import { FinalyseSale, InputToFocus, ISales, SaleId, SaveResponse, StockError } from '../../../shared/model/sales.model';
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
import { FormActionAutorisationComponent } from '../../sales/form-action-autorisation/form-action-autorisation.component';
import { take } from 'rxjs/operators';
import { SpinnerComponent } from '../../../shared/spinner/spinner.component';
import { VenteDepotTableComponent } from '../vente-depot-table/vente-depot-table.component';

@Component({
  selector: 'jhi-vente-depot',
  imports: [
    CommonModule,
    Button,
    ConfirmDialogComponent,
    ProduitSearchAutocompleteScannerComponent,
    QuantiteProdutSaisieComponent,
    Select,
    ToastAlertComponent,
    Tooltip,
    FormsModule,
    SpinnerComponent,
    VenteDepotTableComponent,
  ],
  templateUrl: './vente-depot.component.html',
  styleUrl: './vente-depot.component.scss',
})
export class VenteDepotComponent implements OnInit, AfterViewInit, OnDestroy {
  userBox = viewChild<any>('userBox');
  depotBox = viewChild<any>('depotBox');
  accountService = inject(AccountService);
  currentAccount = this.accountService.trackCurrentAccount();
  remiseCacheService = inject(RemiseCacheService);
  remises: GroupRemise[] = this.remiseCacheService.remises();
  // readonly canRemoveItem = signal(this.hasAuthorityService.hasAuthorities(Authority.PR_SUPPRIME_PRODUIT_VENTE));
  readonly canRemoveItem = signal<boolean>(true);
  readonly canApplyDiscount = signal<boolean>(false);
  readonly canModifyPrice = signal<boolean>(false);
  readonly canForceStock = signal<boolean>(false);

  protected readonly depotFacadeService = inject(DepotFacadeService);
  protected readonly userVendeurService = inject(UserVendeurService);
  protected readonly depotService = inject(DepotService);
  protected selectedDepot?: IMagasin | null;
  protected isLargeScreen = true;
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
  private readonly magasinService = inject(MagasinService);
  private readonly tauriPrinterService = inject(TauriPrinterService);
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
  private readonly saleStockValidator = inject(SaleStockValidator);
  private readonly deconditionnementService = inject(DeconditionnementService);
  private readonly forceStockService = inject(ForceStockService);
  private readonly shortcutsService = inject(SellingHomeShortcutsService);
  private readonly keyboardService = inject(KeyboardShortcutsService);
  private readonly spinner = viewChild.required<SpinnerComponent>('spinner');
  constructor() {
    this.quantityMessage = this.translateLabel('stockInsuffisant');
    this.canApplyDiscount.set(this.hasAuthorityService.hasAuthorities(Authority.PR_AJOUTER_REMISE_VENTE));
    this.canModifyPrice.set(this.hasAuthorityService.hasAuthorities(Authority.PR_MODIFIER_PRIX));
    this.canForceStock.set(this.hasAuthorityService.hasAuthorities(Authority.PR_FORCE_STOCK));

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
      console.log(event, 'handled in VenteDepotComponent');
    });

    this.depotFacadeService.saveResponse$.pipe(takeUntilDestroyed()).subscribe(res => {
      this.onSave(res);
    });

    this.depotFacadeService.spinnerService$.pipe(takeUntilDestroyed()).subscribe(res => {
      if (res) {
        this.spinner().show();
      } else {
        this.spinner().hide();
      }
    });

    this.depotFacadeService.finalyseSale$.pipe(takeUntilDestroyed()).subscribe(res => {
      this.onFinalyse(res);
    });
  }

  protected get disableButton(): boolean {
    return this.produitSelected == null || this.quantyBox().value < 1;
  }

  protected get entryAmount(): number {
    return 0;
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
      if (sales && sales.id) {
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

  manageAmountDiv(): void {}

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
    const qtyMaxToSel = this.depotService.quantityMax();
    const qtyAlreadyRequested = this.totalItemQty() + qytMvt;
    const validation = this.saleStockValidator.validate(
      this.produitSelected,
      qytMvt,
      qtyAlreadyRequested,
      this.canForceStock(),
      qtyMaxToSel,
    );

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

  ngOnDestroy(): void {
    this.shortcutsService.unregisterAll();
  }



  removeLine(salesLine: ISalesLine): void {
    this.depotFacadeService.removeItemFromSale(salesLine.saleLineId);
  }

  confirmDeleteItem(item: ISalesLine): void {
    if (item) {
      this.removeLine(item);
    } else {
      this.produitbox().getFocus();
    }
  }

  updateItemQtyRequested(salesLine: ISalesLine): void {
    if (salesLine) {
      this.depotFacadeService.updateItemQtyRequested(salesLine);
    }
  }

  updateItemQtySold(salesLine: ISalesLine): void {
    this.depotFacadeService.updateItemQtySold(salesLine);
  }

  updateItemPrice(salesLine: ISalesLine): void {
    this.depotFacadeService.updateItemPrice(salesLine);
  }

  onAddRmiseOpenActionAutorisationDialog(remise: IRemise): void {
    const modalRef = this.modalService.open(FormActionAutorisationComponent, {
      backdrop: 'static',
      centered: true,
    });
    modalRef.componentInstance.entity = this.depotService.currentSale();
    modalRef.componentInstance.privilege = Authority.PR_AJOUTER_REMISE_VENTE;
    modalRef.closed.pipe(take(1)).subscribe(reason => {
      if (reason === true) {
        this.addRemise(remise);
      }
    });
  }

  onAddRemise(remise: IRemise): void {
    if (this.canApplyDiscount()) {
      this.addRemise(remise);
    } else {
      if (remise) {
        this.onAddRmiseOpenActionAutorisationDialog(remise);
      } else {
        if (this.depotService.currentSale().remise) {
          this.onAddRmiseOpenActionAutorisationDialog(remise);
        }
      }
    }
  }

  addRemise(remise: IRemise): void {
    this.depotFacadeService.updateRemise(remise);
  }

  protected addQuantity(qte: number): void {
    if (this.selectedDepot) {
      this.onAddNewQty(qte);
    } else {
      this.showError('Veuillez sélectionner un dépôt');
      this.focusDepotBox();
    }
  }
  protected focusDepotBox(): void {
    setTimeout(() => {
      const el = this.depotBox().inputEL?.nativeElement;
      el.focus();
      el.select();
    }, 50);
  }
  protected onSelectUser(): void {
    this.produitbox().getFocus();
  }

  protected onSelectDepot(): void {
    if (this.depotService.currentSale()) {
      this.confimDialog().onConfirm(
        () => {
          this.depotService.changeDepot(this.depotService.currentSale().saleId, this.selectedDepot!.id!).subscribe({
            next: () => {
              this.depotService.setSelectedDepot(this.selectedDepot);
              this.produitbox().getFocus();
            },
            error: error => {
              this.onCommonError(error);
            },
          });
        },
        'Modification Dépôt',
        'Voulez-vous vraiment changer le dépôt de la vente en cours ?',
        null,
        () => {
          this.depotService.setSelectedDepot(this.selectedDepot);
          this.produitbox().getFocus();
        },
      );
    } else {
      this.depotService.setSelectedDepot(this.selectedDepot);
      this.produitbox().getFocus();
    }
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
protected onSaveAction(onSave: boolean): void {
  this.onSaveKeyDown(onSave);
}
  protected onBarcodeScanned(barcode: string): void {

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
      error: () => this.onSaveError(),
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
      },
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
        if (this.canForceStock()) {
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
      saleId: currentSale?.id,
      quantitySold: quantitySold > 0 ? quantitySold : 0,
      quantityRequested,
      sales: currentSale,
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

  private registerKeyboardShortcuts(): void {
    this.shortcutsService.registerAll({
      // Navigation
      focusProductSearch: () => this.produitbox()?.getFocus(),
      focusQuantity: () => this.quantyBox()?.focusProduitControl(),
      focusCustomer: () => {},
      focusVendor: () => this.userBox()?.nativeElement?.focus(),

      // Product actions
      //addProduct: () => this.onAddProduit(),
      addProduct: () => {},
      removeSelectedLine: () => {},
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
      switchToComptant: () => {},
      switchToAssurance: () => {},
      switchToCarnet: () => {},
      switchToDepotAgree: () => {},

      // Payment & Finalization
      finalizeSale: () => this.manageAmountDiv(),
      savePending: () => {},
      viewPendingSales: () => {},
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
      printReceipt: () => this.printSale(this.depotService.currentSale().saleId),

      // Tauri-specific (optional, only if user has permission)
      forceStock: this.canForceStock()
        ? () => {
            console.log('Force stock activated');
            // Force stock logic would go here
          }
        : undefined,
    });
  }
}
