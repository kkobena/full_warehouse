import { AfterViewInit, Component, effect, ElementRef, inject, OnInit, viewChild } from '@angular/core';
import { AutoCompleteModule } from 'primeng/autocomplete';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogModule } from 'primeng/dialog';
import { DividerModule } from 'primeng/divider';
import { DropdownModule } from 'primeng/dropdown';
import { InputSwitchModule } from 'primeng/inputswitch';
import { InputTextModule } from 'primeng/inputtext';
import { KeyFilterModule } from 'primeng/keyfilter';
import { ConfirmationService, PrimeNGConfig } from 'primeng/api';
import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { PreventeModalComponent } from '../prevente-modal/prevente-modal/prevente-modal.component';
import { SidebarModule } from 'primeng/sidebar';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { NgxSpinnerModule } from 'ngx-spinner';
import { TableModule } from 'primeng/table';
import { RippleModule } from 'primeng/ripple';
import { FormsModule } from '@angular/forms';
import { PanelModule } from 'primeng/panel';
import { SelectButtonModule } from 'primeng/selectbutton';
import { TooltipModule } from 'primeng/tooltip';
import { TagModule } from 'primeng/tag';
import { OverlayPanelModule } from 'primeng/overlaypanel';
import { UninsuredCustomerListComponent } from '../uninsured-customer-list/uninsured-customer-list.component';
import { AssuredCustomerListComponent } from '../assured-customer-list/assured-customer-list.component';
import { FormAssuredCustomerComponent } from '../../customer/form-assured-customer/form-assured-customer.component';
import { AyantDroitCustomerListComponent } from '../ayant-droit-customer-list/ayant-droit-customer-list.component';
import { TiersPayantCustomerListComponent } from '../../customer/tiers-payant-customer-list/tiers-payant-customer-list.component';
import { FormAyantDroitComponent } from '../../customer/form-ayant-droit/form-ayant-droit.component';
import { INatureVente } from '../../../shared/model/nature-vente.model';
import { IUser, User } from '../../../core/user/user.model';
import { IPaymentMode } from '../../../shared/model/payment-mode.model';
import { ITypePrescription } from '../../../shared/model/prescription-vente.model';
import { ICustomer } from '../../../shared/model/customer.model';
import { IProduit } from '../../../shared/model/produit.model';
import { IRemise } from '../../../shared/model/remise.model';
import { FinalyseSale, InputToFocus, ISales, Sales, SaveResponse } from '../../../shared/model/sales.model';
import { ISalesLine, SalesLine } from '../../../shared/model/sales-line.model';
import { PRODUIT_COMBO_MIN_LENGTH, PRODUIT_NOT_FOUND } from '../../../shared/constants/pagination.constants';
import { Observable, Subscription } from 'rxjs';
import { SalesService } from '../sales.service';
import { CustomerService } from '../../customer/customer.service';
import { ProduitService } from '../../produit/produit.service';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { UserService } from '../../../core/user/user.service';
import { AccountService } from '../../../core/auth/account.service';
import { ErrorService } from '../../../shared/error.service';
import { ConfigurationService } from '../../../shared/configuration.service';
import { DeconditionService } from '../../decondition/decondition.service';
import { TranslateService } from '@ngx-translate/core';
import { AssuranceService } from '../assurance.service';
import { Decondition, IDecondition } from '../../../shared/model/decondition.model';
import { HttpResponse } from '@angular/common/http';
import { saveAs } from 'file-saver';
import { AlertInfoComponent } from '../../../shared/alert/alert-info.component';
import { ProductTableComponent } from './product-table/product-table.component';
import { CardModule } from 'primeng/card';
import { PresaleComponent } from '../presale/presale.component';
import { SalesComponent } from '../sales.component';
import { VenteEnCoursComponent } from '../vente-en-cours/vente-en-cours.component';
import { ComptantComponent } from './comptant/comptant.component';
import { CustomerOverlayPanelComponent } from '../customer-overlay-panel/customer-overlay-panel.component';
import { SelectedCustomerService } from '../service/selected-customer.service';
import { CurrentSaleService } from '../service/current-sale.service';
import { SelectModeReglementService } from '../service/select-mode-reglement.service';
import { LastCurrencyGivenService } from '../service/last-currency-given.service';
import { InputGroupModule } from 'primeng/inputgroup';
import { SalesStatut } from '../../../shared/model/enumerations/sales-statut.model';
import { QuanityMaxService } from '../service/quanity-max.service';
import { AssuranceComponent } from './assurance/assurance.component';
import { AssuranceDataComponent } from './assurance/assurance-data/assurance-data.component';
import { TypePrescriptionService } from '../service/type-prescription.service';
import { UserCaissierService } from '../service/user-caissier.service';
import { UserVendeurService } from '../service/user-vendeur.service';

@Component({
  selector: 'jhi-selling-home',
  standalone: true,
  providers: [ConfirmationService, DialogService],
  imports: [
    WarehouseCommonModule,
    PreventeModalComponent,
    SidebarModule,
    RouterModule,
    NgxSpinnerModule,
    TableModule,
    InputTextModule,
    ButtonModule,
    RippleModule,
    FormsModule,
    DialogModule,
    ConfirmDialogModule,
    PanelModule,
    SelectButtonModule,
    AutoCompleteModule,
    TooltipModule,
    DividerModule,
    KeyFilterModule,
    TagModule,
    DropdownModule,
    InputSwitchModule,
    OverlayPanelModule,
    UninsuredCustomerListComponent,
    AssuredCustomerListComponent,
    FormAssuredCustomerComponent,
    AyantDroitCustomerListComponent,
    TiersPayantCustomerListComponent,
    FormAyantDroitComponent,
    ProductTableComponent,
    CardModule,
    PresaleComponent,
    SalesComponent,
    VenteEnCoursComponent,
    ComptantComponent,
    CustomerOverlayPanelComponent,
    InputGroupModule,
    AssuranceComponent,
    AssuranceDataComponent,
  ],
  templateUrl: './selling-home.component.html',
})
export class SellingHomeComponent implements OnInit, AfterViewInit {
  readonly minLength = PRODUIT_COMBO_MIN_LENGTH;

  readonly COMPTANT = 'COMPTANT';
  readonly CARNET = 'CARNET';
  readonly ASSURANCE = 'ASSURANCE';
  readonly notFoundText = PRODUIT_NOT_FOUND;
  quantyBox = viewChild.required<ElementRef>('quantyBox');
  comptantComponent = viewChild(ComptantComponent);
  assuranceComponent = viewChild(AssuranceComponent);
  assuranceDataComponent = viewChild(AssuranceDataComponent);
  produitbox = viewChild.required<any>('produitbox');
  userBox = viewChild.required<any>('userBox');
  forcerStockDialogBtn = viewChild.required<ElementRef>('forcerStockDialogBtn');
  quantityMaxService = inject(QuanityMaxService);
  typePrescriptionService = inject(TypePrescriptionService);
  userCaissierService = inject(UserCaissierService);
  userVendeurService = inject(UserVendeurService);
  currentAccount = this.accountService.trackCurrentAccount();
  protected check = true; // mis pour le focus produit et dialogue button
  protected printInvoice = false;
  protected canForceStock = true;
  protected naturesVentes: INatureVente[] = [];
  protected naturesVente: INatureVente | null = null;
  protected users: IUser[];
  protected modeReglementSelected: IPaymentMode[] = [];
  protected userCaissier?: IUser | null;
  protected userSeller?: IUser;
  protected typePrescriptions: ITypePrescription[] = [];
  protected typePrescription?: ITypePrescription | null;
  protected customers: ICustomer[] = [];
  protected produits: IProduit[] = [];
  protected produitSelected?: IProduit | null = null;
  protected searchValue?: string;
  protected appendTo = 'body';
  protected imagesPath!: string;
  protected remise: IRemise[] = [];
  protected sale?: ISales | null = null;
  protected quantiteSaisie = 1;
  protected base64!: string;
  protected event: any;
  protected stockSeverity = 'success';
  protected commentaire?: string;
  protected telephone?: string;
  protected ref!: DynamicDialogRef;
  protected primngtranslate: Subscription;
  protected showAddModePaimentBtn = false;
  protected pendingSalesSidebar = false;
  protected isSaving = false;
  protected isPresale = false;
  protected commonDialog = false;
  protected displayErrorEntryAmountModal = false;
  protected showStock = true;
  protected canUpdatePu = true;
  protected printTicket = true;
  protected active = 'comptant';
  protected monnaie: number;
  protected derniereMonnaie: number;

  constructor(
    protected selectModeReglementService: SelectModeReglementService,
    protected currentSaleService: CurrentSaleService,
    protected selectedCustomerService: SelectedCustomerService,
    protected lastCurrencyGivenService: LastCurrencyGivenService,
    protected salesService: SalesService,
    protected customerService: CustomerService,
    protected produitService: ProduitService,
    protected activatedRoute: ActivatedRoute,
    protected router: Router,
    protected modalService: NgbModal,
    protected userService: UserService,
    private accountService: AccountService,
    public confirmationService: ConfirmationService,
    protected errorService: ErrorService,
    protected configurationService: ConfigurationService,
    protected decondtionService: DeconditionService,
    public dialogService: DialogService,
    public translate: TranslateService,
    public primeNGConfig: PrimeNGConfig,
    private assuranceService: AssuranceService,
  ) {
    this.imagesPath = 'data:image/';
    this.base64 = ';base64,';
    this.searchValue = '';
    this.translate.use('fr');
    this.primngtranslate = this.translate.stream('primeng').subscribe(data => {
      this.primeNGConfig.setTranslation(data);
    });
    effect(() => {
      this.sale = this.currentSaleService.currentSale();
    });
    effect(() => {
      if (this.selectedCustomerService.selectedCustomerSignal() && this.sale) {
        this.salesService
          .addCustommerToCashSale({
            key: this.sale.id,
            value: this.selectedCustomerService.selectedCustomerSignal().id,
          })
          .subscribe(() => {
            this.sale.customerId = this.selectedCustomerService.selectedCustomerSignal().id;

            this.currentSaleService.setCurrentSale(this.sale);
          });
      } else {
        if (this.sale) {
          this.salesService.removeCustommerToCashSale(this.sale.id).subscribe(() => {});
        }
      }
    });
    effect(() => {
      this.modeReglementSelected = this.selectModeReglementService.modeReglements();
    });
    effect(() => {
      this.monnaie = this.lastCurrencyGivenService.givenCurrency();
    });
  }

  isCashSale(): boolean {
    return this.sale?.type === 'VNO' || this.active === 'comptant';
  }

  onLoadPrevente(sales: ISales): void {
    if (sales.statut === SalesStatut.CLOSED) {
      this.router.navigate(['/sales', false, 'new']);
    } else {
      this.currentSaleService.setCurrentSale(sales);
      if (sales && sales.type === 'VNO') {
        this.active = 'comptant';
      }
      this.selectedCustomerService.setCustomer(sales.customer);
      this.naturesVente = this.naturesVentes.find(e => e.code === sales.natureVente) || null;
      this.typePrescription = this.typePrescriptions.find(e => e.code === sales.typePrescription) || null;
      this.typePrescriptionService.setTypePrescription(this.typePrescription);
      this.userSeller = this.users?.find(e => e.id === sales.sellerId) || this.userSeller;
      this.userVendeurService.setVendeur(this.userSeller);
      this.loadPrevente();
    }
  }

  ngOnInit(): void {
    this.currentSaleService.setCurrentSale(null);
    this.selectedCustomerService.setCustomer(null);
    this.selectModeReglementService.selectCashModePayment();
    this.loadAllUsers();
    this.maxToSale();
    this.userCaissier = { ...this.currentAccount() } as IUser;
    this.userCaissierService.setCaissier(this.userCaissier);
    this.typePrescriptions = [
      { code: 'PRESCRIPTION', name: 'Prescription' },
      {
        code: 'CONSEIL',
        name: 'Conseil',
      },
      { code: 'DEPOT', name: 'Dépôt' },
    ];
    this.typePrescription = this.typePrescriptionService.typePrescriptionDefault() || this.typePrescriptions[0];

    this.activatedRoute.data.subscribe(({ sales }) => {
      if (sales.id) {
        this.onLoadPrevente(sales);
      }
      this.loadProduits();
    });
    this.activatedRoute.paramMap.subscribe(params => {
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

  loadAllUsers(): void {
    this.userService.query().subscribe((res: HttpResponse<User[]>) => (this.users = res.body || []));
  }

  manageAmountDiv(): void {
    if (this.active === 'comptant') {
      this.comptantComponent().manageAmountDiv();
    }
  }

  previousState(): void {
    this.resetAll();
    window.history.back();
  }

  onHidedisplayErrorEntryAmountModal(event: Event): void {
    // this.montantCashInput?.nativeElement.focus();
  }

  save(): void {
    if (this.sale) {
      if (this.active === 'comptant') {
        this.comptantComponent().save();
      }
    }
  }

  onHideHideDialog(): void {}

  cancelCommonDialog(): void {
    this.commonDialog = false;
  }

  canceldisplayErrorEntryAmountModal(): void {
    this.displayErrorEntryAmountModal = false;
  }

  searchUser(): void {
    this.loadAllUsers();
  }

  onSelectUser(): void {
    setTimeout(() => {
      this.produitbox().inputEL.nativeElement.focus();
      this.produitbox().inputEL.nativeElement.select();
    }, 50);
  }

  searchFn(event: any): void {
    {
      this.searchValue = event.query;
      this.loadProduits();
    }
  }

  onSelect(): void {
    setTimeout(() => {
      this.quantyBox().nativeElement.focus();
      this.quantyBox().nativeElement.select();
    }, 50);

    if (this.produitSelected.totalQuantity > 0) {
      this.stockSeverity = 'success';
    } else {
      this.stockSeverity = 'danger';
    }
  }

  onSelectKeyDow(event: KeyboardEvent): void {
    if (event.key === 'Enter' && this.produitSelected) {
      if (this.quantyBox()) {
        const el = this.quantyBox().nativeElement;
        el.focus();
        el.select();
      }
      if (this.produitSelected.totalQuantity > 0) {
        this.stockSeverity = 'success';
      } else {
        this.stockSeverity = 'danger';
      }
    } else if (event.key === 'Enter' && this.sale?.salesLines.length > 0) {
      if (this.active !== 'comptant' && this.sale.amountToBePaid === 0) {
        this.save();
      } else {
        this.manageAmountDiv();
      }
    }
  }

  loadProduits(): void {
    this.produitService
      .queryLite({
        page: 0,
        size: 5,
        withdetail: false,
        search: this.searchValue,
      })
      .subscribe((res: HttpResponse<any[]>) => this.onProduitSuccess(res.body));
  }

  totalItemQty(): number {
    if (this.produitSelected) {
      return this.sale?.salesLines.find(e => e.produitId === this.produitSelected.id)?.quantityRequested || 0;
    }
    return 0;
  }

  onQtyBntClickEvent(): void {
    const qytMvt = Number(this.quantyBox().nativeElement.value);
    this.onAddNewQty(qytMvt);
  }

  onAddNewQty(qytMvt: number): void {
    if (qytMvt <= 0) {
      return;
    }
    const qtyMaxToSel = this.quantityMaxService.quantityMax();
    if (this.produitSelected !== null && this.produitSelected !== undefined) {
      const currentStock = this.produitSelected.totalQuantity;
      const qtyAlreadyRequested = this.totalItemQty();
      const inStock = currentStock >= qytMvt + qtyAlreadyRequested;
      if (!inStock) {
        if (this.canForceStock && qytMvt > qtyMaxToSel) {
          this.confirmForceStock(qytMvt, ' La quantité saisie est supérieure à maximale à vendre. Voullez-vous continuer ?');
        } else if (this.canForceStock && qytMvt <= qtyMaxToSel) {
          if (this.produitSelected.produitId) {
            // s il  boite ch
            this.produitService.find(this.produitSelected.produitId).subscribe(res => {
              const prod = res.body;
              if (prod && prod.totalQuantity > 0) {
                // si quantite CH
                this.confirmDeconditionnement(null, prod, qytMvt);
              } else {
                this.openInfoDialog('La quantité saisie est supérieure à la quantité stock du produit', 'alert alert-danger');
              }
            });
          } else {
            this.confirmForceStock(qytMvt, ' La quantité saisie est supérieure à la quantité stock du produit. Voullez-vous continuer ?');
          }
        } else {
          this.openInfoDialog('La quantité saisie est supérieure à la quantité stock du produit', 'alert alert-danger');
        }
      } else {
        if (qytMvt >= qtyMaxToSel) {
          if (this.canForceStock) {
            this.confirmForceStock(qytMvt, ' La quantité saisie est supérieure à maximale à vendre. Voullez-vous continuer ?');
          } else {
            this.openInfoDialog('La quantité saisie est supérieure à maximale à vendre', 'alert alert-danger');
          }
        } else {
          this.onAddProduit(qytMvt);
        }
      }
    }
  }

  onQuantityBoxAction(event: any): void {
    const qytMvt = Number(event.target.value);

    this.onAddNewQty(qytMvt);
  }

  onAddProduit(qytMvt: number): void {
    if (this.produitSelected) {
      if (this.active === 'comptant') {
        if (this.sale) {
          this.comptantComponent().onAddProduit(this.createSalesLine(this.produitSelected, qytMvt));
        } else {
          this.comptantComponent().createComptant(this.createSalesLine(this.produitSelected, qytMvt));
        }
      } else if (this.naturesVente && (this.naturesVente.code === this.CARNET || this.naturesVente.code === this.ASSURANCE)) {
        // this.subscribeToSaveLineResponse(this.assuranceService.addItem(this.createSalesLine(this.produitSelected, qytMvt)));
      }
    }

    /* if (this.produitSelected) {
       if (this.naturesVente && this.naturesVente.code === this.COMPTANT) {
         this.subscribeToCreateSaleComptantResponse(this.salesService.createComptant(this.createSaleComptant(this.produitSelected, qytMvt)));
       } else if (this.naturesVente && (this.naturesVente.code === this.CARNET || this.naturesVente.code === this.ASSURANCE)) {
         this.subscribeToCreateSaleComptantResponse(this.assuranceService.create(this.createThirdPartySale(this.produitSelected, qytMvt)));
       }
     }*/

    // this.computeMonnaie(null);
  }

  print(sale: ISales | null): void {
    if (sale !== null && sale !== undefined) {
      this.salesService.print(sale.id).subscribe(blod => saveAs(blod));
      this.sale = null;
      this.loadProduits();
      this.selectedCustomerService.setCustomer(null);
    }
  }

  printSale(saleId: number, categorie: string): void {
    this.salesService.printReceipt(saleId, categorie).subscribe();
  }

  openInfoDialog(message: string, infoClass: string): void {
    const modalRef = this.modalService.open(AlertInfoComponent, {
      backdrop: 'static',
      centered: true,
    });
    modalRef.componentInstance.message = message;
    modalRef.componentInstance.infoClass = infoClass;
  }

  confirmDeconditionnement(item: ISalesLine | null, produit: IProduit, qytMvt: number): void {
    this.confirmationService.confirm({
      message: 'Stock détail insuffisant . Voullez-vous faire un déconditionnement ?',
      header: 'DECONDITIONNEMENT A LA VENTE',
      icon: 'pi pi-info-circle',
      accept: () => {
        const qtyDetail = produit.itemQty;
        if (qtyDetail) {
          const qtyDecondtionner = Math.round(qytMvt / qtyDetail);
          this.decondtionService.create(this.createDecondition(qtyDecondtionner, produit.id)).subscribe({
            next: () => {
              if (item) {
                this.processQtyRequested(item);
              } else {
                this.onAddProduit(qytMvt);
              }
            },
            error: error => {
              if (error.error && error.error.status === 500) {
                this.openInfoDialog('Erreur applicative', 'alert alert-danger');
              } else {
                this.errorService.getErrorMessageTranslation(error.error.errorKey).subscribe(
                  translatedErrorMessage => {
                    this.openInfoDialog(translatedErrorMessage, 'alert alert-danger');
                  },
                  () => this.openInfoDialog(error.error.title, 'alert alert-danger'),
                );
              }
            },
          });
        }
      },
      reject: () => {
        this.check = true;
        this.updateProduitQtyBox();
      },
      key: 'forcerStock',
    });
    // this.forcerStockBtn.nativeElement.focus();
  }

  onUpdateConfirmForceStock(salesLine: ISalesLine, message: string): void {
    this.confirmationService.confirm({
      message,
      header: 'FORCER LE STOCK ',
      icon: 'pi pi-info-circle',
      accept: () => {
        this.processQtyRequested(salesLine);
      },
      reject: () => {
        this.check = true;
        this.updateProduitQtyBox();
      },
      key: 'forcerStockDialog',
    });
    this.forcerStockDialogBtn().nativeElement.focus();
  }

  maxToSale(): void {
    const quantityMax = this.quantityMaxService.quantityMax();
    if (!quantityMax) {
      this.configurationService.find('APP_QTY_MAX').subscribe(res => {
        if (res.body) {
          this.quantityMaxService.setQuantity(Number(res.body.value));
        }
      });
    }
  }

  resetAll(): void {
    this.currentSaleService.setCurrentSale(null);
    this.selectedCustomerService.setCustomer(null);
    this.modeReglementSelected = [];

    this.typePrescription = this.typePrescriptionService.typePrescriptionDefault();
    this.userSeller = this.userCaissier;
    this.userVendeurService.setVendeur(this.userCaissier);
    this.check = true;
    this.lastCurrencyGivenService.setLastCurrency(this.monnaie);
    this.lastCurrencyGivenService.setGivenCurrentSale(0);
    this.showAddModePaimentBtn = false;
    this.updateProduitQtyBox();
    this.loadProduits();
  }

  onNatureVenteChange(event: any): void {
    const selectNature = event.value;

    if (selectNature.code !== this.COMPTANT && this.sale && this.selectedCustomerService.selectedCustomerSignal()) {
      const nature = (element: INatureVente) => element.code === this.COMPTANT;
      this.naturesVentes.find(nature)!.disabled = true;

      // TODO si vente en cours et vente est de type VO, alors message si la nvelle est COMPTANT, ON ne peut transformer une VO en VNO
    }
    if (selectNature.code !== this.COMPTANT && !this.selectedCustomerService.selectedCustomerSignal()) {
      // this.setClientSearchBoxFocus();
    } else {
      this.produitbox().inputEL.nativeElement.focus();
    }
  }

  openPindingSide(): void {
    this.pendingSalesSidebar = true;
  }

  closeSideBar(booleanValue: boolean): void {
    this.pendingSalesSidebar = booleanValue;
    this.loadPrevente();
  }

  onSave(saveResponse: SaveResponse): void {
    if (saveResponse.success) {
      this.updateProduitQtyBox();
    } else {
      this.onCommonError(saveResponse.error);
    }
  }

  onFinalyse(finalyseSale: FinalyseSale): void {
    if (finalyseSale.success) {
      let saleType = 'vno';
      if (this.active === 'vo') {
        saleType = 'vo';
      }
      if (!finalyseSale.putOnStandBy) {
        if (this.printTicket) {
          this.printSale(finalyseSale.saleId, saleType);
        }
        if (this.printInvoice) {
          this.onPrintInvoice();
        }
      }

      this.resetAll();
    } else {
      this.onCommonError(finalyseSale.error);
    }
  }

  getControlToFocus(inputToFocusEvent: InputToFocus): void {
    if (inputToFocusEvent.control === 'produitBox') {
      this.updateProduitQtyBox();
    }
  }

  getToolBarCssClass(): string {
    const cust = this.selectedCustomerService.selectedCustomerSignal();
    let css = 'col-md-5';
    if (this.active === 'comptant' && cust) {
      css = 'col-md-4';
    } else if (this.active === 'comptant' && !cust && !this.sale) {
      css = 'col-md-7';
    }
    return css;
  }

  getToolBarActionCssClass(): string {
    let css = 'col-md-6';
    if (this.active === 'comptant' && !this.sale) {
      css = 'col-md-5';
    }
    if (this.active === 'comptant' && this.sale) {
      css = 'col-md-2';
    }
    return css;
  }

  getToolBarCustomerCssClass(): string {
    let css = 'col-md-6';
    if (this.sale && !this.selectedCustomerService.selectedCustomerSignal()) {
      return 'col-md-5';
    }
    return css;
  }

  onNavChange(evt: any): void {
    const currentSale = this.currentSaleService.currentSale();

    if (evt.nextId === 'comptant') {
      if (currentSale && currentSale.categorie === 'VO') {
        this.active = 'assurance';
        evt.preventDefault();
      }
    }
  }

  clickNavChange(evt: any): void {
    console.log(evt);
    console.log(this.active);
  }

  onTypePrescriptionChange(event: any): void {
    this.typePrescriptionService.setTypePrescription(event.value);
  }

  protected processQtyRequested(salesLine: ISalesLine): void {
    if (this.active === 'comptant') {
      this.processQtyRequestedForVNO(salesLine);
    } else if (this.naturesVente.code === this.CARNET || this.naturesVente.code === this.ASSURANCE) {
      this.processQtyRequestedForCarnet(salesLine);
    }
  }

  protected processQtyRequestedForVNO(salesLine: ISalesLine): void {
    this.salesService.updateItemQtyRequested(salesLine).subscribe({
      next: () => {
        if (this.sale) {
          this.subscribeToSaveResponse(this.salesService.find(this.sale.id));
        }
        this.check = true;
      },
      error: error => {
        this.check = false;
        this.subscribeToSaveResponse(this.salesService.find(this.sale.id));
        this.onStockError(salesLine, error);
      },
    });
  }

  protected processQtyRequestedForCarnet(salesLine: ISalesLine): void {
    this.assuranceService.updateItemQtyRequested(salesLine).subscribe({
      next: () => {
        if (this.sale) {
          this.subscribeToSaveResponse(this.assuranceService.find(this.sale.id));
        }
        this.check = true;
      },
      error: error => {
        this.check = false;
        this.subscribeToSaveResponse(this.salesService.find(this.sale.id));
        this.onStockError(salesLine, error);
      },
    });
  }

  protected updateProduitQtyBox(): void {
    if (this.quantyBox()) {
      this.quantyBox().nativeElement.value = 1;
    }
    if (this.check) {
      this.produitbox().inputEL.nativeElement.focus();
    } else {
      this.forcerStockDialogBtn().nativeElement.focus();
    }

    this.produitSelected = null;
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<ISales>>): void {
    result.subscribe({
      next: (res: HttpResponse<ISales>) => this.onSaveSuccess(res.body),
      error: () => this.onSaveError(),
    });
  }

  protected onProduitSuccess(data: IProduit[] | null): void {
    this.produits = data || [];
  }

  protected onSaveSuccess(sale: ISales | null): void {
    this.isSaving = false;
    this.sale = sale!;

    this.updateProduitQtyBox();
  }

  protected onSaveError(): void {
    this.isSaving = false;
    const message = 'Une erreur est survenue';
    this.openInfoDialog(message, 'alert alert-danger');
  }

  protected refresh(): void {
    this.subscribeToSaveResponse(this.salesService.find(this.sale?.id));
  }

  protected onStockError(salesLine: ISalesLine, error: any): void {
    if (error.error) {
      if (error.error.errorKey === 'stock') {
        if (this.canForceStock) {
          salesLine.forceStock = true;
          this.onUpdateConfirmForceStock(
            salesLine,
            'La quantité saisie est supérieure à la quantité stock du produit. Voullez-vous continuer ?',
          );
        } else {
          this.errorService.getErrorMessageTranslation(error.error.errorKey).subscribe(translatedErrorMessage => {
            this.openInfoDialog(translatedErrorMessage, 'alert alert-danger');
          });
        }
      } else if (error.error.errorKey === 'stockChInsufisant') {
        this.produitService.find(Number(error.error.title)).subscribe(res => {
          const prod = res.body;
          if (prod && prod.totalQuantity > 0) {
            // si quantite CH
            this.confirmDeconditionnement(salesLine, prod, salesLine.quantityRequested);
          } else {
            this.openInfoDialog('La quantité saisie est supérieure à la quantité stock du produit', 'alert alert-danger');
          }
        });
      }
    }
  }

  protected onStockOutError(error: any): void {
    if (error.error) {
      this.errorService.getErrorMessageTranslation(error.error.errorKey).subscribe(translatedErrorMessage => {
        this.openInfoDialog(translatedErrorMessage, 'alert alert-danger');
      });
    }
  }

  protected onCommonError(error: any): void {
    if (error.error && error.error.status === 500) {
      this.openInfoDialog('Erreur applicative', 'alert alert-danger');
    } else {
      this.errorService.getErrorMessageTranslation(error.error.errorKey).subscribe({
        next: translatedErrorMessage => {
          this.openInfoDialog(translatedErrorMessage, 'alert alert-danger');
        },
        error: () => this.openInfoDialog(error.error.title, 'alert alert-danger'),
      });
    }
  }

  protected onCustomerOverlay(evnt: boolean): void {
    this.produitbox().inputEL.nativeElement.focus();
  }

  private loadPrevente(): void {
    setTimeout(() => {
      if (this.sale?.payments?.length > 0) {
        if (this.active === 'comptant') {
          this.comptantComponent().onLoadPrevente();
        }
      } else {
        this.updateProduitQtyBox();
      }
    }, 20);
  }

  private createDecondition(qtyDeconditione: number, produitId: number): IDecondition {
    return {
      ...new Decondition(),
      qtyMvt: qtyDeconditione,
      produitId,
    };
  }

  private createSaleComptant(produit: IProduit, quantitySold: number): ISales {
    let currentCustomer = this.selectedCustomerService.selectedCustomerSignal();

    if (currentCustomer && currentCustomer.type === 'ASSURE') {
      currentCustomer = null;
    }
    return {
      ...new Sales(),
      salesLines: [this.createSalesLine(produit, quantitySold)],
      customerId: currentCustomer?.id,
      natureVente: this.COMPTANT,
      typePrescription: this.typePrescription?.code,
      cassierId: this.userCaissier?.id,
      sellerId: this.userSeller?.id,
      type: 'VNO',
      categorie: 'VNO',
    };
  }

  private createSalesLine(produit: IProduit, quantityRequested: number): ISalesLine {
    return {
      ...new SalesLine(),
      produitId: produit.id,
      regularUnitPrice: produit.regularUnitPrice,
      saleId: this.sale?.id,
      quantitySold: quantityRequested,
      quantityRequested,
      sales: this.sale,
    };
  }

  private onPrintInvoice(): void {
    this.salesService.print(this.sale?.id).subscribe(blod => {
      const blobUrl = URL.createObjectURL(blod);
      window.open(blobUrl);
    });
  }

  private confirmForceStock(qytMvt: number, message: string): void {
    this.confirmationService.confirm({
      message,
      header: 'FORCER LE STOCK',
      icon: 'pi pi-info-circle',
      accept: () => {
        this.onAddProduit(qytMvt);
      },
      reject: () => {
        this.check = true;
        this.updateProduitQtyBox();
      },
      key: 'forcerStockDialog',
    });
    this.forcerStockDialogBtn().nativeElement.focus();
  }
}
