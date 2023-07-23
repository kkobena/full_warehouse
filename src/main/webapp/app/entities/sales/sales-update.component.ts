import { AfterViewInit, Component, ElementRef, Inject, OnInit, ViewChild } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
// eslint-disable-next-line @typescript-eslint/no-unused-vars
import { ActivatedRoute, Router } from '@angular/router';
import { Observable, Subscription } from 'rxjs';
import { ISales, Sales } from 'app/shared/model/sales.model';
import { SalesService } from './sales.service';
import { ICustomer } from 'app/shared/model/customer.model';
import { CustomerService } from 'app/entities/customer/customer.service';
import { IProduit } from 'app/shared/model/produit.model';
import { ProduitService } from '../produit/produit.service';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { AlertInfoComponent } from 'app/shared/alert/alert-info.component';
import { ISalesLine, SalesLine } from 'app/shared/model/sales-line.model';
import { saveAs } from 'file-saver';
import { INatureVente } from '../../shared/model/nature-vente.model';
import { ITypePrescription } from '../../shared/model/prescription-vente.model';
import { IUser, User } from '../../core/user/user.model';
import { IRemiseProduit } from '../../shared/model/remise-produit.model';
import { IPaymentMode, PaymentMode } from '../../shared/model/payment-mode.model';
import { IPayment, Payment } from '../../shared/model/payment.model';
import { UserService } from '../../core/user/user.service';
import { AccountService } from '../../core/auth/account.service';
import { ConfirmationService, PrimeNGConfig } from 'primeng/api';
import { ErrorService } from '../../shared/error.service';
import { ConfigurationService } from '../../shared/configuration.service';
import { DeconditionService } from '../decondition/decondition.service';
import { Decondition, IDecondition } from '../../shared/model/decondition.model';
import { IResponseDto } from '../../shared/util/response-dto';
import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';
import { UninsuredCustomerFormComponent } from '../customer/uninsured-customer-form/uninsured-customer-form.component';
import { UninsuredCustomerListComponent } from './uninsured-customer-list/uninsured-customer-list.component';
import { FormAssuredCustomerComponent } from '../customer/form-assured-customer/form-assured-customer.component';
import { TranslateService } from '@ngx-translate/core';
import { NgxSpinnerService } from 'ngx-spinner';
import { ClientTiersPayant, IClientTiersPayant } from '../../shared/model/client-tiers-payant.model';
import { AssuredCustomerListComponent } from './assured-customer-list/assured-customer-list.component';
import { AyantDroitCustomerListComponent } from './ayant-droit-customer-list/ayant-droit-customer-list.component';
import { FormAyantDroitComponent } from '../customer/form-ayant-droit/form-ayant-droit.component';
import { TiersPayantCustomerListComponent } from '../customer/tiers-payant-customer-list/tiers-payant-customer-list.component';
import { AssuranceService } from './assurance.service';

import { map } from 'rxjs/operators';
import { DOCUMENT } from '@angular/common';
import { ModePaymentService } from '../mode-payments/mode-payment.service';

type SelectableEntity = ICustomer | IProduit;

@Component({
  selector: 'jhi-sales-update',
  styles: [
    `
      .table tr:hover {
        cursor: pointer;
      }
    `,
  ],
  templateUrl: './sales-update.component.html',
  providers: [ConfirmationService, DialogService],
})
export class SalesUpdateComponent implements OnInit, AfterViewInit {
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
  showModeReglementCard = true;
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
  ayantDroit: ICustomer | null = null;
  selectedRowIndex?: number;
  remiseProduits: IRemiseProduit[] = [];
  remiseProduit?: IRemiseProduit | null;
  sale?: ISales | null = null;
  salesLines: ISalesLine[] = [];
  quantiteSaisie = 1;
  base64!: string;
  event: any;
  @ViewChild('clientSearchBox', { static: false })
  clientSearchBox?: ElementRef;
  @ViewChild('quantyBox', { static: true })
  quantyBox?: ElementRef;
  @ViewChild('produitbox', { static: true })
  produitbox?: any;
  @ViewChild('userBox', { static: true })
  userBox?: any;
  @ViewChild('removeOverlayPanel', { static: true })
  removeOverlayPanel?: any;
  @ViewChild('addOverlayPanel', { static: true })
  addOverlayPanel?: any;
  @ViewChild('forcerStockBtn', { static: false })
  forcerStockBtn?: ElementRef;
  @ViewChild('addModePaymentConfirmDialogBtn', { static: false })
  addModePaymentConfirmDialogBtn?: ElementRef;
  clientSearchValue?: string | null = null;
  clientBoxHeader = 'INFO CLIENT';
  stockSeverity = 'success';
  produitClass = 'col-6 row';
  rayonClass = 'col-2';
  entryAmount?: number | null = null;
  commentaire?: string;
  telephone?: string;
  referenceBancaire?: string;
  banque?: string;
  lieux?: string;
  qtyMaxToSel = 999999;
  derniereMonnaie = 0;
  monnaie = 0;
  readonly minLength = 1;
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
  readonly notFoundText = 'Aucun produit';
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
  tiersPayants: IClientTiersPayant[] = [];
  showOrHideTiersPayantBtn = false;
  tiersPayantsOriginal = 0;
  sansBon = false;
  isReadonly = false;
  canSaleWithoutSansBon = false;
  showAddModePaimentBtn = false;
  selectedMode: IPaymentMode | null;

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
    private modePaymentService: ModePaymentService,
    @Inject(DOCUMENT) private document: Document
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

  private static createPaymentMode(code: string): PaymentMode {
    return {
      ...new PaymentMode(),
      code,
    };
  }

  private static createDecondition(qtyDeconditione: number, produitId: number): IDecondition {
    return {
      ...new Decondition(),
      qtyMvt: qtyDeconditione,
      produitId,
    };
  }

  private static createTiersPayant(id: number, numBon: string, priorite: number): IClientTiersPayant {
    return {
      ...new ClientTiersPayant(),
      id,
      numBon,
      priorite,
    };
  }

  ngOnInit(): void {
    /*
    const logger$ = fromEvent<KeyboardEvent>(this.inpu
nativeElement, 'keyup');
logger$.subscribe(evt => this.keys += evt.key);
     */
    this.loadPaymentMode();
    this.loadAllUsers();
    this.maxToSale();
    this.saleWithoutSansBon();
    this.maxModePaymentNumber();
    this.accountService.identity().subscribe(account => {
      if (account) {
        this.userCaissier = account;
      }
    });

    this.resetNaturesVente();
    this.typePrescriptions = [
      { code: 'PRESCRIPTION', name: 'PRESCRIPTION' },
      {
        code: 'CONSEIL',
        name: 'CONSEIL',
      },
      { code: 'DEPOT', name: 'DEPÔT' },
    ];
    this.typePrescription = { code: 'PRESCRIPTION', name: 'PRESCRIPTION' };

    this.activatedRoute.data.subscribe(({ sales }) => {
      if (sales.id) {
        this.sale = sales;
        this.salesLines = sales.salesLines;
        this.customerSelected = sales.customer;
        this.naturesVente = this.naturesVentes.find(e => e.code === sales.natureVente) || null;
        this.typePrescription = this.typePrescriptions.find(e => e.code === sales.typePrescription) || null;
      }
      if (!this.showStock) {
        if (this.remiseProduits.length === 0) {
          this.produitClass = 'col-9 row';
          this.rayonClass = 'col-3';
        } else {
          this.produitClass = 'col-7 row';
        }
      } else {
        if (this.remiseProduits.length === 0) {
          this.produitClass = 'col-8 row';
          this.rayonClass = 'col-3';
        }
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

  getTiersPayants(): IClientTiersPayant[] {
    this.tiersPayantsOriginal = this.customerSelected?.tiersPayants?.length!;
    if (this.sale !== null && this.sale !== undefined) {
      this.tiersPayants = this.sale.tiersPayants!;
    } else {
      this.tiersPayants = this.customerSelected?.tiersPayants!;
    }

    return this.tiersPayants;
  }

  removeTiersPayant(tiersPayant: IClientTiersPayant): void {
    this.tiersPayants = this.tiersPayants.filter(e => e.id !== tiersPayant.id);
  }

  removeTiersPayantFromIndex(index: number, id: number): void {
    this.tiersPayants.splice(index, 1);
    this.showTiersPayantBtn();
    this.updateVenteTiersPayant(id);
  }

  loadAllUsers(): void {
    this.userService.query().subscribe((res: HttpResponse<User[]>) => (this.users = res.body || []));
  }

  manageAmountDiv(): void {
    const input = this.getInputAtIndex(0);

    if (input) {
      this.modeReglementSelected.find((e: IPaymentMode) => e.code === input.id).amount = this.sale?.amountToBePaid!;
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
        this.sale?.amountToBePaid - this.modeReglementSelected.find((e: IPaymentMode) => e.code !== input.id).amount;
      this.modeReglementSelected.find((e: IPaymentMode) => e.code === input.id).amount = secondInputDefaultAmount;

      setTimeout(() => {
        input.select();
      }, 50);
    }
  }

  manageShowInfosComplementaireReglementCard(): void {
    const mode = (element: IPaymentMode) => {
      return element.code === this.CB || element.code === this.VIREMENT || element.code === this.CH || this.isDiffere;
    };
    this.showInfosComplementaireReglementCard = this.modeReglementSelected.some(mode);
  }

  manageShowInfosBancaire(): void {
    const mode = (element: IPaymentMode) => {
      return element.code === this.CB || this.VIREMENT || element.code === this.CH;
    };
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
        this.errorEntryAmountBtn?.nativeElement.focus();
      },
      key: 'addModePaymentConfirmDialog',
    });
    this.addModePaymentConfirmDialogBtn?.nativeElement.focus();
  }

  save(): void {
    if (this.sale) {
      this.isSaving = true;
      this.sale.customerId = this.customerSelected?.id;
      this.sale.differe = this.isDiffere;
      if (this.isPresale === false) {
        const thatentryAmount = this.getEntryAmount();
        this.computeMonnaie(thatentryAmount);
        const restToPay = this.sale?.amountToBePaid - thatentryAmount;
        this.sale.montantRendu = this.monnaie;
        this.sale.montantVerse = this.getCashAmount();
        if (!this.isDiffere && thatentryAmount < this.sale.amountToBePaid) {
          this.addModeConfirmDialog();
        } else if (this.isDiffere && !this.sale.customerId) {
          this.displayErrorModal = true;
          this.clientSearchModalBtn?.nativeElement.focus();
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
    this.sale.sansBon = this.sansBon;
    this.sale.type = 'VO';
    this.sale.tiersPayants = this.buildTiersPayants();
    if (this.checkEmptyBon() && !this.canSaleWithoutSansBon) {
      this.commonDialog = true;
      this.commonDialogModalBtn?.nativeElement.focus();
    } else {
      this.subscribeToFinalyseResponse(this.assuranceService.save(this.sale));
    }
  }

  saveCashSale(): void {
    this.sale.type = 'VNO';
    this.subscribeToFinalyseResponse(this.salesService.saveComptant(this.sale));
  }

  onHideClientErrorDialog(event: Event): void {
    this.clientSearchBox?.nativeElement.focus();
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
    this.sale!.payments = this.buildPayment();
    this.sale!.type = 'VNO';
    this.subscribeToPutOnHoldResponse(this.salesService.putCurrentCashSaleOnHold(this.sale!));
  }

  putCurrentAssuranceSaleOnHold(): void {
    this.sale.payments = this.buildPayment();
    this.sale.type = 'VO';
    this.subscribeToPutOnHoldResponse(this.assuranceService.putCurrentSaleOnHold(this.sale!));
  }

  saveAntPrint(): void {
    this.isSaving = true;
    //  this.subscribeToFinalyseResponse(this.salesService.saveComptant(this.sale!));
  }

  searchUser(event: any): void {
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

    if (this.produitSelected?.totalQuantity > 0) {
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
      if (this.produitSelected?.totalQuantity > 0) {
        this.stockSeverity = 'success';
      } else {
        this.stockSeverity = 'danger';
      }
    } else if (event.key === 'Enter' && this.sale && this.salesLines.length > 0) {
      if ((this.naturesVente?.code === this.CARNET || this.naturesVente?.code === this.ASSURANCE) && this.sale.amountToBePaid === 0) {
        this.save();
      } else {
        this.manageAmountDiv();
      }
    }
  }

  changePaimentMode(newPaymentMode: IPaymentMode): void {
    const oldIndex = this.modeReglementSelected.findIndex((el: IPaymentMode) => (el.code = this.selectedMode?.code));
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

  showAyantDroit(): boolean {
    return !!(this.naturesVente?.code === this.ASSURANCE && this.ayantDroit);
  }

  showTiersPayant(): boolean {
    return !!((this.naturesVente?.code === this.ASSURANCE || this.naturesVente?.code === this.CARNET) && this.customerSelected);
  }

  showTiersPayantBtn(): void {
    this.showOrHideTiersPayantBtn = this.tiersPayants.length !== this.tiersPayantsOriginal;
  }

  showClientSearch(): boolean {
    return !!((this.isDiffere && this.sale) || this.naturesVente?.code !== this.COMPTANT);
  }

  onDiffereChange(): void {
    if (!this.customerSelected) {
      setTimeout(() => {
        this.clientSearchBox?.nativeElement.focus();
      }, 50);
    }

    this.manageShowInfosComplementaireReglementCard();
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
    const thatMonnaie = thatentryAmount - this.sale?.amountToBePaid!;
    this.monnaie = thatMonnaie > 0 ? thatMonnaie : 0;
  }

  getEntryAmount(): number {
    return this.modeReglementSelected.reduce((sum, current) => sum + Number(current.amount), 0);
  }

  showInfoCustomer(): boolean {
    return true;
  }

  loadProduits(): void {
    this.produitService
      .query({
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
      return this.salesLines.find(e => e.produitId === this.produitSelected!.id)?.quantityRequested || 0;
    }
    return 0;
  }

  onQuantityBoxAction(event: any): void {
    const qytMvt = Number(event.target.value);
    if (qytMvt <= 0) return;
    if (this.produitSelected !== null && this.produitSelected !== undefined) {
      const currentStock = this.produitSelected.totalQuantity;
      const qtyAlreadyRequested = this.totalItemQty();
      const inStock = currentStock! >= qytMvt + qtyAlreadyRequested;
      if (!inStock) {
        if (this.canForceStock && qytMvt > this.qtyMaxToSel) {
          this.confirmForceStock(qytMvt, ' La quantité saisie est supérieure à maximale à vendre. Voullez-vous continuer ?');
        } else if (this.canForceStock && qytMvt <= this.qtyMaxToSel) {
          if (this.produitSelected.produitId) {
            // s il  boite ch
            this.produitService.find(this.produitSelected.produitId).subscribe(res => {
              const prod = res.body;
              if (prod && prod.totalQuantity! > 0) {
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
    if (this.sale) {
      if (this.produitSelected) {
        if (this.naturesVente && this.naturesVente.code === this.COMPTANT) {
          this.subscribeToSaveLineResponse(this.salesService.addItemComptant(this.createSalesLine(this.produitSelected, qytMvt)));
        } else if (this.naturesVente && (this.naturesVente.code === this.CARNET || this.naturesVente.code === this.ASSURANCE)) {
          this.subscribeToSaveLineResponse(this.assuranceService.addItem(this.createSalesLine(this.produitSelected, qytMvt)));
        }
      }
    } else {
      if (this.produitSelected) {
        if (this.naturesVente && this.naturesVente.code === this.COMPTANT) {
          this.subscribeToCreateSaleComptantResponse(
            this.salesService.createComptant(this.createSaleComptant(this.produitSelected, qytMvt))
          );
        } else if (this.naturesVente && (this.naturesVente.code === this.CARNET || this.naturesVente.code === this.ASSURANCE)) {
          this.subscribeToCreateSaleComptantResponse(this.assuranceService.create(this.createThirdPartySale(this.produitSelected, qytMvt)));
        }
      }
    }
    this.computeMonnaie(null);
  }

  openUninsuredCustomerListTable(customers: ICustomer[] | []): void {
    this.ref = this.dialogService.open(UninsuredCustomerListComponent, {
      data: { customers },
      header: 'CLIENTS NON ASSURES',
      width: '80%',
      closeOnEscape: false,
    });
    this.ref.onClose.subscribe((resp: ICustomer) => {
      if (resp) {
        this.customerSelected = resp;
        this.produitbox.inputEL.nativeElement.focus();
      }
    });
  }

  openAssuredCustomerListTable(customers: ICustomer[] | []): void {
    this.ref = this.dialogService.open(AssuredCustomerListComponent, {
      data: { customers },
      header: 'CLIENTS  ASSURES',
      width: '95%',
      closeOnEscape: false,
    });
    this.ref.onClose.subscribe((resp: ICustomer) => {
      if (resp) {
        this.customerSelected = resp;
        setTimeout(() => {
          this.firstRefBonFocus();
        }, 100);
      }
    });
  }

  addNewCustomer(): void {
    if (this.naturesVente?.code === this.COMPTANT) {
      this.addUninsuredCustomer();
    } else {
      this.addAssuredCustomer();
    }
  }

  editCustomer(): void {
    if (this.naturesVente?.code === this.COMPTANT) {
      this.editUninsuredCustomer();
    } else {
      this.editAssuredCustomer();
    }
  }

  loadsCustomer(): void {
    if (this.naturesVente?.code === this.COMPTANT) {
      this.loadUninsuredCustomers();
    } else {
      this.loadAssuredCustomers();
    }
  }

  openCustomerListTable(customers: ICustomer[] | []): void {
    if (this.naturesVente?.code === this.COMPTANT) {
      this.openUninsuredCustomerListTable(customers);
    } else {
      this.openAssuredCustomerListTable(customers);
    }
  }

  addUninsuredCustomer(): void {
    this.ref = this.dialogService.open(UninsuredCustomerFormComponent, {
      data: { entity: null },
      header: "FORMULAIRE D'AJOUT DE NOUVEAU DE CLIENT ",
      width: '50%',
    });
    this.ref.onClose.subscribe((resp: ICustomer) => {
      if (resp) {
        this.customerSelected = resp;
        this.produitbox.inputEL.nativeElement.focus();
      }
    });
  }

  addAssuredCustomer(): void {
    this.ref = this.dialogService.open(FormAssuredCustomerComponent, {
      data: { entity: null },
      header: "FORMULAIRE D'AJOUT DE NOUVEAU DE CLIENT",
      width: '80%',
    });
    this.ref.onClose.subscribe((resp: ICustomer) => {
      if (resp) {
        this.customerSelected = resp;
      }
    });
  }

  editAssuredCustomer(): void {
    this.ref = this.dialogService.open(FormAssuredCustomerComponent, {
      data: { entity: this.customerSelected },
      header: 'FORMULAIRE DE MODIFICATION DE CLIENT ',
      width: '80%',
    });
    this.ref.onClose.subscribe((resp: ICustomer) => {
      if (resp) {
        this.customerSelected = resp;
        if (this.ayantDroit && this.ayantDroit.id === this.customerSelected.id) {
          this.ayantDroit = this.customerSelected;
        }
      }
    });
  }

  editUninsuredCustomer(): void {
    this.ref = this.dialogService.open(UninsuredCustomerFormComponent, {
      data: { entity: this.customerSelected },
      header: 'FORMULAIRE DE MODIFICATION DE CLIENT ',
      width: '50%',
    });
    this.ref.onClose.subscribe((resp: ICustomer) => {
      if (resp) {
        this.customerSelected = resp;
      }
    });
  }

  print(sale: ISales | null): void {
    if (sale !== null && sale !== undefined) {
      this.salesService.print(sale.id!).subscribe(blod => saveAs(blod));
      this.sale = null;
      this.loadProduits();
      this.customerSelected = null;
    }
  }

  printSale(): void {
    if (this.sale !== null && this.sale !== undefined) {
      this.salesService.printReceipt(this.sale.id!, this.sale.categorie!).subscribe();
      this.sale = null;
      this.loadProduits();
      this.customerSelected = null;
    }
  }

  removeLine(salesLine: ISalesLine): void {
    if (this.naturesVente?.code === this.COMPTANT) {
      this.removeItemFromVNO(salesLine.id!);
    } else if (this.naturesVente?.code === this.CARNET || this.naturesVente?.code === this.ASSURANCE) {
      this.removeItemFromCarnet(salesLine.id!);
    }
  }

  updateItemQtyRequested(salesLine: ISalesLine, event: any): void {
    const newQty = Number(event.target.value);
    if (newQty <= 0) return;
    salesLine.quantityRequested = newQty;
    if (newQty > this.qtyMaxToSel) {
      this.onUpdateConfirmForceStock(salesLine, ' La quantité saisie est supérieure à maximale à vendre. Voullez-vous continuer ?');
    } else {
      this.processQtyRequested(salesLine);
    }
  }

  updateItemQtySold(salesLine: ISalesLine, event: any): void {
    const newQty = Number(event.target.value);
    if (newQty < 0) return;
    if (newQty > salesLine.quantityRequested!) {
      this.openInfoDialog(
        `La quantité saisie  ${newQty}  ne doit pas être supérieure à la quantité demandée ${salesLine.quantityRequested}`,
        'alert alert-danger'
      );
      return;
    }
    salesLine.quantitySold = newQty;
    this.processQtySold(salesLine);
  }

  updateItemPrice(salesLine: ISalesLine, event: any): void {
    const newPrice = Number(event.target.value);
    if (newPrice <= 0) return;
    salesLine.regularUnitPrice = newPrice;
    this.processItemPrice(salesLine);
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

  confirmDeleteItem(item: ISalesLine): void {
    this.confirmationService.confirm({
      message: ' Voullez-vous detacher  ce produit ?',
      header: 'RUPPRESSION DE PRODUIT ',
      icon: 'pi pi-info-circle',
      accept: () => this.removeLine(item),
      reject: () => {
        this.check = true;
        this.updateProduitQtyBox();
      },
      key: 'deleteItem',
    });
  }

  confirmForceStock(qytMvt: number, message: string): void {
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
      key: 'forcerStock',
    });
    this.forcerStockBtn?.nativeElement.focus();
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
          this.decondtionService.create(SalesUpdateComponent.createDecondition(qtyDecondtionner, produit.id!)).subscribe(
            () => {
              if (item) {
                this.processQtyRequested(item);
              } else {
                this.onAddProduit(qytMvt);
              }
            },
            error => {
              if (error.error && error.error.status === 500) {
                this.openInfoDialog('Erreur applicative', 'alert alert-danger');
              } else {
                this.errorService.getErrorMessageTranslation(error.error.errorKey).subscribe(
                  translatedErrorMessage => {
                    this.openInfoDialog(translatedErrorMessage, 'alert alert-danger');
                  },
                  () => this.openInfoDialog(error.error.title, 'alert alert-danger')
                );
              }
            }
          );
        }
      },
      reject: () => {
        this.check = true;
        this.updateProduitQtyBox();
      },
      key: 'forcerStock',
    });
    this.forcerStockBtn?.nativeElement.focus();
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
      key: 'forcerStock',
    });
    this.forcerStockBtn?.nativeElement.focus();
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
      () => (this.maxModePayementNumber = 2)
    );
  }

  resetAll(): void {
    this.sale = null;
    this.customerSelected = null;
    this.ayantDroit = null;
    this.tiersPayants = [];
    this.salesLines = [];
    this.buildReglementInput();
    this.resetNaturesVente();
    this.typePrescription = { code: 'PRESCRIPTION', name: 'PRESCRIPTION' };
    this.userSeller = this.userCaissier;
    this.check = true;
    this.derniereMonnaie = this.monnaie;
    this.monnaie = 0;
    this.payments = [];
    this.sansBon = false;
    this.isDiffere = false;
    this.showAddModePaimentBtn = false;
    this.updateComponent();
    this.updateProduitQtyBox();
    this.loadProduits();
  }

  totalQtyProduit(): number {
    return this.salesLines.reduce((sum, current) => sum + current.quantityRequested!, 0);
  }

  totalQtyServi(): number {
    return this.salesLines.reduce((sum, current) => sum + current.quantitySold!, 0);
  }

  totalTtc(): number {
    return this.salesLines.reduce((sum, current) => sum + current.salesAmount!, 0);
  }

  buildPayment(): IPayment[] {
    return this.modeReglementSelected.filter((m: IPaymentMode) => m.amount).map((mode: IPaymentMode) => this.buildModePayment(mode));
  }

  buildPaymentFromSale(sale: ISales): void {
    sale.payments?.forEach(payment => {
      if (payment.paymentMode) {
        const code = payment.paymentMode.code;
      }
    });
  }

  loadUninsuredCustomers(): void {
    if (this.clientSearchValue) {
      this.spinner.show('salespinner');
      this.customerService.queryUninsuredCustomers({ search: this.clientSearchValue }).subscribe({
        next: (res: HttpResponse<ICustomer[]>) => {
          this.spinner.hide('salespinner');
          const uninsuredCustomers = res.body;
          if (uninsuredCustomers && uninsuredCustomers.length > 0) {
            if (uninsuredCustomers.length === 1) {
              this.customerSelected = uninsuredCustomers[0];
              this.produitbox.inputEL.nativeElement.focus();
            } else {
              this.openUninsuredCustomerListTable(uninsuredCustomers);
            }

            this.clientSearchValue = null;
          } else {
            this.addUninsuredCustomer();
            this.clientSearchValue = null;
          }
        },
        error: () => {
          this.spinner.hide('salespinner');
        },
      });
    }
  }

  loadAssuredCustomers(): void {
    if (this.clientSearchValue) {
      this.spinner.show('salespinner');
      this.customerService
        .queryAssuredCustomer({
          search: this.clientSearchValue,
          typeTiersPayant: this.naturesVente?.code,
        })
        .subscribe({
          next: (res: HttpResponse<ICustomer[]>) => {
            this.spinner.hide('salespinner');
            const assuredCustomers = res.body;
            if (assuredCustomers && assuredCustomers.length > 0) {
              if (assuredCustomers.length === 1) {
                this.customerSelected = assuredCustomers[0];
                setTimeout(() => {
                  this.firstRefBonFocus();
                }, 100);
              } else {
                this.openAssuredCustomerListTable(assuredCustomers);
              }

              this.clientSearchValue = null;
            } else {
              this.addAssuredCustomer();
              this.clientSearchValue = null;
            }
          },
          error: () => {
            this.spinner.hide('salespinner');
          },
        });
    }
  }

  setClientSearchBoxFocus(): void {
    setTimeout(() => {
      this.clientSearchBox?.nativeElement.focus();
    }, 50);
  }

  onNatureVenteChange(event: any): void {
    const selectNature = event.value;

    if (selectNature.code !== this.COMPTANT && this.sale && this.customerSelected) {
      const nature = (element: INatureVente) => {
        return element.code === this.COMPTANT;
      };
      this.naturesVentes.find(nature)!.disabled = true;

      //TODO si vente en cours et vente est de type VO, alors message si la nvelle est COMPTANT, ON ne peut transformer une VO en VNO
    }
    if (selectNature.code !== this.COMPTANT && !this.customerSelected) {
      this.setClientSearchBoxFocus();
    } else {
      this.produitbox.inputEL.nativeElement.focus();
    }
  }

  loadAyantDoits(): void {
    this.ref = this.dialogService.open(AyantDroitCustomerListComponent, {
      data: { assure: this.customerSelected },
      header: 'LISTE DES AYANT DROITS DU CLIENT',
      width: '80%',
      closeOnEscape: false,
    });
    this.ref.onClose.subscribe((resp: ICustomer) => {
      if (resp) {
        if (resp.id !== null && resp.id !== undefined) {
          this.ayantDroit = resp;
        } else {
          this.addAyantDroit();
        }
      }
    });
  }

  addAyantDroit(): void {
    this.ref = this.dialogService.open(FormAyantDroitComponent, {
      data: { entity: null, assure: this.customerSelected },
      header: "FORMULAIRE D'AJOUT D'AYANT DROIT ",
      width: '50%',
      closeOnEscape: false,
    });
    this.ref.onClose.subscribe((resp: ICustomer) => {
      if (resp) {
        this.ayantDroit = resp;
      }
    });
  }

  onRemoveAyantDroit(): void {
    this.ayantDroit = null;
  }

  onEditAyantDroit(): void {
    this.ref = this.dialogService.open(FormAyantDroitComponent, {
      data: { entity: this.ayantDroit, assure: this.customerSelected },
      header: 'FORMULAIRE DE MODIFICATION ',
      width: '50%',
      closeOnEscape: false,
    });
    this.ref.onClose.subscribe((resp: ICustomer) => {
      if (resp) {
        this.ayantDroit = resp;
      }
    });
  }

  firstRefBonFocus(): void {
    this.tierspayntDiv?.nativeElement.querySelector('input#tierspayant_0')?.focus();
  }

  lastRefBonFocus(): void {
    const refBonInputs = this.tierspayntDiv?.nativeElement.querySelectorAll('input[type=text]');
    refBonInputs[refBonInputs?.length - 1]?.focus();
  }

  addComplementaire(): void {
    this.ref = this.dialogService.open(TiersPayantCustomerListComponent, {
      data: { tiersPayants: this.tiersPayants, assure: this.customerSelected },
      header: 'TIERS-PAYANTS DU CLIENT',
      width: '70%',
      closeOnEscape: false,
    });
    this.ref.onClose.subscribe((resp: IClientTiersPayant) => {
      if (resp) {
        if (this.sale) {
          this.addTiersPayant(resp);
        }
        this.tiersPayants.push(resp);
        setTimeout(() => {
          this.lastRefBonFocus();
        }, 50);
      }
    });
  }

  manageCashPaymentMode(evt: any, modePay: IPaymentMode): void {
    if (this.modeReglementSelected.length === 2) {
      const secondInputDefaultAmount = this.sale?.amountToBePaid - modePay.amount; /* Number(evt.target.value)*/
      this.modeReglementSelected.find((e: IPaymentMode) => e.code !== evt.target.id).amount = secondInputDefaultAmount;
      const amount = this.getEntryAmount();

      this.computeMonnaie(amount);
    } else {
      this.computeMonnaie(null);
    }
    this.showAddModePaymentButton(modePay);
  }

  loadPaymentMode(): void {
    this.modePaymentService
      .query()
      .pipe(map((res: HttpResponse<IPaymentMode[]>) => this.convertPaymentMode(res)))
      .subscribe((res: HttpResponse<IPaymentMode[]>) => {
        this.modeReglements = res.body;
        this.cashModePayment = this.modeReglements.find(mode => mode.code === 'CASH');
        this.buildReglementInput();
      });
  }

  buildReglementInput(): void {
    if (this.sale && this.sale.payments?.length > 0) {
      this.modeReglements.forEach((mode: IPaymentMode) => {
        const el = this.sale.payments.find(payment => payment.paymentMode.code === mode.code);
        if (el) {
          el.paymentMode.amount = el.paidAmount;
          this.modeReglementSelected.push(el.paymentMode);
        }
      });
    } else {
      this.resetCashInput();
    }
    this.getReglements();
  }

  resetCashInput(): void {
    this.modeReglementSelected = [];
    this.modeReglementSelected[0] = this.cashModePayment;
    this.modeReglementSelected[0].amount = null;
  }

  getInputAtIndex(index: number | null): HTMLInputElement {
    const modeInputs = this.getInputs() as HTMLInputElement[];
    const indexAt = index === 0 ? index : modeInputs.length - 1;
    if (modeInputs && modeInputs?.length > 0) return modeInputs[indexAt];
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
      if (cashInput.code === this.CASH) return cashInput.amount;
      return 0;
    }
  }

  onAddPaymentModeToggle(old: IPaymentMode, evt: any): void {
    this.onModeBtnClick(old);
    this.addOverlayPanel.toggle(evt);
  }

  showAddModePaymentButton(mode: IPaymentMode): void {
    this.showAddModePaimentBtn = this.modeReglementSelected.length < this.maxModePayementNumber && mode.amount < this.sale?.amountToBePaid;
  }

  onRemovePaymentModeToggle(old: IPaymentMode, evt: any): void {
    if (this.modeReglementSelected.length === 1) {
      this.onModeBtnClick(old);
      this.removeOverlayPanel.toggle(evt);
    } else {
      const oldIndex = this.modeReglementSelected.findIndex((el: IPaymentMode) => el.code === old.code);
      this.modeReglementSelected.splice(oldIndex, 1);
      this.showAddModePaymentButton(this.modeReglementSelected[0]);
    }
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

  trackPaymentModeId(index: number, item: IPaymentMode): string {
    // eslint-disable-next-line @typescript-eslint/no-unnecessary-type-assertion
    return item.code!;
  }

  getReglements(): void {
    this.reglements = this.modeReglements.filter(x => !this.modeReglementSelected.includes(x));
  }

  protected processQtyRequested(salesLine: ISalesLine): void {
    if (this.naturesVente?.code === this.COMPTANT) {
      this.processQtyRequestedForVNO(salesLine);
    } else if (this.naturesVente?.code === this.CARNET || this.naturesVente?.code === this.ASSURANCE) {
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
        this.subscribeToSaveResponse(this.salesService.find(this.sale?.id));
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
        this.subscribeToSaveResponse(this.salesService.find(this.sale?.id!));
        this.onStockError(salesLine, error);
      },
    });
  }

  protected subscribeToSaveLineResponse(result: Observable<HttpResponse<ISalesLine>>): void {
    result.subscribe({
      next: (res: HttpResponse<ISalesLine>) => this.subscribeToSaveResponse(this.salesService.find(res.body?.saleId!)),
      error: () => this.onSaveError(),
    });
  }

  protected updateProduitQtyBox(): void {
    if (this.quantyBox) {
      this.quantyBox.nativeElement.value = 1;
    }
    if (this.check) {
      this.produitbox.inputEL.nativeElement.focus();
    } else {
      this.forcerStockBtn?.nativeElement.focus();
    }

    this.produitSelected = null;
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<ISales>>): void {
    result.subscribe({
      next: (res: HttpResponse<ISales>) => this.onSaveSuccess(res.body),
      error: () => this.onSaveError(),
    });
  }

  protected subscribeToUpdateItemPriceOrQuantityResponse(result: Observable<HttpResponse<ISales>>): void {
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
    this.subscribeToSaveResponse(this.salesService.find(this.sale?.id!));
  }

  protected subscribeToCreateSaleComptantResponse(result: Observable<HttpResponse<ISales>>): void {
    result.subscribe({
      next: (res: HttpResponse<ISales>) => this.onSaleComptantResponseSuccess(res.body),
      error: error => this.onCommonError(error),
    });
  }

  protected onSaleComptantResponseSuccess(sale: ISales | null): void {
    this.isSaving = false;
    this.sale = sale;
    if (sale && sale.salesLines) {
      this.salesLines = sale?.salesLines;
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
            'La quantité saisie est supérieure à la quantité stock du produit. Voullez-vous continuer ?'
          );
        } else {
          this.errorService.getErrorMessageTranslation(error.error.errorKey).subscribe(translatedErrorMessage => {
            this.openInfoDialog(translatedErrorMessage, 'alert alert-danger');
          });
        }
      } else if (error.error.errorKey === 'stockChInsufisant') {
        this.produitService.find(Number(error.error.title)).subscribe(res => {
          const prod = res.body;
          if (prod && prod.totalQuantity! > 0) {
            // si quantite CH
            this.confirmDeconditionnement(salesLine, prod, salesLine.quantityRequested!);
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
      this.errorService.getErrorMessageTranslation(error.error.errorKey).subscribe(
        translatedErrorMessage => {
          this.openInfoDialog(translatedErrorMessage, 'alert alert-danger');
        },
        () => this.openInfoDialog(error.error.title, 'alert alert-danger')
      );
    }
  }

  protected processQtySold(salesLine: ISalesLine): void {
    if (this.naturesVente?.code === this.COMPTANT) {
      this.updateVNOQuantitySold(salesLine);
    } else if (this.naturesVente?.code === this.CARNET || this.naturesVente?.code === this.ASSURANCE) {
      this.updateCarnetQuantitySold(salesLine);
    }
  }

  protected processItemPrice(salesLine: ISalesLine): void {
    if (this.naturesVente?.code === this.COMPTANT) {
      this.updateVNOItemPrice(salesLine);
    } else if (this.naturesVente?.code === this.CARNET || this.naturesVente?.code === this.ASSURANCE) {
      this.updateCarnetItemPrice(salesLine);
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

  private convertPaymentMode(res: HttpResponse<IPaymentMode[]>): HttpResponse<IPaymentMode[]> {
    if (res.body) {
      this.isReadonly = this.modeReglementSelected.length > 1;
      res.body.forEach((paymentMode: IPaymentMode) => {
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
      });
    }
    return res;
  }

  private addTiersPayant(resp: IClientTiersPayant): void {
    this.assuranceService.addThirdPartySaleLineToSales(resp, this.sale?.id!).subscribe(() => {
      this.subscribeToSaveResponse(this.assuranceService.find(this.sale?.id!));
    });
  }

  private buildModePayment(mode: IPaymentMode): Payment {
    console.warn(mode);
    this.entryAmount = this.getEntryAmount();
    const amount = this.sale?.amountToBePaid! - (this.entryAmount - mode.amount);
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
      //cassier: this.userCaissier!,
      //  seller: this.userSeller!,
      cassierId: this.userCaissier.id,
      sellerId: this.userSeller.id,
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
    this.salesService.print(this.sale?.id!).subscribe(blod => {
      const blobUrl = URL.createObjectURL(blod);
      window.open(blobUrl);
    });
  }

  private createThirdPartySale(produit: IProduit, quantitySold: number): ISales {
    return {
      ...new Sales(),
      salesLines: [this.createSalesLine(produit, quantitySold)],
      customerId: this.customerSelected.id,
      natureVente: this.naturesVente?.code,
      typePrescription: this.typePrescription?.code,
      cassierId: this.userCaissier.id,
      sellerId: this.userSeller.id,
      type: 'VO',
      categorie: 'VO',
      tiersPayants: this.buildTiersPayants(),
    };
  }

  private buildTiersPayants(): IClientTiersPayant[] {
    const inputs = this.tierspayntDiv?.nativeElement.querySelectorAll('.thirdPartySaleLines');
    const tiersPayantsData: IClientTiersPayant[] = [];
    inputs.forEach((e: any) => {
      const id = e.children[0].value;
      const numBon = e.children[2].children[0].children[1].value;
      const priorite = e.children[3].value;
      tiersPayantsData.push(SalesUpdateComponent.createTiersPayant(id, numBon, priorite));
    });
    return tiersPayantsData;
  }

  private disableComptant(): void {
    this.naturesVentes = [
      {
        code: this.COMPTANT,
        name: this.COMPTANT,
        disabled: true,
      } /*,
      { code: this.ASSURANCE, name: this.ASSURANCE, disabled: false },
      {
        code: this.CARNET,
        name: this.CARNET,
        disabled: false,
      },*/,
    ];
  }

  private updateVNOQuantitySold(salesLine: ISalesLine): void {
    this.salesService.updateItemQtySold(salesLine).subscribe({
      next: () => {
        if (this.sale) {
          this.subscribeToSaveResponse(this.salesService.find(this.sale.id));
        }
      },
      error: () => {
        this.onSaveError();
        this.subscribeToSaveResponse(this.salesService.find(this.sale?.id));
      },
    });
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
        this.subscribeToSaveResponse(this.salesService.find(this.sale?.id));
      },
    });
  }

  private updateVNOItemPrice(salesLine: ISalesLine): void {
    this.salesService.updateItemPrice(salesLine).subscribe({
      next: () => {
        if (this.sale) {
          this.subscribeToSaveResponse(this.salesService.find(this.sale.id));
        }
      },
      error: () => {
        this.onSaveError();
        this.subscribeToSaveResponse(this.salesService.find(this.sale?.id));
      },
    });
  }

  private updateCarnetItemPrice(salesLine: ISalesLine): void {
    this.assuranceService.updateItemPrice(salesLine).subscribe({
      next: () => {
        if (this.sale) {
          this.subscribeToSaveResponse(this.salesService.find(this.sale.id));
        }
      },
      error: () => {
        this.onSaveError();
        this.subscribeToSaveResponse(this.salesService.find(this.sale?.id));
      },
    });
  }

  private removeItemFromVNO(id: number): void {
    this.salesService.deleteItem(id).subscribe(() => {
      if (this.sale) {
        this.subscribeToSaveResponse(this.salesService.find(this.sale.id));
      }
    });
  }

  private removeItemFromCarnet(id: number): void {
    this.assuranceService.deleteItem(id).subscribe(() => {
      if (this.sale) {
        this.subscribeToSaveResponse(this.salesService.find(this.sale.id));
      }
    });
  }

  private updateVenteTiersPayant(id: number): void {
    if (this.sale) {
      if (this.naturesVente?.code === this.CARNET || this.naturesVente?.code === this.ASSURANCE) {
        this.assuranceService.removeVenteTiersPayant(id, this.sale.id!).subscribe(() => {
          if (this.sale) {
            this.subscribeToSaveResponse(this.salesService.find(this.sale.id));
          }
        });
      }
    }
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

  private updateSelectedModeReglement(mode: IPaymentMode): void {
    if (this.modeReglementSelected.length < this.maxModePayementNumber) {
      this.modeReglementSelected.push(mode);
    }
  }

  private checkEmptyBon(): boolean {
    if (this.naturesVente !== this.ASSURANCE) return false;
    if (!this.sansBon) return true;
    const emptyBon = (element: IClientTiersPayant) => {
      return element.numBon === undefined || element.numBon === null || element.numBon === '';
    };
    return !!this.sale?.tiersPayants.some(emptyBon);
  }
}
