import { Component, ElementRef, Inject, ViewChild } from '@angular/core';
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
import { NgxSpinnerModule, NgxSpinnerService } from 'ngx-spinner';
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
import { IPayment, Payment } from '../../../shared/model/payment.model';
import { ITypePrescription } from '../../../shared/model/prescription-vente.model';
import { ICustomer } from '../../../shared/model/customer.model';
import { IProduit } from '../../../shared/model/produit.model';
import { IRemiseProduit } from '../../../shared/model/remise-produit.model';
import { InputToFocus, ISales, Sales, SaveResponse } from '../../../shared/model/sales.model';
import { ISalesLine, SalesLine } from '../../../shared/model/sales-line.model';
import { PRODUIT_COMBO_MIN_LENGTH, PRODUIT_NOT_FOUND } from '../../../shared/constants/pagination.constants';
import { Observable, Subscription } from 'rxjs';
import { IClientTiersPayant } from '../../../shared/model/client-tiers-payant.model';
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
import { DOCUMENT } from '@angular/common';
import { Decondition, IDecondition } from '../../../shared/model/decondition.model';
import { HttpResponse } from '@angular/common/http';
import { saveAs } from 'file-saver';
import { AlertInfoComponent } from '../../../shared/alert/alert-info.component';
import { IResponseDto } from '../../../shared/util/response-dto';
import { ProductTableComponent } from './product-table/product-table.component';
import { CardModule } from 'primeng/card';
import { PresaleComponent } from '../presale/presale.component';
import { SalesComponent } from '../sales.component';
import { VenteEnCoursComponent } from '../vente-en-cours/vente-en-cours.component';
import { ComptantComponent } from './comptant/comptant.component';

type SelectableEntity = ICustomer | IProduit;

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
  ],
  templateUrl: './selling-home.component.html',
})
export class SellingHomeComponent {
  isSaving = false;
  isPresale = false;
  displayErrorModal = false;
  commonDialog = false;
  displayErrorEntryAmountModal = false;
  showStock = true;
  maxModePayementNumber = 2;
  canUpdatePu = true;
  isDiffere = false;
  printTicket = true;
  printInvoice = false;
  showInfosBancaire = false;
  canForceStock = true;
  showInfosComplementaireReglementCard = false;
  naturesVentes: INatureVente[] = [];
  naturesVente: INatureVente | null = null;
  users: IUser[];
  modeReglements: IPaymentMode[] = [];
  reglements: IPaymentMode[] = [];
  payments: IPayment[] = [];
  modeReglementSelected: IPaymentMode[] = [];
  cashModePayment: IPaymentMode | null;
  userCaissier?: IUser | null;
  userSeller?: IUser;
  typePrescriptions: ITypePrescription[] = [];
  typePrescription?: ITypePrescription | null;
  customers: ICustomer[] = [];
  produits: IProduit[] = [];
  produitSelected?: IProduit | null = null;
  searchValue?: string;
  appendTo = 'body';
  imagesPath!: string;
  customerSelected: ICustomer | null = null;
  selectedRowIndex?: number;
  remiseProduits: IRemiseProduit[] = [];
  sale?: ISales | null = null;
  salesLines: ISalesLine[] = [];
  quantiteSaisie = 1;
  base64!: string;
  event: any;
  @ViewChild('clientSearchBox')
  clientSearchBox?: ElementRef;
  @ViewChild('quantyBox')
  quantyBox?: ElementRef;
  @ViewChild('produitbox')
  produitbox?: any;
  @ViewChild('userBox')
  userBox?: any;
  @ViewChild('removeOverlayPanel')
  removeOverlayPanel?: any;
  @ViewChild('addOverlayPanel')
  addOverlayPanel?: any;
  @ViewChild('addModePaymentConfirmDialogBtn')
  addModePaymentConfirmDialogBtn?: ElementRef;
  stockSeverity = 'success';
  produitClass = 'col-6 row';
  rayonClass = 'col-2';
  entryAmount?: number | null = null;
  commentaire?: string;
  telephone?: string;
  qtyMaxToSel = 999999;
  derniereMonnaie = 0;
  monnaie = 0;
  readonly minLength = PRODUIT_COMBO_MIN_LENGTH;
  readonly CASH = 'CASH';
  readonly COMPTANT = 'COMPTANT';
  readonly CARNET = 'CARNET';
  readonly ASSURANCE = 'ASSURANCE';
  readonly OM = 'OM';
  readonly CB = 'CB';
  readonly CH = 'CH';
  readonly VIREMENT = 'VIREMENT';
  readonly WAVE = 'WAVE';
  readonly MOOV = 'MOOV';
  readonly MTN = 'MTN';
  check = true; // mis pour le focus produit et dialogue button
  readonly notFoundText = PRODUIT_NOT_FOUND;
  @ViewChild('clientSearchModalBtn', { static: false })
  clientSearchModalBtn?: ElementRef;
  @ViewChild('errorEntryAmountBtn', { static: false })
  errorEntryAmountBtn?: ElementRef;
  @ViewChild('commonDialogModalBtn', { static: false })
  commonDialogModalBtn?: ElementRef;
  @ViewChild('tierspayantDiv', { static: false })
  tierspayntDiv?: ElementRef;
  ref!: DynamicDialogRef;
  primngtranslate: Subscription;
  isReadonly = false;
  canSaleWithoutSansBon = false;
  showAddModePaimentBtn = false;
  selectedMode: IPaymentMode | null;
  pendingSalesSidebar = false;
  @ViewChild('forcerStockDialogBtn')
  forcerStockDialogBtn?: ElementRef;
  protected active = 'comptant';
  @ViewChild(ComptantComponent)
  private comptantComponent: ComptantComponent;

  constructor(
    protected salesService: SalesService,
    protected customerService: CustomerService,
    protected produitService: ProduitService,
    protected activatedRoute: ActivatedRoute,
    protected router: Router,
    protected modalService: NgbModal,
    protected userService: UserService,
    private accountService: AccountService,
    protected confirmationService: ConfirmationService,
    protected errorService: ErrorService,
    protected configurationService: ConfigurationService,
    protected decondtionService: DeconditionService,
    private dialogService: DialogService,
    public translate: TranslateService,
    public primeNGConfig: PrimeNGConfig,
    private spinner: NgxSpinnerService,
    private assuranceService: AssuranceService,
    @Inject(DOCUMENT) private document: Document,
  ) {
    this.imagesPath = 'data:image/';
    this.base64 = ';base64,';
    this.selectedRowIndex = 0;
    this.searchValue = '';
    this.translate.use('fr');
    this.primngtranslate = this.translate.stream('primeng').subscribe(data => {
      this.primeNGConfig.setTranslation(data);
    });
  }

  onLoadPrevente(sales: ISales): void {
    this.modeReglementSelected = [];
    this.sale = sales;
    this.salesLines = sales.salesLines;
    this.customerSelected = sales.customer;
    this.naturesVente = this.naturesVentes.find(e => e.code === sales.natureVente) || null;
    this.typePrescription = this.typePrescriptions.find(e => e.code === sales.typePrescription) || null;
    this.userSeller = this.users.find(e => e.id === sales.sellerId) || this.userSeller;
  }

  ngOnInit(): void {
    this.loadAllUsers();
    this.maxToSale();
    this.accountService.identity().subscribe(account => {
      if (account) {
        this.userCaissier = account;
      }
    });

    this.resetNaturesVente();
    this.typePrescriptions = [
      { code: 'PRESCRIPTION', name: 'Prescription' },
      {
        code: 'CONSEIL',
        name: 'Conseil',
      },
      { code: 'DEPOT', name: 'Dépôt' },
    ];
    this.typePrescription = { code: 'PRESCRIPTION', name: 'Prescription' };

    this.activatedRoute.data.subscribe(({ sales }) => {
      if (sales.id) {
        this.onLoadPrevente(sales);
      }
      if (!this.showStock) {
        if (this.remiseProduits.length === 0) {
          this.produitClass = 'col-9 row';
          this.rayonClass = 'col-3';
        } else {
          this.produitClass = 'col-7 row';
        }
      } else if (this.remiseProduits.length === 0) {
        this.produitClass = 'col-8 row';
        this.rayonClass = 'col-3';
      }

      this.loadProduits();
    });
    this.activatedRoute.paramMap.subscribe(params => {
      console.warn(params);
      if (params.has('isPresale')) {
        this.isPresale = params.get('isPresale') === 'true';
      }
    });
  }

  ngAfterViewInit(): void {
    if (this.userBox) {
      if (!this.userSeller) {
        this.userSeller = this.userCaissier;
      }
    }
  }

  loadAllUsers(): void {
    this.userService.query().subscribe((res: HttpResponse<User[]>) => (this.users = res.body || []));
  }

  manageAmountDiv(): void {
    const input = this.getInputAtIndex(0);

    if (input) {
      this.modeReglementSelected.find((e: IPaymentMode) => e.code === input.id).amount = this.sale.amountToBePaid;
      input.focus();
      setTimeout(() => {
        input.select();
      }, 50);
    }
  }

  focusLastAddInput(): void {
    const input = this.getInputAtIndex(null);
    if (input) {
      input.focus();
      const secondInputDefaultAmount =
        this.sale.amountToBePaid - this.modeReglementSelected.find((e: IPaymentMode) => e.code !== input.id).amount;
      this.modeReglementSelected.find((e: IPaymentMode) => e.code === input.id).amount = secondInputDefaultAmount;

      setTimeout(() => {
        input.select();
      }, 50);
    }
  }

  manageShowInfosComplementaireReglementCard(): void {
    const mode = (element: IPaymentMode) =>
      element.code === this.CB || element.code === this.VIREMENT || element.code === this.CH || this.isDiffere;
    this.showInfosComplementaireReglementCard = this.modeReglementSelected.some(mode);
  }

  manageShowInfosBancaire(): void {
    const mode = (element: IPaymentMode) => element.code === this.CB || this.VIREMENT || element.code === this.CH;
    this.showInfosBancaire = this.modeReglementSelected.some(mode);
  }

  previousState(): void {
    window.history.back();
  }

  onHidedisplayErrorEntryAmountModal(event: Event): void {
    // this.montantCashInput?.nativeElement.focus();
  }

  addModeConfirmDialog(): void {
    this.confirmationService.confirm({
      message: 'Voullez-vous ajouter un autre moyen  de payment',
      header: "AJOUT D'UN AUTRE MOYEN DE PAYMENT",
      icon: 'pi pi-info-circle',
      accept: () => {
        this.addOverlayPanel.toggle(this.getAddModePaymentButton());
      },
      reject: () => {
        this.displayErrorEntryAmountModal = true;
        this.errorEntryAmountBtn.nativeElement.focus();
      },
      key: 'addModePaymentConfirmDialog',
    });
    this.addModePaymentConfirmDialogBtn.nativeElement.focus();
  }

  save(): void {
    if (this.sale) {
      this.isSaving = true;
      this.sale.customerId = this.customerSelected?.id;
      this.sale.differe = this.isDiffere;
      if (this.isPresale === false) {
        const thatentryAmount = this.getEntryAmount();
        this.computeMonnaie(thatentryAmount);
        const restToPay = this.sale.amountToBePaid - thatentryAmount;
        this.sale.montantRendu = this.monnaie;
        this.sale.montantVerse = this.getCashAmount();
        if (!this.isDiffere && thatentryAmount < this.sale.amountToBePaid) {
          this.addModeConfirmDialog();
        } else if (this.isDiffere && !this.sale.customerId) {
          this.displayErrorModal = true;
          this.clientSearchModalBtn.nativeElement.focus();
        } else {
          if (restToPay <= 0) {
            this.sale.payrollAmount = this.sale.amountToBePaid;
            this.sale.restToPay = 0;
          } else {
            this.sale.payrollAmount = thatentryAmount;
            this.sale.restToPay = restToPay;
          }
          this.sale.payments = this.buildPayment();
          if (this.naturesVente && this.naturesVente.code === this.COMPTANT) {
            this.saveCashSale();
          } else {
            this.saveAssuranceSale();
          }
        }
      } else if (this.isPresale === true) {
        this.putCurrentSaleOnHold();
      }
    }
  }

  saveAssuranceSale(): void {
    // this.sale.sansBon = this.sansBon;
    this.sale.type = 'VO';
    //  this.sale.tiersPayants = this.buildTiersPayants();
    /* if (this.checkEmptyBon() && !this.canSaleWithoutSansBon) {
       this.commonDialog = true;
       this.commonDialogModalBtn.nativeElement.focus();
     } else {
       this.subscribeToFinalyseResponse(this.assuranceService.save(this.sale));
     }*/
  }

  saveCashSale(): void {
    this.sale.type = 'VNO';
    this.subscribeToFinalyseResponse(this.salesService.saveComptant(this.sale));
  }

  onHideClientErrorDialog(event: Event): void {
    this.clientSearchBox.nativeElement.focus();
  }

  onHideHideDialog(): void {}

  cancelErrorModal(): void {
    this.displayErrorModal = false;
  }

  cancelCommonDialog(): void {
    this.commonDialog = false;
  }

  canceldisplayErrorEntryAmountModal(): void {
    this.displayErrorEntryAmountModal = false;
  }

  putCurrentSaleOnHold(): void {
    if (this.naturesVente && this.naturesVente.code === this.COMPTANT) {
      this.putCurrentCashSaleOnHold();
    } else if (this.naturesVente && (this.naturesVente.code === this.CARNET || this.naturesVente.code === this.ASSURANCE)) {
      this.putCurrentAssuranceSaleOnHold();
    }
  }

  putCurrentCashSaleOnHold(): void {
    this.sale.payments = this.buildPayment();
    this.sale.type = 'VNO';
    this.subscribeToPutOnHoldResponse(this.salesService.putCurrentCashSaleOnHold(this.sale));
  }

  putCurrentAssuranceSaleOnHold(): void {
    this.sale.payments = this.buildPayment();
    this.sale.type = 'VO';
    this.subscribeToPutOnHoldResponse(this.assuranceService.putCurrentSaleOnHold(this.sale));
  }

  saveAntPrint(): void {
    this.isSaving = true;
    //  this.subscribeToFinalyseResponse(this.salesService.saveComptant(this.sale!));
  }

  searchUser(): void {
    this.loadAllUsers();
  }

  onSelectUser(): void {
    setTimeout(() => {
      this.produitbox.inputEL.nativeElement.focus();
      this.produitbox.inputEL.nativeElement.select();
    }, 50);
  }

  searchFn(event: any): void {
    /*  const key = event.key;
      if (
        key !== 'ArrowDown' &&
        key !== 'ArrowUp' &&
        key !== 'ArrowRight' &&
        key !== 'ArrowLeft' &&
        key !== 'NumLock' &&
        key !== 'CapsLock' &&
        key !== 'Control' &&
        key !== 'PageUp' &&
        key !== 'PageDown' &&
        key !== 'Backspace'
      ) */

    {
      this.searchValue = event.query;
      this.loadProduits();
    }
  }

  produitComponentSearch(term: string, item: IProduit): boolean {
    return !!item;
  }

  onSelect(): void {
    setTimeout(() => {
      this.quantyBox.nativeElement.focus();
      this.quantyBox.nativeElement.select();
    }, 50);

    if (this.produitSelected.totalQuantity > 0) {
      this.stockSeverity = 'success';
    } else {
      this.stockSeverity = 'danger';
    }
  }

  onSelectKeyDow(event: KeyboardEvent): void {
    if (event.key === 'Enter' && this.produitSelected) {
      if (this.quantyBox) {
        const el = this.quantyBox.nativeElement;
        el.focus();
        el.select();
      }
      if (this.produitSelected.totalQuantity > 0) {
        this.stockSeverity = 'success';
      } else {
        this.stockSeverity = 'danger';
      }
    } else if (event.key === 'Enter' && this.sale && this.salesLines.length > 0) {
      if ((this.naturesVente.code === this.CARNET || this.naturesVente.code === this.ASSURANCE) && this.sale.amountToBePaid === 0) {
        this.save();
      } else {
        this.manageAmountDiv();
      }
    }
  }

  changePaimentMode(newPaymentMode: IPaymentMode): void {
    const oldIndex = this.modeReglementSelected.findIndex((el: IPaymentMode) => (el.code = this.selectedMode.code));
    this.modeReglementSelected[oldIndex] = newPaymentMode;
    this.getReglements();
    this.updateComponent();
    setTimeout(() => {
      this.manageAmountDiv();
    }, 50);
  }

  updateComponent(): void {
    this.manageShowInfosComplementaireReglementCard();
    this.manageShowInfosBancaire();
  }

  trackId(index: number, item: IProduit): number {
    // eslint-disable-next-line @typescript-eslint/no-unnecessary-type-assertion
    return item.id!;
  }

  trackById(index: number, item: SelectableEntity): any {
    return item.id;
  }

  computeMonnaie(amount: number | null): void {
    const thatentryAmount = amount || this.getEntryAmount();
    const thatMonnaie = thatentryAmount - this.sale?.amountToBePaid;
    this.monnaie = thatMonnaie > 0 ? thatMonnaie : 0;
  }

  getEntryAmount(): number {
    return this.modeReglementSelected.reduce((sum, current) => sum + Number(current.amount), 0);
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

  clickRow(item: IProduit): void {
    this.selectedRowIndex = item.id;
  }

  totalItemQty(): number {
    if (this.produitSelected) {
      return this.salesLines.find(e => e.produitId === this.produitSelected.id)?.quantityRequested || 0;
    }
    return 0;
  }

  onQuantityBoxAction(event: any): void {
    const qytMvt = Number(event.target.value);
    if (qytMvt <= 0) {
      return;
    }
    if (this.produitSelected !== null && this.produitSelected !== undefined) {
      const currentStock = this.produitSelected.totalQuantity;
      const qtyAlreadyRequested = this.totalItemQty();
      const inStock = currentStock >= qytMvt + qtyAlreadyRequested;
      if (!inStock) {
        if (this.canForceStock && qytMvt > this.qtyMaxToSel) {
          this.confirmForceStock(qytMvt, ' La quantité saisie est supérieure à maximale à vendre. Voullez-vous continuer ?');
        } else if (this.canForceStock && qytMvt <= this.qtyMaxToSel) {
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
        if (qytMvt >= this.qtyMaxToSel) {
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

  onAddProduit(qytMvt: number): void {
    if (this.produitSelected) {
      if (this.naturesVente && this.naturesVente.code === this.COMPTANT) {
        if (this.sale) {
          this.comptantComponent.onAddProduit(this.createSalesLine(this.produitSelected, qytMvt));
        } else {
          this.comptantComponent.createComptant(this.createSaleComptant(this.produitSelected, qytMvt));
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
      this.customerSelected = null;
    }
  }

  printSale(): void {
    if (this.sale !== null && this.sale !== undefined) {
      this.salesService.printReceipt(this.sale?.id, this.sale.categorie).subscribe();
      this.sale = null;
      this.loadProduits();
      this.customerSelected = null;
    }
  }

  formatNumber(number: any): string {
    return Math.floor(number.value)
      .toString()
      .replace(/(\d)(?=(\d{3})+(?!\d))/g, '$1 ');
  }

  openInfoDialog(message: string, infoClass: string): void {
    const modalRef = this.modalService.open(AlertInfoComponent, {
      backdrop: 'static',
      centered: true,
    });
    modalRef.componentInstance.message = message;
    modalRef.componentInstance.infoClass = infoClass;
  }

  onCellValueChanged(params: any): void {
    if (Number(params.data.quantitySold) > params.data.quantityStock) {
      this.openInfoDialog('La quantité saisie est supérieure à la quantité stock du produit', 'alert alert-danger');
    } else {
      // this.subscribeToSaveLineResponse(this.saleItemService.update(this.createSalesLine(params.data)));
    }
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
    this.forcerStockDialogBtn.nativeElement.focus();
  }

  maxToSale(): void {
    this.configurationService.find('APP_QTY_MAX').subscribe(res => {
      if (res.body) {
        this.qtyMaxToSel = Number(res.body.value);
      }
    });
  }

  saleWithoutSansBon(): void {
    this.configurationService.find('APP_SANS_NUM_BON').subscribe(res => {
      if (res.body) {
        this.canSaleWithoutSansBon = Number(res.body.value) === 1;
      }
    });
  }

  maxModePaymentNumber(): void {
    this.configurationService.find('APP_MODE_REGL_NUMBER').subscribe(
      res => {
        if (res.body) {
          this.maxModePayementNumber = Number(res.body.value);
        }
      },
      () => (this.maxModePayementNumber = 2),
    );
  }

  resetAll(): void {
    this.sale = null;
    this.salesLines = [];
    this.resetNaturesVente();
    this.typePrescription = { code: 'PRESCRIPTION', name: 'PRESCRIPTION' };
    this.userSeller = this.userCaissier;
    this.check = true;
    this.derniereMonnaie = this.monnaie;
    this.monnaie = 0;
    this.payments = [];
    this.isDiffere = false;
    this.showAddModePaimentBtn = false;
    this.updateComponent();
    this.updateProduitQtyBox();
    this.loadProduits();
  }

  buildPayment(): IPayment[] {
    return this.modeReglementSelected.filter((m: IPaymentMode) => m.amount).map((mode: IPaymentMode) => this.buildModePayment(mode));
  }

  buildPaymentFromSale(sale: ISales): void {
    sale.payments.forEach(payment => {
      if (payment.paymentMode) {
        const code = payment.paymentMode.code;
      }
    });
  }

  onNatureVenteChange(event: any): void {
    const selectNature = event.value;

    if (selectNature.code !== this.COMPTANT && this.sale && this.customerSelected) {
      const nature = (element: INatureVente) => element.code === this.COMPTANT;
      this.naturesVentes.find(nature)!.disabled = true;

      // TODO si vente en cours et vente est de type VO, alors message si la nvelle est COMPTANT, ON ne peut transformer une VO en VNO
    }
    if (selectNature.code !== this.COMPTANT && !this.customerSelected) {
      // this.setClientSearchBoxFocus();
    } else {
      this.produitbox.inputEL.nativeElement.focus();
    }
  }

  firstRefBonFocus(): void {
    this.tierspayntDiv.nativeElement.querySelector('input#tierspayant_0')?.focus();
  }

  getInputAtIndex(index: number | null): HTMLInputElement {
    const modeInputs = this.getInputs() as HTMLInputElement[];
    const indexAt = index === 0 ? index : modeInputs.length - 1;
    if (modeInputs && modeInputs.length > 0) {
      return modeInputs[indexAt];
    }
    return null;
  }

  getCashAmount(): number {
    let cashInput;
    this.entryAmount = this.getEntryAmount();
    if (this.modeReglementSelected.length > 0) {
      cashInput = this.modeReglementSelected.find((input: IPaymentMode) => input.code === this.CASH);
      if (cashInput) {
        return cashInput.amount;
      }
      return 0;
    } else {
      cashInput = this.modeReglementSelected[0];
      if (cashInput.code === this.CASH) {
        return cashInput.amount;
      }
      return 0;
    }
  }

  showAddModePaymentButton(mode: IPaymentMode): void {
    this.showAddModePaimentBtn = this.modeReglementSelected?.length < this.maxModePayementNumber && mode.amount < this.sale?.amountToBePaid;
  }

  onRemovePaymentMode(newMode: IPaymentMode): void {
    this.changePaimentMode(newMode);
    this.removeOverlayPanel.hide();
  }

  onAddPaymentMode(newMode: IPaymentMode): void {
    if (this.modeReglementSelected.length < this.maxModePayementNumber) {
      this.modeReglementSelected[this.modeReglementSelected.length++] = newMode;
      this.addOverlayPanel.hide();
      this.getReglements();
      this.updateComponent();
      setTimeout(() => {
        this.focusLastAddInput();
      }, 50);
    }
  }

  onModeBtnClick(paymentMode: IPaymentMode): void {
    this.selectedMode = paymentMode;
  }

  getReglements(): void {
    this.reglements = this.modeReglements.filter(x => !this.modeReglementSelected.includes(x));
  }

  openPindingSide(): void {
    this.pendingSalesSidebar = true;
  }

  closeSideBar(booleanValue: boolean): void {
    this.pendingSalesSidebar = booleanValue;
  }

  onSelectPrevente(prevente: ISales): void {
    this.subscribeOnloadPreventeResponse(this.salesService.find(prevente.id));
  }

  onSave(saveResponse: SaveResponse): void {
    this.sale = saveResponse.sale;
    if (saveResponse.success) {
      this.computeMonnaie(null);
      this.updateProduitQtyBox();
    }
  }

  getControlToFocus(inputToFocusEvent: InputToFocus): void {
    if (inputToFocusEvent.control === 'produitBox') {
      this.updateProduitQtyBox();
    }
  }

  protected processQtyRequested(salesLine: ISalesLine): void {
    if (this.naturesVente.code === this.COMPTANT) {
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
    if (this.quantyBox) {
      this.quantyBox.nativeElement.value = 1;
    }
    if (this.check) {
      this.produitbox.inputEL.nativeElement.focus();
    } else {
      this.forcerStockDialogBtn.nativeElement.focus();
    }

    this.produitSelected = null;
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<ISales>>): void {
    result.subscribe({
      next: (res: HttpResponse<ISales>) => this.onSaveSuccess(res.body),
      error: () => this.onSaveError(),
    });
  }

  protected subscribeOnloadPreventeResponse(result: Observable<HttpResponse<ISales>>): void {
    result.subscribe({
      next: (res: HttpResponse<ISales>) => this.onLoadPrevente(res.body),
      error: () => this.onSaveError(),
    });
  }

  protected onProduitSuccess(data: IProduit[] | null): void {
    this.produits = data || [];
  }

  protected onSaveSuccess(sale: ISales | null): void {
    this.isSaving = false;
    this.sale = sale!;
    this.salesLines = this.sale.salesLines!;
    this.computeMonnaie(null);
    this.updateProduitQtyBox();
  }

  protected onSaveError(): void {
    this.isSaving = false;
    const message = 'Une erreur est survenue';
    this.openInfoDialog(message, 'alert alert-danger');
  }

  protected onFinalyseSuccess(response: IResponseDto | null): void {
    this.isSaving = false;
    if (this.printTicket) {
      this.printSale();
    }
    if (this.printInvoice) {
      this.onPrintInvoice();
    }
    this.resetAll();
  }

  protected subscribeToFinalyseResponse(result: Observable<HttpResponse<IResponseDto>>): void {
    result.subscribe({
      next: (res: HttpResponse<IResponseDto>) => this.onFinalyseSuccess(res.body),
      error: () => this.onSaveError(),
    });
  }

  protected subscribeToPutOnHoldResponse(result: Observable<HttpResponse<IResponseDto>>): void {
    result.subscribe({
      next: () => {
        this.isSaving = false;
        this.resetAll();
      },
      error: () => this.onSaveError(),
    });
  }

  protected refresh(): void {
    this.subscribeToSaveResponse(this.salesService.find(this.sale?.id));
  }

  protected onSaleComptantResponseSuccess(sale: ISales | null): void {
    this.isSaving = false;
    this.sale = sale;
    if (sale && sale.salesLines) {
      this.salesLines = sale.salesLines;
      if (sale.type === 'VO' || sale.categorie === 'VO') {
        this.disableComptant();
      }

      this.updateProduitQtyBox();
    }
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

  private createDecondition(qtyDeconditione: number, produitId: number): IDecondition {
    return {
      ...new Decondition(),
      qtyMvt: qtyDeconditione,
      produitId,
    };
  }

  private setFirstInputFocused(): void {
    const input = this.getInputAtIndex(0);
    if (input) {
      input.focus();
      setTimeout(() => {
        input.select();
      }, 50);
    }
  }

  private getInputs(): Element[] {
    const inputs = this.document.querySelectorAll('.payment-mode-input');
    return Array.from(inputs);
  }

  private getAddModePaymentButton(): HTMLInputElement {
    const inputs = this.document.querySelectorAll('.add-mode-payment-btn')[0] as HTMLInputElement;
    return inputs;
  }

  private convertmodeReglement(paymentMode: IPaymentMode): IPaymentMode {
    paymentMode.disabled = false;
    switch (paymentMode.code) {
      case 'CASH':
        paymentMode.styleImageClass = 'cash';
        paymentMode.styleBtnClass = 'cash-btn';
        break;
      case 'WAVE':
        paymentMode.styleImageClass = 'wave';
        paymentMode.styleBtnClass = 'wave-btn';
        paymentMode.isReadonly = this.isReadonly;
        break;
      case 'OM':
        paymentMode.styleImageClass = 'om';
        paymentMode.styleBtnClass = 'om-btn';
        paymentMode.isReadonly = true;
        break;
      case 'CB':
        paymentMode.styleImageClass = 'cb';
        paymentMode.styleBtnClass = 'cb-btn';
        paymentMode.isReadonly = this.isReadonly;
        break;
      case 'MOOV':
        paymentMode.styleImageClass = 'moov';
        paymentMode.styleBtnClass = 'moov-btn';
        paymentMode.isReadonly = this.isReadonly;
        break;
      case 'MTN':
        paymentMode.styleImageClass = 'mtn';
        paymentMode.styleBtnClass = 'mtn-btn';
        paymentMode.isReadonly = this.isReadonly;
        break;
      case 'CH':
        paymentMode.styleImageClass = 'cheque';
        paymentMode.styleBtnClass = 'cheque-btn';
        paymentMode.isReadonly = true;
        break;
      case 'VIREMENT':
        paymentMode.styleImageClass = 'virement';
        paymentMode.styleBtnClass = 'virement-btn';
        paymentMode.isReadonly = this.isReadonly;
        break;
      default:
        break;
    }
    return paymentMode;
  }

  private convertPaymentMode(res: HttpResponse<IPaymentMode[]>): IPaymentMode[] {
    this.isReadonly = this.modeReglementSelected.length > 1;
    return res.body.map((paymentMode: IPaymentMode) => this.convertmodeReglement(paymentMode));
  }

  private addTiersPayant(resp: IClientTiersPayant): void {
    this.assuranceService.addThirdPartySaleLineToSales(resp, this.sale?.id).subscribe(() => {
      this.subscribeToSaveResponse(this.assuranceService.find(this.sale?.id));
    });
  }

  private buildModePayment(mode: IPaymentMode): Payment {
    console.warn(mode);
    this.entryAmount = this.getEntryAmount();
    const amount = this.sale.amountToBePaid - (this.entryAmount - mode.amount);
    return {
      ...new Payment(),
      paidAmount: amount,
      netAmount: amount,
      paymentMode: mode,
      montantVerse: mode.amount,
    };
  }

  private createSaleComptant(produit: IProduit, quantitySold: number): ISales {
    return {
      ...new Sales(),
      salesLines: [this.createSalesLine(produit, quantitySold)],
      customerId: this.customerSelected?.id,
      natureVente: this.naturesVente?.code,
      typePrescription: this.typePrescription?.code,
      // cassier: this.userCaissier!,
      //  seller: this.userSeller!,
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

  private disableComptant(): void {
    this.naturesVentes = [
      {
        code: this.COMPTANT,
        name: this.COMPTANT,
        disabled: true,
      } /* ,
      { code: this.ASSURANCE, name: this.ASSURANCE, disabled: false },
      {
        code: this.CARNET,
        name: this.CARNET,
        disabled: false,
      },*/,
    ];
  }

  private updateCarnetQuantitySold(salesLine: ISalesLine): void {
    this.assuranceService.updateItemQtySold(salesLine).subscribe({
      next: () => {
        if (this.sale) {
          this.subscribeToSaveResponse(this.salesService.find(this.sale.id));
        }
      },
      error: () => {
        this.onSaveError();
        this.subscribeToSaveResponse(this.salesService.find(this.sale.id));
      },
    });
  }

  private resetNaturesVente(): void {
    this.naturesVentes = [
      {
        code: this.COMPTANT,
        name: this.COMPTANT,
        disabled: false,
      },
      /* { code: this.ASSURANCE, name: this.ASSURANCE, disabled: false },
       {
         code: this.CARNET,
         name: this.CARNET,
         disabled: false,
       },*/
    ];
    this.naturesVente = { code: this.COMPTANT, name: this.COMPTANT, disabled: false };
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
    this.forcerStockDialogBtn.nativeElement.focus();
  }
}
