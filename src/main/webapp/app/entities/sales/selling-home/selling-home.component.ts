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
  PLATFORM_ID, signal,
  viewChild
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { PreventeModalComponent } from '../prevente-modal/prevente-modal/prevente-modal.component';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { PanelModule } from 'primeng/panel';
import { TooltipModule } from 'primeng/tooltip';
import { INatureVente } from '../../../shared/model/nature-vente.model';
import { IUser } from '../../../core/user/user.model';
import { ITypePrescription } from '../../../shared/model/prescription-vente.model';
import { ICustomer } from '../../../shared/model/customer.model';
import { ProduitSearch } from '../../../shared/model/produit.model';
import { GroupRemise, IRemise } from '../../../shared/model/remise.model';
import {
  FinalyseSale,
  InputToFocus,
  ISales,
  SaleId,
  SaveResponse,
  StockError
} from '../../../shared/model/sales.model';
import { ISalesLine, SalesLine } from '../../../shared/model/sales-line.model';
import { PRODUIT_COMBO_MIN_LENGTH, PRODUIT_COMBO_RESULT_SIZE } from '../../../shared/constants/pagination.constants';
import { Observable } from 'rxjs';
import { SalesService } from '../sales.service';
import { CustomerService } from '../../customer/customer.service';
import { ProduitService } from '../../produit/produit.service';
import { NgbModal, NgbNavChangeEvent } from '@ng-bootstrap/ng-bootstrap';
import { AccountService } from '../../../core/auth/account.service';
import { ErrorService } from '../../../shared/error.service';
import { TranslateService } from '@ngx-translate/core';
import { HttpResponse } from '@angular/common/http';
import { CardModule } from 'primeng/card';
import { ComptantComponent } from './comptant/comptant.component';
import { CustomerOverlayPanelComponent } from '../customer-overlay-panel/customer-overlay-panel.component';
import { SelectedCustomerService } from '../service/selected-customer.service';
import { CurrentSaleService } from '../service/current-sale.service';
import { SelectModeReglementService } from '../service/select-mode-reglement.service';
import { LastCurrencyGivenService } from '../service/last-currency-given.service';
import { InputGroupModule } from 'primeng/inputgroup';
import { SalesStatut } from '../../../shared/model/enumerations/sales-statut.model';
import { AssuranceComponent } from './assurance/assurance.component';
import { AssuranceDataComponent } from './assurance/assurance-data/assurance-data.component';
import { TypePrescriptionService } from '../service/type-prescription.service';
import { UserCaissierService } from '../service/user-caissier.service';
import { UserVendeurService } from '../service/user-vendeur.service';
import { SaleEvent } from '../service/sale-event-manager.service';
import { VoSalesService } from '../service/vo-sales.service';
import { HasAuthorityService } from '../service/has-authority.service';
import { BaseSaleService } from '../service/base-sale.service';
import { CarnetComponent } from './carnet/carnet.component';
import { Authority } from '../../../shared/constants/authority.constants';
import { RemiseCacheService } from '../service/remise-cache.service';
import { Select } from 'primeng/select';
import { InputGroupAddonModule } from 'primeng/inputgroupaddon';
import { DrawerModule } from 'primeng/drawer';
import { ConfirmDialogComponent } from '../../../shared/dialog/confirm-dialog/confirm-dialog.component';
import { ToastAlertComponent } from '../../../shared/toast-alert/toast-alert.component';
import { QuantiteProdutSaisieComponent } from '../../../shared/quantite-produt-saisie/quantite-produt-saisie.component';
import {
  assignCustomerToSale,
  getActiveTab,
  getNavChangeMessage,
  isBonEmpty,
  isEditMode,
  isVno,
  isVo,
  SaleType,
  showCommonError,
  translateSalesLabel
} from './sale-helper';
import { SaleEventSignal } from './sale-event';
import { handleSaleEvents } from './sale-event-helper';
import { DeconditionnementService } from '../validator/deconditionnement.service';
import { ForceStockService } from '../validator/force-stock.service';
import { SaleStockValidator } from '../validator/sale-stock-validator.service';
import {
  ProduitSearchAutocompleteScannerComponent
} from '../../../shared/produit-search-autocomplete-scanner/produit-search-autocomplete-scanner.component';
import { Tag } from 'primeng/tag';
import { SellingHomeShortcutsService } from './racourci/selling-home-shortcuts.service';
import { KeyboardShortcutsService } from './racourci/keyboard-shortcuts.service';

@Component({
  selector: 'jhi-selling-home',
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
    AssuranceComponent,
    AssuranceDataComponent,
    CarnetComponent,
    Select,
    InputGroupAddonModule,
    DrawerModule,
    ConfirmDialogComponent,
    ToastAlertComponent,
    QuantiteProdutSaisieComponent,
    ProduitSearchAutocompleteScannerComponent
  ],
  templateUrl: './selling-home.component.html',
  styleUrl: './selling-home.component.scss'
})
export class SellingHomeComponent implements OnInit, AfterViewInit, OnDestroy {
  readonly minLength = PRODUIT_COMBO_MIN_LENGTH;
  readonly COMPTANT = 'COMPTANT';
  readonly CARNET = 'CARNET';
  readonly ASSURANCE = 'ASSURANCE';
  comptantComponent = viewChild(ComptantComponent);
  assuranceComponent = viewChild(AssuranceComponent);
  carnetComponent = viewChild(CarnetComponent);
  assuranceDataComponent = viewChild(AssuranceDataComponent);
  userBox = viewChild<any>('userBox');
  accountService = inject(AccountService);
  currentAccount = this.accountService.trackCurrentAccount();
  remiseCacheService = inject(RemiseCacheService);
  remises: GroupRemise[] = this.remiseCacheService.remises();
  protected canFocusLastModeInput = false;
  protected isLargeScreen = true;
  protected canForceStock: boolean;
  protected check = true; // mis pour le focus produit et dialogue button
  protected naturesVentes: INatureVente[] = [];
  protected naturesVente: INatureVente | null = null;
  protected userCaissier?: IUser | null;
  protected userSeller?: IUser;
  protected typePrescription?: ITypePrescription | null;
  protected customers: ICustomer[] = [];
  protected produitSelected?: ProduitSearch | null = null;
  protected appendTo = 'body';
  protected remise: IRemise[] = [];
  protected base64 = ';base64,';
  protected event: any;
  protected stockSeverity = 'success';
  protected commentaire?: string;
  protected telephone?: string;
  protected showAddModePaimentBtn = false;
  protected pendingSalesSidebar = false;
  protected isSaving = false;
  protected isPresale = false;
  protected showStock = true;
  protected printTicket = true;
  protected active = 'comptant';
  protected showInsuranceDataBar = signal(true);
  protected showInsuranceTogle = signal(false);
  protected sidebarCollapsed = signal(false);
  protected currentSaleService = inject(CurrentSaleService);
  protected userVendeurService = inject(UserVendeurService);
  protected readonly PRODUIT_COMBO_RESULT_SIZE = PRODUIT_COMBO_RESULT_SIZE;
  private readonly typePrescriptionService = inject(TypePrescriptionService);
  private readonly userCaissierService = inject(UserCaissierService);
  private readonly hasAuthorityService = inject(HasAuthorityService);
  private readonly voSalesService = inject(VoSalesService);
  private readonly baseSaleService = inject(BaseSaleService);
  private readonly selectModeReglementService = inject(SelectModeReglementService);
  private readonly selectedCustomerService = inject(SelectedCustomerService);
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

  toggleInsuranceDataBar(): void {
    this.showInsuranceDataBar.set(!this.showInsuranceDataBar());
  }

  toggleSidebar(): void {
    this.sidebarCollapsed.set(!this.sidebarCollapsed());
  }
  protected get disableButton(): boolean {
    return this.produitSelected == null || this.quantyBox().value < 1;
  }

  onRemoveCustomer(): void {
    if (this.isComptant()) {
      this.salesService.removeCustommerToCashSale(this.currentSaleService.currentSale().saleId).subscribe(() => {
        this.currentSaleService.currentSale().customerId = null;
      });
    }
  }

  onAddCustommer(): void {
    if (this.isComptant()) {
      this.salesService
        .addCustommerToCashSale({
          id: this.currentSaleService.currentSale().saleId,
          value: this.selectedCustomerService.selectedCustomerSignal().id
        })
        .subscribe(() => {
          this.currentSaleService.currentSale().customerId = this.selectedCustomerService.selectedCustomerSignal().id;
        });
    }
  }

  onLoadPrevente(sales: ISales, toEdit = false): void {
    if (!toEdit && sales.statut !== SalesStatut.CLOSED) {
      // modification vente cloturee
      // 1 annuler la vente originale
      // gerer ordonnance de la vente vo
      // Afficher
      // Avoir
      // suggestion auto
      // moidfier info client vo
      // modifier date vo
      // suggestion d'une vente
      // annulerVenteAnterieur
      // vente vo à exclure
      // annulerVenteAnterieur
      // notification
      this.router.navigate(['/sales', false, 'new']);
    } else {
      this.currentSaleService.setCurrentSale(sales);
      this.currentSaleService.setIsEdit(sales.statut === SalesStatut.CLOSED);
      this.active = getActiveTab(sales);
      //   this.selectedCustomerService.setCustomer(sales.customer);
      this.naturesVente = this.naturesVentes.find(e => e.code === sales.natureVente) || null;
      this.typePrescriptionService.setTypePrescription(this.typePrescription);
      this.userSeller = this.userVendeurService.vendeurs().find(e => e.id === sales.sellerId) || this.userSeller;
      this.userVendeurService.setVendeur(this.userSeller);
      this.loadPrevente();
    }
  }

  ngOnInit(): void {
    const width = window.innerWidth;
    if (width < 1800) {
      this.isLargeScreen = false;
    }
    this.currentSaleService.setCurrentSale(null);
    this.selectedCustomerService.setCustomer(null);
    this.userCaissier = { ...this.currentAccount() } as IUser;
    this.userCaissierService.setCaissier(this.userCaissier);

    this.typePrescription = this.typePrescriptionService.typePrescriptionDefault();

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
    if (this.isComptant()) {
      this.comptantComponent().manageAmountDiv();
    } else if (this.isAssurance()) {
      this.assuranceComponent().manageAmountDiv();
    } else if (this.isCartnet()) {
      this.carnetComponent().manageAmountDiv();
    }
  }

  previousState(): void {
    this.resetAll();
    window.history.back();
  }

  save(): void {
    if (this.currentSaleService.currentSale()) {
      if (this.isComptant()) {
        this.comptantComponent().save();
      } else if (this.isAssurance() || this.isCartnet()) {
        this.saveAssurance();
      }
    }
  }

  saveAssurance(): void {
    this.currentSaleService.currentSale().tiersPayants = this.assuranceDataComponent().buildIClientTiersPayantFromInputs();
    if (this.checkEmptyBon()) {
      if (this.baseSaleService.hasSansBon()) {
        this.confimDialog().onConfirm(
          () => {
            if (this.isAssurance()) {
              this.assuranceComponent().save();
            } else if (this.isCartnet()) {
              this.carnetComponent().save();
            }
          },
          this.translateLabel('venteSansBonHeader'),
          this.translateLabel('venteSansBon')
        );
      } else {
        this.confimDialog().onWarn(() => {
        }, this.translateLabel('numeroBonMaquant'), this.translateLabel('numeroBonMaquantHeader'));
      }
    } else {
      if (this.isAssurance()) {
        this.assuranceComponent().save();
      } else if (this.isCartnet()) {
        this.carnetComponent().save();
      }
    }
  }

  setEnAttenteAssurance(): void {
    this.currentSaleService.currentSale().tiersPayants = this.assuranceDataComponent().buildIClientTiersPayantFromInputs();
    if (this.isAssurance()) {
      this.assuranceComponent().finalyseSale(true);
    } else if (this.isCartnet()) {
      this.carnetComponent().finalyseSale(true);
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

  updateTransformedSales(): void {
    const curr = this.currentSaleService.currentSale();
    const cust = this.selectedCustomerService.selectedCustomerSignal();
    assignCustomerToSale(curr, cust);
    this.voSalesService.updateTransformedSale(curr).subscribe({
      next: () => {
        this.currentSaleService.setVoFromCashSale(false);
        this.voSalesService.find(curr.saleId).subscribe({
          next: res => {
            this.currentSaleService.setCurrentSale(res.body);
          }
        });
      },
      error: (err: any) => this.onCommonError(err)
    });
  }

  changeCustomer(): void {
    const curr = this.currentSaleService.currentSale();
    const oldCustomer = curr.customer;
    const cust = this.selectedCustomerService.selectedCustomerSignal();
    assignCustomerToSale(curr, cust);
    this.voSalesService
      .changeCustomer({
        id: curr.saleId,
        value: cust.id
      })
      .subscribe({
        next: () => {
          this.voSalesService.find(curr.saleId).subscribe({
            next: res => {
              this.currentSaleService.setCurrentSale(res.body);
            }
          });
        },
        error: (err: any) => this.onChangeCustomerError(err, oldCustomer)
      });
  }

  onAddProduit(qytMvt: number): void {
    if (this.produitSelected) {
      if (this.isComptant()) {
        if (this.currentSaleService.currentSale()) {
          this.comptantComponent().onAddProduit(this.createSalesLine(this.produitSelected, qytMvt));
        } else {
          this.comptantComponent().createComptant(this.createSalesLine(this.produitSelected, qytMvt));
        }
      } else if (this.isVoSale()) {
        if (this.selectedCustomerService.selectedCustomerSignal()) {
          if (this.isAssurance()) {
            if (this.currentSaleService.currentSale()) {
              this.assuranceComponent().onAddProduit(this.createSalesLine(this.produitSelected, qytMvt));
            } else {
              this.assuranceComponent().create(
                this.createSalesLine(this.produitSelected, qytMvt),
                this.assuranceDataComponent().buildIClientTiersPayantFromInputs()
              );
            }
          } else if (this.isCartnet()) {
            if (this.currentSaleService.currentSale()) {
              this.carnetComponent().onAddProduit(this.createSalesLine(this.produitSelected, qytMvt));
            } else {
              this.carnetComponent().create(
                this.createSalesLine(this.produitSelected, qytMvt),
                this.assuranceDataComponent().buildIClientTiersPayantFromInputs()
              );
            }
          }
        } else {
          this.alert().showError(this.translateLabel('emptyClient'));
          this.assuranceDataComponent().searchInput().nativeElement.focus();
        }
      }
    }
  }

  print(sale: ISales | null): void {
    if (sale !== null && sale !== undefined) {
      if (this.isComptant()) {
        this.comptantComponent().print(sale);
      } else if (this.isVoSale()) {
        if (this.isAssurance()) {
          this.assuranceComponent().print(sale);
        } else if (this.isCartnet()) {
          this.carnetComponent().print(sale);
        }
      }
      this.currentSaleService.setCurrentSale(null);
      this.selectedCustomerService.setCustomer(null);
    }
  }

  printSale(saleId: SaleId): void {
    if (this.isComptant()) {
      this.comptantComponent().printSale(saleId);
    } else if (this.isVoSale()) {
      if (this.isAssurance()) {
        this.assuranceComponent().printSale(saleId);
      } else if (this.isCartnet()) {
        this.carnetComponent().printSale(saleId);
      }
    }
  }

  showError(message: string): void {
    this.alert().showError(message);
  }

  openInfoDialog(message: string): void {
    showCommonError(this.modalService, message);
  }

  resetAll(): void {
    this.currentSaleService.reset();
    if (this.assuranceDataComponent()) {
      this.assuranceDataComponent().reset();
    }
    this.selectModeReglementService.resetAllModeReglements();
    this.selectedCustomerService.setCustomer(null);
    this.typePrescription = this.typePrescriptionService.typePrescriptionDefault();
    this.userSeller = this.userCaissier;
    this.userVendeurService.setVendeur(this.userCaissier);
    this.check = true;
    const lastCurrency = this.lastCurrencyGivenService.givenCurrency();
    this.lastCurrencyGivenService.setLastCurrency(lastCurrency);
    this.lastCurrencyGivenService.resetGivenCurrency();
    this.showAddModePaimentBtn = false;
    this.goToNew();
    if (!this.isComptant()) {
      this.active = 'comptant';
    }
    this.updateProduitQtyBox();
  }


  onChangeCashSaleToVo(): void {
    this.selectedCustomerService.setCustomer(null);
    this.salesService
      .transform({
        natureVente: 'ASSURANCE',
        saleId: this.currentSaleService.currentSale().id
      })
      .subscribe({
        next: res => {
          this.salesService.find(res.body).subscribe({
            next: res => {
              this.currentSaleService.setVoFromCashSale(true);
              if (res.body.customer) {
                this.customerService
                  .find(res.body.customer.id)
                  .subscribe({ next: (resp: HttpResponse<ICustomer>) => this.selectedCustomerService.setCustomer(resp.body) });
              }
              this.onLoadPrevente(res.body, false);
            }
          });
        },
        error: error => {
          this.onCommonError(error);
        }
      });
  }

  onChangeCashSaleToCarnet(): void {
    this.selectedCustomerService.setCustomer(null);
    this.salesService
      .transform({
        natureVente: 'CARNET',
        saleId: this.currentSaleService.currentSale().id
      })
      .subscribe({
        next: res => {
          this.salesService.find(res.body).subscribe({
            next: res => {
              this.currentSaleService.setVoFromCashSale(true);
              if (res.body.customer) {
                this.customerService
                  .find(res.body.customer.id)
                  .subscribe({ next: (resp: HttpResponse<ICustomer>) => this.selectedCustomerService.setCustomer(resp.body) });
              }
              this.onLoadPrevente(res.body, false);
            }
          });
        },
        error: error => {
          this.onCommonError(error);
        }
      });
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

  onNavChange(evt: NgbNavChangeEvent): void {
    const currentSale = this.currentSaleService.currentSale();
    // eslint-disable-next-line @typescript-eslint/no-unnecessary-condition
    if (currentSale) {
      evt.preventDefault();
      const message = this.getMessateOnNavChange(evt);
      this.confimDialog().onConfirm(
        () => {
          if (evt.nextId === SaleType.COMPTANT) {
            if (isVo(currentSale.categorie)) {

              this.setEnAttenteAssurance();
              this.active = SaleType.COMPTANT;
            }else{
              this.selectedCustomerService.setCustomer(null);
            }
          } else if (evt.nextId === SaleType.ASSURANCE) {
            this.onChangeCashSaleToVo();
            this.active = SaleType.ASSURANCE;
          } else if (evt.nextId === SaleType.CARNET) {
            if (isVno(currentSale.categorie)) {
              this.selectedCustomerService.setCustomer(null);
            }
            this.onChangeCashSaleToCarnet();
            this.active = SaleType.CARNET;
          }
        },
        this.translateLabel('modificationTypeVente'),
        message,
        null,
        () => evt.preventDefault()
      );
    }
  }

  onCustomerOverlay(evnt: boolean): void {
    this.produitbox().getFocus();
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
      if (this.isVoSale() && this.currentSaleService.currentSale().amountToBePaid === 0) {
        this.save();
      } else {
        this.manageAmountDiv();
      }
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

  private handleCompleteSale = (response: SaleEvent<unknown>): void => {
    const content = response.content;
    if ((this.isAssurance() || this.isCartnet()) && content === 'save') {
      this.saveAssurance();
    } else if (content === 'standby') {
      this.setEnAttenteAssurance();
    }
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

      // Effect for handling customer changes in credit/carnet sales (VO)
      effect(() => {
        const customer = this.selectedCustomerService.selectedCustomerSignal();
        const sale = this.currentSaleService.currentSale();

        if (!customer || !sale || !isVo(sale.categorie)) {
          return;
        }

        if (!sale.customerId && this.currentSaleService.voFromCashSale()) {
          this.updateTransformedSales();
        } else if (sale.customerId && sale.customerId !== customer.id) {
          this.changeCustomer();
        }
      });
    }
  }

  private isCartnet(): boolean {
    return this.active === SaleType.CARNET;
  }

  protected isAssurance(): boolean {
    return this.active === SaleType.ASSURANCE;
  }

  private isComptant(): boolean {
    return this.active === SaleType.COMPTANT;
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
    if (this.isComptant()) {
      this.processQtyRequestedForVNO(salesLine);
    } else if (this.isVoSale()) {
      this.processQtyRequestedVo(salesLine);
    }
  }

  private processQtyRequestedVo(salesLine: ISalesLine): void {
    this.voSalesService.updateItemQtyRequested(salesLine).subscribe({
      next: () => {
        if (this.currentSaleService.currentSale()) {
          this.subscribeToSaveResponse(this.voSalesService.find(this.currentSaleService.currentSale().saleId));
        }
        this.check = true;
      },
      error: error => {
        this.check = false;
        this.onStockError(salesLine, error);
      }
    });
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
      this.currentSaleService.setCurrentSale(sale);
    }

    this.updateProduitQtyBox();
  }

  private processQtyRequestedForVNO(salesLine: ISalesLine): void {
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
          if (this.isComptant()) {
            this.subscribeToSaveResponse(this.salesService.find(this.currentSaleService.currentSale().saleId));
          } else if (this.isVoSale()) {
            this.subscribeToSaveResponse(this.voSalesService.find(this.currentSaleService.currentSale().saleId));
          }
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
      if (!this.currentSaleService.plafondIsReached()) {
        this.currentSaleService.setPlafondIsReached(true);
        this.openInfoDialog(this.errorService.getErrorMessage(error.error));
      }
      if (this.currentSaleService.voFromCashSale()) {
        this.currentSaleService.setVoFromCashSale(false);
      }
    } else {
      this.openInfoDialog(this.errorService.getErrorMessage(error.error));
    }
  }

  private onChangeCustomerError(error: any, old: ICustomer): void {
    if (error.status === 412) {
      this.updateProduitQtyBox();
      if (!this.currentSaleService.plafondIsReached()) {
        this.currentSaleService.setPlafondIsReached(true);
        this.openInfoDialog(this.errorService.getErrorMessage(error));
      }
    } else {
      this.selectedCustomerService.setCustomer(old);
      this.openInfoDialog(this.errorService.getErrorMessage(error));
    }
  }

  private checkEmptyBon(): boolean {
    if (this.isPresale) {
      return false;
    }
    if (this.currentSaleService.currentSale() && this.currentSaleService.isVenteSansBon()) {
      return false;
    }
    return this.currentSaleService.currentSale().tiersPayants.some(isBonEmpty);
  }

  private getMessateOnNavChange(evt: NgbNavChangeEvent): string {
    return getNavChangeMessage(evt.nextId, this.translate);
  }

  private isVoSale(): boolean {
    return this.isAssurance() || this.isCartnet();
  }

  private loadPrevente(): void {
    queueMicrotask(() => {
      if (this.isComptant()) {
        this.comptantComponent().onLoadPrevente();
      } else if (this.isVoSale()) {
        if (this.isAssurance()) {
          this.assuranceComponent().onLoadPrevente();
        } else if (this.isCartnet()) {
          this.carnetComponent().onLoadPrevente();
        }
      }
    });
  }

  private createSalesLine(produit: ProduitSearch, quantityRequested: number): ISalesLine {
   const quantitySold=Math.min(produit.totalQuantity, quantityRequested);
    return {
      ...new SalesLine(),
      produitId: produit.id,
      regularUnitPrice: produit.regularUnitPrice,
      saleId: this.currentSaleService.currentSale()?.id,
      quantitySold: quantitySold>0 ? quantitySold : 0,
      quantityRequested,
      sales: this.currentSaleService.currentSale()
    };
  }

  private onPrintInvoice(): void {
    if (this.isComptant()) {
      this.comptantComponent().printInvoice();
    } else if (this.isVoSale()) {
      if (this.isAssurance()) {
        this.assuranceComponent().printInvoice();
      } else if (this.isCartnet()) {
        this.carnetComponent().printInvoice();
      }
    }
  }

  private translateLabel(label: string): string {
    return translateSalesLabel(this.translate, label);
  }

  private goToNew() {
    this.router.navigate(['/sales', 'false', 'new']);
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
        if (this.isComptant()) {
          // this.customerBox()?.show();
        }
      },
      focusVendor: () => this.userBox()?.nativeElement?.focus(),

      // Product actions
      //addProduct: () => this.onAddProduit(),
      addProduct: () => {
      },
      removeSelectedLine: () => {
        if (this.isComptant()) {
          //   this.comptantComponent()?.removeSelectedItem();
        } else if (this.isAssurance()) {
          // this.assuranceComponent()?.removeSelectedItem();
        } else if (this.isCartnet()) {
          // this.carnetComponent()?.removeSelectedItem();
        }
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
        this.active = 'comptant';
      },
      switchToAssurance: () => {
        this.active = 'assurance';
      },
      switchToCarnet: () => {
        this.active = 'carnet';
      },

      // Payment & Finalization
      finalizeSale: () => this.manageAmountDiv(),
      savePending: () => {
        if (this.isComptant()) {
          this.comptantComponent()?.putCurrentSaleOnStandBy();
        } else if (this.isAssurance()) {
          this.setEnAttenteAssurance();
        } else if (this.isCartnet()) {
          this.setEnAttenteAssurance();
        }
      },
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
      applyDiscount: () => {
        if (this.isComptant()) {
          // this.comptantComponent()?.openRemiseModal();
        } else if (this.isAssurance()) {
          //  this.assuranceComponent()?.openRemiseModal();
        } else if (this.isCartnet()) {
          // this.carnetComponent()?.openRemiseModal();
        }
      },
      removeDiscount: () => {
        if (this.isComptant()) {
          //  this.comptantComponent()?.removeDiscount();
        } else if (this.isAssurance()) {
          //  this.assuranceComponent()?.removeDiscount();
        } else if (this.isCartnet()) {
          //  this.carnetComponent()?.removeDiscount();
        }
      },

      // Printing
      printInvoice: () => this.onPrintInvoice(),
      printReceipt: () => {
        if (this.isComptant()) {
          this.comptantComponent()?.printSale(this.currentSaleService.currentSale()?.saleId);
        } else if (this.isAssurance()) {
          this.assuranceComponent()?.printSale(this.currentSaleService.currentSale()?.saleId);

        } else if (this.isCartnet()) {
          this.carnetComponent()?.printSale(this.currentSaleService.currentSale()?.saleId);
        }
      },

      // Tauri-specific (optional, only if user has permission)
      forceStock: this.canForceStock ? () => {
        console.log('Force stock activated');
        // Force stock logic would go here
      } : undefined
    });
  }
}
