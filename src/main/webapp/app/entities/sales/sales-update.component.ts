import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
// eslint-disable-next-line @typescript-eslint/no-unused-vars
import { ActivatedRoute } from '@angular/router';
import { Observable, Subject, Subscription } from 'rxjs';
import { ISales, Sales } from 'app/shared/model/sales.model';
import { SalesService } from './sales.service';
import { Customer, ICustomer } from 'app/shared/model/customer.model';
import { CustomerService } from 'app/entities/customer/customer.service';
import { IProduit } from 'app/shared/model/produit.model';
import { ProduitService } from '../produit/produit.service';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { AlertInfoComponent } from 'app/shared/alert/alert-info.component';
import { SalesLineService } from '../sales-line/sales-line.service';
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
import { IClientTiersPayant } from '../../shared/model/client-tiers-payant.model';
import { AssuredCustomerListComponent } from './assured-customer-list/assured-customer-list.component';
import { AyantDroitCustomerListComponent } from './ayant-droit-customer-list/ayant-droit-customer-list.component';
import { FormAyantDroitComponent } from '../customer/form-ayant-droit/form-ayant-droit.component';

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
export class SalesUpdateComponent implements OnInit {
  isSaving = false;
  isPresale = false;
  showTiersPayantCard = false;
  showClientSearchCard = false;
  displayErrorModal = false;
  displayErrorEntryAmountModal = false;
  showStock = true;
  canUpdatePu = true;
  isDiffere = false;
  printTicket = true;
  printInvoice = false;
  showInfosBancaire = false;
  showModeReglementCard = true;
  canForceStock = true;
  showInfosComplementaireReglementCard = true;
  naturesVentes: INatureVente[] = [];
  naturesVente: INatureVente | null = null;
  users: IUser[] = [];
  modeReglements: IPaymentMode[] = [];
  payments: IPayment[] = [];
  modeReglementSelected: any[] = [];
  modeReglementEmitter = new Subject<any>();
  userCaissier?: IUser | null;
  userSeller?: IUser | null = null;
  typePrescriptions: ITypePrescription[] = [];
  typePrescription?: ITypePrescription | null;
  customers: ICustomer[] = [];
  produits: IProduit[] = [];
  produitSelected?: IProduit | null = null;
  searchValue?: string;
  imagesPath!: string;
  customerSelected: ICustomer | null = null;
  ayantDroit: ICustomer | null = null;
  selectedRowIndex?: number;
  produitsSelected?: IProduit[] = [];
  remiseProduits: IRemiseProduit[] = [];
  remiseProduit?: IRemiseProduit | null;
  sale?: ISales | null = null;
  salesLines: ISalesLine[] = [];
  quantiteSaisie = 1;
  base64!: string;
  event: any;
  @ViewChild('clientSearchBox', { static: false })
  clientSearchBox?: ElementRef;
  @ViewChild('quantyBox', { static: false })
  quantyBox?: ElementRef;
  @ViewChild('produitbox', { static: false })
  produitbox?: any;
  @ViewChild('forcerStockBtn', { static: false })
  forcerStockBtn?: ElementRef;
  clientSearchValue?: string | null = null;
  clientBoxHeader = 'INFO CLIENT';
  stockSeverity = 'success';
  produitClass = 'col-6 row';
  rayonClass = 'col-2';
  reglementInputClass = 'p-inputgroup';
  reglementInputParentClass = '';
  montantCash?: number | null = null;
  montantCb?: number | null = null;
  montantVirement?: number | null = null;
  montantMtn?: number | null = null;
  montantOrange?: number | null = null;
  montantMoov?: number | null = null;
  montantWave?: number | null = null;
  montantCheque?: number | null = null;
  entryAmount?: number | null = null;
  commentaire?: string;
  telephone?: string;
  referenceBancaire?: string;
  banque?: string;
  lieux?: string;
  montantCashDiv = false;
  montantCbDiv = false;
  montantVirementDiv = false;
  montantMtnDiv = false;
  montantOrangeDiv = false;
  montantMoovDiv = false;
  montantWaveDiv = false;
  montantChequeDiv = false;
  qtyMaxToSel = 999999;
  derniereMonnaie = 0;
  monnaie = 0;
  check = true; // mis pour le focus produit et dialogue button
  readonly notFoundText = 'Aucun produit';
  @ViewChild('montantCashInput', { static: false })
  montantCashInput?: ElementRef;
  @ViewChild('montantOMInput', { static: false })
  montantOMInput?: ElementRef;
  @ViewChild('montantMTNInput', { static: false })
  montantMTNInput?: ElementRef;
  @ViewChild('montantMOOVInput', { static: false })
  montantMOOVInput?: ElementRef;
  @ViewChild('montantWAVEInput', { static: false })
  montantWAVEInput?: ElementRef;
  @ViewChild('montantCbInput', { static: false })
  montantCbInput?: ElementRef;
  @ViewChild('montantVirInput', { static: false })
  montantVirInput?: ElementRef;
  @ViewChild('montantChInput', { static: false })
  montantChInput?: ElementRef;
  @ViewChild('clientSearchModalBtn', { static: false })
  clientSearchModalBtn?: ElementRef;
  @ViewChild('errorEntryAmountBtn', { static: false })
  errorEntryAmountBtn?: ElementRef;
  @ViewChild('tierspayntDiv', { static: false })
  tierspayntDiv?: ElementRef;
  ref!: DynamicDialogRef;
  primngtranslate: Subscription;
  tiersPayants: IClientTiersPayant[] = [];
  showOrHideTiersPayantBtn = false;
  tiersPayantsOriginal = 0;

  constructor(
    protected salesService: SalesService,
    protected customerService: CustomerService,
    protected produitService: ProduitService,
    protected activatedRoute: ActivatedRoute,
    protected modalService: NgbModal,
    protected saleItemService: SalesLineService,
    protected userService: UserService,
    private accountService: AccountService,
    protected confirmationService: ConfirmationService,
    protected errorService: ErrorService,
    protected configurationService: ConfigurationService,
    protected decondtionService: DeconditionService,
    private dialogService: DialogService,
    public translate: TranslateService,
    public primeNGConfig: PrimeNGConfig,
    private spinner: NgxSpinnerService
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

  ngOnInit(): void {
    this.loadAllUsers();
    this.maxToSale();
    this.accountService.identity().subscribe(account => {
      if (account) {
        this.userCaissier = account;
        if (!this.userSeller) {
          this.userSeller = account;
        }
      }
    });

    this.modeReglementEmitter.subscribe(() => {
      this.manageShowInfosComplementaireReglementCard();
      this.manageShowInfosBancaire();
      this.manageReglementInputParentClass();
      this.manageAmontDiv(null);
    });

    this.modeReglements = [
      { code: 'CASH', libelle: 'ESPECE' },
      { code: 'OM', libelle: 'ORANGE' },
      { code: 'MTN', libelle: 'MTN' },
      { code: 'MOOV', libelle: 'MOOV' },
      { code: 'WAVE', libelle: 'WAVE' },
      { code: 'CB', libelle: 'CB' },
      { code: 'VIREMENT', libelle: 'VIREMENT' },
      { code: 'CH', libelle: 'CHEQUE' },
    ];
    this.modeReglementSelected = ['CASH'];
    this.modeReglementEmitter.next('CASH');
    this.naturesVentes = [
      { code: 'COMPTANT', name: 'COMPTANT', disabled: false },
      { code: 'ASSURANCE', name: 'ASSURANCE', disabled: false },
      {
        code: 'CARNET',
        name: 'CARNET',
        disabled: false,
      },
    ];

    this.typePrescriptions = [
      { code: 'PRESCRIPTION', name: 'PRESCRIPTION' },
      {
        code: 'CONSEIL',
        name: 'CONSEIL',
      },
      { code: 'DEPOT', name: 'DEPÔT' },
    ];
    this.naturesVente = { code: 'COMPTANT', name: 'COMPTANT', disabled: false };
    this.typePrescription = { code: 'PRESCRIPTION', name: 'PRESCRIPTION' };
    this.activatedRoute.data.subscribe(({ sales }) => {
      if (sales.id) {
        this.sale = sales;
        this.salesLines = sales.salesLines;
        this.customerSelected = sales.customer;
        this.naturesVente = this.naturesVentes.find(e => e.code === sales.natureVente) || null;
        this.typePrescription = this.typePrescriptions.find(e => e.code === sales.typePrescription) || null;
        this.buildPaymentFromSale(sales);
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
      if (params.has('isPresale')) {
        this.isPresale = params.get('isPresale') === 'true';
      }
    });
  }

  getTiersPayants(): IClientTiersPayant[] {
    this.tiersPayantsOriginal = this.customerSelected?.tiersPayants?.length!;
    this.tiersPayants = this.customerSelected?.tiersPayants!;
    return this.tiersPayants;
  }

  removeTiersPayant(tiersPayant: IClientTiersPayant): void {
    const newTiersp = this.tiersPayants.filter(e => e.id !== tiersPayant.id);
    this.tiersPayants = newTiersp;
  }

  removeTiersPayantFromIndex(index: number): void {
    this.tiersPayants.splice(index, 1);
    this.showTiersPayantBtn();
  }

  loadAllUsers(): void {
    this.userService.query().subscribe((res: HttpResponse<User[]>) => (this.users = res.body || []));
  }

  manageAmontDiv(modeRegle: string | null): void {
    this.montantCashDiv = this.modeReglementSelected.find(e => e === 'CASH');
    this.montantCbDiv = this.modeReglementSelected.find(e => e === 'CB');
    this.montantVirementDiv = this.modeReglementSelected.find(e => e === 'VIREMENT');
    this.montantMtnDiv = this.modeReglementSelected.find(e => e === 'MTN');
    this.montantOrangeDiv = this.modeReglementSelected.find(e => e === 'OM');
    this.montantMoovDiv = this.modeReglementSelected.find(e => e === 'MOOV');
    this.montantWaveDiv = this.modeReglementSelected.find(e => e === 'WAVE');
    this.montantChequeDiv = this.modeReglementSelected.find(e => e === 'CH');

    if (modeRegle) {
      setTimeout(() => {
        this.manageReglementOnClickFocus(modeRegle);
      }, 100);
    }

    this.resetReglementInput();
  }

  resetReglementInput(): void {
    if (!this.montantCashDiv) {
      this.montantCash = null;
    }
    if (!this.montantCbDiv) {
      this.montantCb = null;
    }

    if (!this.montantVirementDiv) {
      this.montantVirement = null;
    }
    if (!this.montantMtnDiv) {
      this.montantMtn = null;
    }
    if (!this.montantOrangeDiv) {
      this.montantOrange = null;
    }
    if (!this.montantMoovDiv) {
      this.montantMoov = null;
    }
    if (!this.montantWaveDiv) {
      this.montantWave = null;
    }
    if (!this.montantChequeDiv) {
      this.montantCheque = null;
    }
  }

  manageShowInfosComplementaireReglementCard(): void {
    const mode = (element: string) => {
      if (element === 'CB' || element === 'VIREMENT' || element === 'CH' || this.isDiffere) return true;
      return false;
    };
    this.showInfosComplementaireReglementCard = this.modeReglementSelected.some(mode);
  }

  manageShowInfosBancaire(): void {
    const mode = (element: string) => {
      if (element === 'CB' || element === 'VIREMENT' || element === 'CH') return true;
      return false;
    };
    this.showInfosBancaire = this.modeReglementSelected.some(mode);
  }

  manageReglementInputParentClass(): void {
    if (this.modeReglementSelected && this.modeReglementSelected.length < 2) {
      this.reglementInputParentClass = '';
      this.reglementInputClass = 'p-inputgroup';
    } else {
      if (this.modeReglementSelected.length === 2) {
        this.reglementInputParentClass = 'row';
        this.reglementInputClass = 'p-inputgroup col-6';
      }
      if (this.modeReglementSelected.length === 3 && !this.showInfosComplementaireReglementCard) {
        this.reglementInputParentClass = 'row';
        this.reglementInputClass = 'p-inputgroup col-4';
      }
      if (this.modeReglementSelected.length === 3 && this.showInfosComplementaireReglementCard) {
        this.reglementInputParentClass = 'row';
        this.reglementInputClass = 'p-inputgroup col-6';
      }
      if (this.modeReglementSelected.length > 3 && !this.showInfosComplementaireReglementCard) {
        this.reglementInputParentClass = 'row';
        this.reglementInputClass = 'p-inputgroup col-3';
      }
      if (this.modeReglementSelected.length > 3 && this.showInfosComplementaireReglementCard) {
        this.reglementInputParentClass = 'row';
        this.reglementInputClass = 'p-inputgroup col-6';
      }
    }
  }

  previousState(): void {
    window.history.back();
  }

  save(): void {
    if (this.sale) {
      this.isSaving = true;
      if (this.isPresale === false) {
        this.sale.differe = this.isDiffere;
        this.sale.payments = this.buildPayment();
        const thatentryAmount = this.getEntryAmount();
        this.computeMonnaie();
        const restToPay = this.sale?.amountToBePaid! - thatentryAmount;
        if (this.naturesVente && this.naturesVente.code === 'COMPTANT') {
          this.sale.type = 'VNO';
          this.sale.montantRendu = this.monnaie;
          this.sale.montantVerse = this.montantCash ?? 0;
          if (!this.isDiffere && thatentryAmount < this.sale.amountToBePaid!) {
            // this.openInfoDialog("Le montant  saisi n'est pas correct", 'alert alert-warning');
            this.displayErrorEntryAmountModal = true;
            this.errorEntryAmountBtn?.nativeElement.focus();
          } else if (this.isDiffere && !this.customerSelected) {
            this.displayErrorModal = true;
            this.clientSearchModalBtn?.nativeElement.focus();
            // this.openInfoDialog("Veuilez ajouter un client à la vente", 'alert alert-warning');
          } else {
            if (restToPay <= 0) {
              this.sale.payrollAmount = this.sale.amountToBePaid;
              this.sale.restToPay = 0;
            } else {
              this.sale.payrollAmount = thatentryAmount;
              this.sale.restToPay = restToPay;
            }
            this.subscribeToFinalyseResponse(this.salesService.saveComptant(this.sale));
          }
        }
      } else if (this.isPresale === true) {
        if (this.naturesVente && this.naturesVente.code === 'COMPTANT') {
          this.putCurrentSaleOnHold();
        }
      }
    }
  }

  onHideClientErrorDialog(event: Event): void {
    this.clientSearchBox?.nativeElement.focus();
  }

  cancelErrorModal(): void {
    this.displayErrorModal = false;
  }

  canceldisplayErrorEntryAmountModal(): void {
    this.displayErrorEntryAmountModal = false;
  }

  onHidedisplayErrorEntryAmountModal(event: Event): void {
    this.montantCashInput?.nativeElement.focus();
  }

  putCurrentSaleOnHold(): void {
    if (this.sale) {
      this.isSaving = true;
      this.sale.payments = this.buildPayment();
      if (this.naturesVente && this.naturesVente.code === 'COMPTANT') {
        this.sale.type = 'VNO';
        this.subscribeToPutOnHoldResponse(this.salesService.putCurrentCashSaleOnHold(this.sale));
      }
    }
  }

  saveAntPrint(): void {
    this.isSaving = true;
    //  this.subscribeToFinalyseResponse(this.salesService.saveComptant(this.sale!));
  }

  searchUser(event: any): void {
    const key = event.key;
    if (
      key !== 'ArrowDown' &&
      key !== 'ArrowUp' &&
      key !== 'ArrowRight' &&
      key !== 'ArrowLeft' &&
      key !== 'NumLock' &&
      key !== 'CapsLock' &&
      key !== 'Control' &&
      key !== 'PageUp' &&
      key !== 'PageDown'
    ) {
      this.loadAllUsers();
    }
  }

  onSelectUser(): void {
    this.produitbox.focus();
  }

  searchFn(event: any): void {
    const key = event.key;
    if (
      key !== 'ArrowDown' &&
      key !== 'ArrowUp' &&
      key !== 'ArrowRight' &&
      key !== 'ArrowLeft' &&
      key !== 'NumLock' &&
      key !== 'CapsLock' &&
      key !== 'Control' &&
      key !== 'PageUp' &&
      key !== 'PageDown'
    ) {
      this.searchValue = event.target.value;
      this.loadProduits();
    }
  }

  produitComponentSearch(term: string, item: IProduit): boolean {
    if (item) return true;
    return false;
  }

  onSelect(): void {
    if (this.quantyBox) {
      this.quantyBox.nativeElement.focus();
      this.quantyBox.nativeElement.select();
    }
    if (this.produitSelected?.totalQuantity! > 0) {
      this.stockSeverity = 'success';
    } else {
      this.stockSeverity = 'danger';
    }
  }

  onSelectKeyDow(event: KeyboardEvent): void {
    if (event.key === 'Enter' && this.produitSelected) {
      if (this.quantyBox) {
        this.quantyBox.nativeElement.focus();
        this.quantyBox.nativeElement.select();
      }
      if (this.produitSelected.totalQuantity! > 0) {
        this.stockSeverity = 'success';
      } else {
        this.stockSeverity = 'danger';
      }
    } else if (event.key === 'Enter' && this.sale && this.salesLines.length > 0) {
      this.montantCashInputAssignment2();
    }
  }

  montantCashInputAssignment2(): void {
    if (this.montantCashInput) {
      this.montantCashInput.nativeElement.focus();
      this.montantCash = this.sale?.amountToBePaid;
      setTimeout(() => {
        this.montantCashInput?.nativeElement.select();
      }, 50);
    }
  }

  montantCashInputAssignment(): void {
    if (this.montantCashInput) {
      this.entryAmount = this.getEntryAmount();
      if (this.entryAmount) {
        const restTopay = this.sale?.amountToBePaid! - this.entryAmount;
        if (restTopay && restTopay > 0) {
          this.montantCash = restTopay;
        } else {
          this.montantCash = 0;
        }
      } else {
        this.montantCash = this.sale?.amountToBePaid;
      }
      this.montantCashInput.nativeElement.focus();
      setTimeout(() => {
        this.montantCashInput?.nativeElement.select();
      }, 50);
    }
  }

  montantCbInputAssignment(): void {
    if (this.montantCbInput) {
      this.montantCbInput.nativeElement.focus();
      this.entryAmount = this.getEntryAmount();
      if (this.entryAmount) {
        const restTopay = this.sale?.amountToBePaid! - this.entryAmount;
        if (restTopay && restTopay > 0) {
          this.montantCb = restTopay;
        } else {
          this.montantCb = 0;
        }
      } else {
        this.montantCb = this.sale?.amountToBePaid;
      }
      setTimeout(() => {
        this.montantCbInput?.nativeElement.select();
      }, 50);
    }
  }

  montantOMInputAssignment(): void {
    if (this.montantOMInput) {
      this.montantOMInput.nativeElement.focus();
      this.entryAmount = this.getEntryAmount();
      if (this.entryAmount) {
        const restTopay = this.sale?.amountToBePaid! - this.entryAmount;
        if (restTopay && restTopay > 0) {
          this.montantOrange = restTopay;
        } else {
          this.montantOrange = 0;
        }
      } else {
        this.montantOrange = this.sale?.amountToBePaid;
      }
      setTimeout(() => {
        this.montantOMInput?.nativeElement.select();
      }, 50);
    }
  }

  montantVirementInputAssignment(): void {
    if (this.montantVirInput) {
      this.montantVirInput.nativeElement.focus();
      this.entryAmount = this.getEntryAmount();
      if (this.entryAmount) {
        const restTopay = this.sale?.amountToBePaid! - this.entryAmount;
        if (restTopay && restTopay > 0) {
          this.montantVirement = restTopay;
        } else {
          this.montantVirement = 0;
        }
      } else {
        this.montantVirement = this.sale?.amountToBePaid;
      }
      setTimeout(() => {
        this.montantVirInput?.nativeElement.select();
      }, 50);
    }
  }

  montantMtnInputAssignment(): void {
    if (this.montantMTNInput) {
      this.montantMTNInput.nativeElement.focus();
      this.entryAmount = this.getEntryAmount();
      if (this.entryAmount) {
        const restTopay = this.sale?.amountToBePaid! - this.entryAmount;
        if (restTopay && restTopay > 0) {
          this.montantMtn = restTopay;
        } else {
          this.montantMtn = 0;
        }
      } else {
        this.montantMtn = this.sale?.amountToBePaid;
      }
      setTimeout(() => {
        this.montantMTNInput?.nativeElement.select();
      }, 50);
    }
  }

  montantMOOVInputAssignment(): void {
    if (this.montantMOOVInput) {
      this.montantMOOVInput.nativeElement.focus();
      this.entryAmount = this.getEntryAmount();
      if (this.entryAmount) {
        const restTopay = this.sale?.amountToBePaid! - this.entryAmount;
        if (restTopay && restTopay > 0) {
          this.montantMoov = restTopay;
        } else {
          this.montantMoov = 0;
        }
      } else {
        this.montantMoov = this.sale?.amountToBePaid;
      }
      setTimeout(() => {
        this.montantMOOVInput?.nativeElement.select();
      }, 50);
    }
  }

  montantWAVEInputAssignment(): void {
    if (this.montantWAVEInput) {
      this.montantWAVEInput.nativeElement.focus();
      this.entryAmount = this.getEntryAmount();
      if (this.entryAmount) {
        const restTopay = this.sale?.amountToBePaid! - this.entryAmount;
        if (restTopay && restTopay > 0) {
          this.montantWave = restTopay;
        } else {
          this.montantWave = 0;
        }
      } else {
        this.montantWave = this.sale?.amountToBePaid;
      }
      setTimeout(() => {
        this.montantWAVEInput?.nativeElement.select();
      }, 50);
    }
  }

  montantCHInputAssignment(): void {
    if (this.montantChInput) {
      this.montantChInput.nativeElement.focus();
      this.entryAmount = this.getEntryAmount();
      if (this.entryAmount) {
        const restTopay = this.sale?.amountToBePaid! - this.entryAmount;
        if (restTopay && restTopay > 0) {
          this.montantCheque = restTopay;
        } else {
          this.montantCheque = 0;
        }
      } else {
        this.montantCheque = this.sale?.amountToBePaid;
      }
      setTimeout(() => {
        this.montantChInput?.nativeElement.select();
      }, 50);
    }
  }

  manageReglementFocus(): void {
    const cashInput = this.modeReglementSelected.find(mode => mode === 'CASH');
    if (cashInput) {
      this.montantCashInputAssignment();
    } else {
      const cb = this.modeReglementSelected.find(mode => mode === 'CB');
      const om = this.modeReglementSelected.find(mode => mode === 'OM');
      const VIREMENT = this.modeReglementSelected.find(mode => mode === 'VIREMENT');
      const MTN = this.modeReglementSelected.find(mode => mode === 'MTN');
      const MOOV = this.modeReglementSelected.find(mode => mode === 'MOOV');
      const WAVE = this.modeReglementSelected.find(mode => mode === 'WAVE');
      const CH = this.modeReglementSelected.find(mode => mode === 'CH');
      if (cb) {
        this.montantCbInputAssignment();
      } else if (om) {
        this.montantOMInputAssignment();
      } else if (VIREMENT) {
        this.montantVirementInputAssignment();
      } else if (MTN) {
        this.montantMtnInputAssignment();
      } else if (MOOV) {
        this.montantMOOVInputAssignment();
      } else if (WAVE) {
        this.montantWAVEInputAssignment();
      } else if (CH) {
        this.montantCHInputAssignment();
      }
    }
  }

  manageReglementOnClickFocus(modeSelected: string): void {
    const selectedMode = this.modeReglementSelected.find(mode => mode === modeSelected);
    if (selectedMode === 'CASH') {
      this.montantCashInputAssignment();
    } else if (selectedMode === 'CB') {
      this.montantCbInputAssignment();
    } else if (selectedMode === 'OM') {
      this.montantOMInputAssignment();
    } else if (selectedMode === 'VIREMENT') {
      this.montantVirementInputAssignment();
    } else if (selectedMode === 'MTN') {
      this.montantMtnInputAssignment();
    } else if (selectedMode === 'MOOV') {
      this.montantMOOVInputAssignment();
    } else if (selectedMode === 'WAVE') {
      this.montantWAVEInputAssignment();
    } else if (selectedMode === 'CH') {
      this.montantCHInputAssignment();
    }
  }

  onModeReglementChange(event: any): void {
    let modeRegle = event.originalEvent.target.innerText;
    this.manageShowInfosComplementaireReglementCard();
    this.manageShowInfosBancaire();
    this.manageReglementInputParentClass();
    if (modeRegle === 'ORANGE') {
      modeRegle = 'OM';
    } else if (modeRegle === 'CHEQUE') {
      modeRegle = 'CH';
    } else if (modeRegle === 'ESPECE') {
      modeRegle = 'CASH';
    }
    this.manageAmontDiv(modeRegle);
  }

  showAyantDroit(): boolean {
    if (this.naturesVente?.code === 'ASSURANCE' && this.ayantDroit) {
      return true;
    }
    return false;
  }

  showTiersPayant(): boolean {
    if ((this.naturesVente?.code === 'ASSURANCE' || this.naturesVente?.code === 'CARNET') && this.customerSelected) {
      return true;
    }
    return false;
  }

  showTiersPayantBtn(): void {
    this.showOrHideTiersPayantBtn = this.tiersPayants.length !== this.tiersPayantsOriginal;
  }

  showClientSearch(): boolean {
    if ((this.isDiffere && this.sale) || this.naturesVente?.code !== 'COMPTANT') {
      return true;
    }
    return false;
  }

  firstRefBonFocus(): void {
    this.tierspayntDiv?.nativeElement.querySelector('input#tierspayant_0')?.focus();
  }

  lastRefBonFocus(): void {
    const refBonInputs = this.tierspayntDiv?.nativeElement.querySelector('input.ref-bon');
    refBonInputs?.slice(-1)?.focus();
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

  onFilterTextBoxChanged(event: any): void {
    this.searchValue = event.target.value;
    this.loadProduits();
  }

  computeMonnaie(): void {
    const thatentryAmount = this.getEntryAmount();
    const thatMonnaie = thatentryAmount - this.sale?.amountToBePaid!;
    this.monnaie = thatMonnaie > 0 ? thatMonnaie : 0;
  }

  getEntryAmount(): number {
    return (
      this.montantCash! +
      this.montantCb! +
      this.montantCheque! +
      this.montantVirement! +
      this.montantOrange! +
      this.montantWave! +
      this.montantMoov! +
      this.montantMtn!
    );
  }

  onReglementInputChange(): void {
    this.computeMonnaie();
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
        if (this.naturesVente && this.naturesVente.code === 'COMPTANT') {
          this.subscribeToSaveLineResponse(this.salesService.addItemComptant(this.createSalesLine(this.produitSelected, qytMvt)));
        } else {
          this.subscribeToSaveLineResponse(this.salesService.addItemAssurance(this.createSalesLine(this.produitSelected, qytMvt)));
        }
      }
    } else {
      if (this.produitSelected) {
        if (this.naturesVente && this.naturesVente.code === 'COMPTANT') {
          this.subscribeToCreateSaleComptantResponse(
            this.salesService.createComptant(this.createSaleComptant(this.produitSelected, qytMvt))
          );
        }
      }
    }
    this.computeMonnaie();
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
    if (this.naturesVente?.code === 'COMPTANT') {
      this.addUninsuredCustomer();
    } else {
      this.addAssuredCustomer();
    }
  }

  editCustomer(): void {
    if (this.naturesVente?.code === 'COMPTANT') {
      this.editUninsuredCustomer();
    } else {
      this.editAssuredCustomer();
    }
  }

  loadsCustomer(): void {
    if (this.naturesVente?.code === 'COMPTANT') {
      this.loadUninsuredCustomers();
    } else {
      this.loadAssuredCustomers();
    }
  }

  openCustomerListTable(customers: ICustomer[] | []): void {
    if (this.naturesVente?.code === 'COMPTANT') {
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

  removeLine(salesLine: ISalesLine): void {
    this.salesService.deleteItem(salesLine.id!).subscribe(() => {
      if (this.sale) {
        this.subscribeToSaveResponse(this.salesService.find(this.sale.id!));
      }
    });
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
    this.salesService.updateItemQtySold(salesLine).subscribe(
      () => {
        if (this.sale) {
          this.subscribeToSaveResponse(this.salesService.find(this.sale.id!));
        }
      },
      () => {
        this.onSaveError();
        this.subscribeToSaveResponse(this.salesService.find(this.sale?.id!));
      }
    );
  }

  updateItemPrice(salesLine: ISalesLine, event: any): void {
    const newPrice = Number(event.target.value);
    if (newPrice <= 0) return;
    salesLine.regularUnitPrice = newPrice;
    this.salesService.updateItemPrice(salesLine).subscribe(
      () => {
        if (this.sale) {
          this.subscribeToSaveResponse(this.salesService.find(this.sale.id!));
        }
      },
      () => {
        this.onSaveError();
        this.subscribeToSaveResponse(this.salesService.find(this.sale?.id!));
      }
    );
  }

  formatNumber(number: any): string {
    return Math.floor(number.value)
      .toString()
      .replace(/(\d)(?=(\d{3})+(?!\d))/g, '$1 ');
  }

  openInfoDialog(message: string, infoClass: string): void {
    const modalRef = this.modalService.open(AlertInfoComponent, { backdrop: 'static', centered: true });
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
          this.decondtionService.create(this.createDecondition(qtyDecondtionner, produit.id!)).subscribe(
            () => {
              if (item) {
                this.processQtyRequested(item);
              } else {
                this.onAddProduit(qytMvt);
              }
            },
            error => {
              if (error.error && (error.error.status === 500 || error.error.status === 400)) {
                this.openInfoDialog('Erreur applicatif', 'alert alert-danger');
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

  restAll(): void {
    this.sale = null;
    this.salesLines = [];
    this.modeReglementSelected = [];
    this.modeReglementSelected = ['CASH'];
    this.modeReglementEmitter.next('CASH');
    this.naturesVente = { code: 'COMPTANT', name: 'COMPTANT' };
    this.typePrescription = { code: 'PRESCRIPTION', name: 'PRESCRIPTION' };
    this.userSeller = this.userCaissier;
    this.check = true;
    this.derniereMonnaie = this.monnaie;
    this.monnaie = 0;
    this.payments = [];
    this.montantCheque = null;
    this.montantCash = null;
    this.montantVirement = null;
    this.montantCb = null;
    this.montantWave = null;
    this.montantMoov = null;
    this.montantMtn = null;
    this.montantOrange = null;
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
    if (this.montantCash && this.montantCash > 0) {
      this.payments.push(this.createPayment(this.montantCash, 'CASH'));
    }
    if (this.montantCb && this.montantCb > 0) {
      this.payments.push(this.createPayment(this.montantCb, 'CB'));
    }
    if (this.montantVirement && this.montantVirement > 0) {
      this.payments.push(this.createPayment(this.montantVirement, 'VIREMENT'));
    }
    if (this.montantCheque && this.montantCheque > 0) {
      this.payments.push(this.createPayment(this.montantCheque, 'CH'));
    }
    if (this.montantOrange && this.montantOrange > 0) {
      this.payments.push(this.createPayment(this.montantOrange, 'OM'));
    }
    if (this.montantWave && this.montantWave > 0) {
      this.payments.push(this.createPayment(this.montantWave, 'WAVE'));
    }
    if (this.montantMtn && this.montantMtn > 0) {
      this.payments.push(this.createPayment(this.montantMtn, 'MTN'));
    }
    if (this.montantMoov && this.montantMoov > 0) {
      this.payments.push(this.createPayment(this.montantMoov, 'MOOV'));
    }
    return this.payments;
  }

  buildPaymentFromSale(sale: ISales): void {
    sale.payments?.forEach(payment => {
      if (payment.paymentMode) {
        const code = payment.paymentMode.code;
        this.modeReglementSelected.push(code);
        if (code === 'CASH') {
          this.montantCash = payment.netAmount;
        } else if (code === 'OM') {
          this.montantOrange = payment.netAmount;
        } else if (code === 'CB') {
          this.montantCb = payment.netAmount;
        } else if (code === 'CH') {
          this.montantCheque = payment.netAmount;
        } else if (code === 'VIREMENT') {
          this.montantVirement = payment.netAmount;
        } else if (code === 'WAVE') {
          this.montantWave = payment.netAmount;
        } else if (code === 'MOOV') {
          this.montantMoov = payment.netAmount;
        } else if (code === 'MTN') {
          this.montantMtn = payment.netAmount;
        }
      }
    });
    this.modeReglementEmitter.next(1);
  }

  loadUninsuredCustomers(): void {
    if (this.clientSearchValue) {
      this.spinner.show('salespinner');
      this.customerService.queryUninsuredCustomers({ search: this.clientSearchValue }).subscribe(
        res => {
          this.spinner.hide('salespinner');
          const uninsuredCustomers = res.body;
          if (uninsuredCustomers && uninsuredCustomers.length > 0) {
            if (uninsuredCustomers.length === 1) {
              this.customerSelected = uninsuredCustomers[0];
            } else {
              this.openUninsuredCustomerListTable(uninsuredCustomers);
            }

            this.clientSearchValue = null;
          } else {
            this.addUninsuredCustomer();
            this.clientSearchValue = null;
          }
        },
        () => {
          this.spinner.hide('salespinner');
        }
      );
    }
  }

  loadAssuredCustomers(): void {
    if (this.clientSearchValue) {
      this.spinner.show('salespinner');
      this.customerService.queryAssuredCustomer({ search: this.clientSearchValue }).subscribe(
        res => {
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
        () => {
          this.spinner.hide('salespinner');
        }
      );
    }
  }

  onNatureVenteChange(event: any): void {
    const selectNature = event.value;

    if (selectNature.code !== 'COMPTANT' && this.sale && this.customerSelected) {
      const nature = (element: INatureVente) => {
        if (element.code === 'COMPTANT') return true;
        return false;
      };
      this.naturesVentes.find(nature)!.disabled = true;

      //TODO si vente en cours et vente est de type VO, alors message si la nvelle est COMPTANT, ON ne peut transformer une VO en VNO
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

  protected processQtyRequested(salesLine: ISalesLine): void {
    this.salesService.updateItemQtyRequested(salesLine).subscribe(
      () => {
        if (this.sale) {
          this.subscribeToSaveResponse(this.salesService.find(this.sale.id!));
        }
        this.check = true;
      },
      error => {
        this.check = false;
        this.subscribeToSaveResponse(this.salesService.find(this.sale?.id!));
        this.onStockError(salesLine, error);
      }
    );
  }

  private createPayment(montant: number, code: string): Payment {
    return {
      ...new Payment(),
      netAmount: this.sale?.amountToBePaid,
      paidAmount: montant,
      paymentMode: this.createPaymentMode(code),
      montantVerse: this.montantCash ?? 0,
    };
  }

  private createPaymentMode(code: string): PaymentMode {
    return {
      ...new PaymentMode(),
      code,
    };
  }

  private createDecondition(qtyDeconditione: number, produitId: number): IDecondition {
    return {
      ...new Decondition(),
      qtyMvt: qtyDeconditione,
      produitId,
    };
  }

  protected subscribeToSaveLineResponse(result: Observable<HttpResponse<ISalesLine>>): void {
    result.subscribe(
      (res: HttpResponse<ISalesLine>) => this.subscribeToSaveResponse(this.salesService.find(res.body?.saleId!)),
      () => this.onSaveError()
    );
  }

  protected updateProduitQtyBox(): void {
    if (this.quantyBox) {
      this.quantyBox.nativeElement.value = 1;
    }
    if (this.check) {
      this.produitbox?.focus();
    } else {
      this.forcerStockBtn?.nativeElement.focus();
    }

    this.produitSelected = null;
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<ISales>>): void {
    result.subscribe(
      (res: HttpResponse<ISales>) => this.onSaveSuccess(res.body),
      () => this.onSaveError()
    );
  }

  protected subscribeToUpdateItemPriceOrQuantityResponse(result: Observable<HttpResponse<ISales>>): void {
    result.subscribe(
      (res: HttpResponse<ISales>) => this.onSaveSuccess(res.body),
      () => this.onSaveError()
    );
  }

  protected onProduitSuccess(data: IProduit[] | null): void {
    this.produits = data || [];
  }

  private createSaleComptant(produit: IProduit, quantitySold: number): ISales {
    return {
      ...new Sales(),
      salesLines: [this.createSalesLine(produit, quantitySold)],
      customer: this.customerSelected!,
      natureVente: this.naturesVente?.code,
      typePrescription: this.typePrescription?.code,
      cassier: this.userCaissier!,
      seller: this.userSeller!,
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

  private updateSalesLine(item: ISalesLine): ISalesLine {
    return {
      ...new SalesLine(),
      produitId: item.produit?.id,
      regularUnitPrice: item.regularUnitPrice,
      saleId: this.sale?.id,
      quantitySold: item.quantitySold,
      sales: this.sale,
    };
  }

  protected onSaveSuccess(sale: ISales | null): void {
    this.isSaving = false;
    this.sale = sale!;
    this.salesLines = this.sale.salesLines!;
    this.computeMonnaie();
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
      this.onPrintInvoice(); // TODO a remplacer par le code impression ticket de caisse
    }
    if (this.printInvoice) {
      this.onPrintInvoice();
    }
    this.restAll();
  }

  protected subscribeToFinalyseResponse(result: Observable<HttpResponse<IResponseDto>>): void {
    result.subscribe(
      (res: HttpResponse<IResponseDto>) => this.onFinalyseSuccess(res.body),
      () => this.onSaveError()
    );
  }

  protected subscribeToPutOnHoldResponse(result: Observable<HttpResponse<IResponseDto>>): void {
    result.subscribe(
      () => {
        this.isSaving = false;
        this.restAll();
      },
      () => this.onSaveError()
    );
  }

  protected refresh(): void {
    this.subscribeToSaveResponse(this.salesService.find(this.sale?.id!));
  }

  protected subscribeToCreateSaleComptantResponse(result: Observable<HttpResponse<ISales>>): void {
    result.subscribe(
      (res: HttpResponse<ISales>) => this.onSaleComptantResponseSuccess(res.body),
      error => this.onCommonError(error)
    );
  }

  protected onSaleComptantResponseSuccess(sale: ISales | null): void {
    this.isSaving = false;
    this.sale = sale;
    if (sale && sale.salesLines) {
      this.salesLines = sale?.salesLines;
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
    if (error.error && (error.error.status === 500 || error.error.status === 400)) {
      this.openInfoDialog('Erreur applicatif', 'alert alert-danger');
    } else {
      this.errorService.getErrorMessageTranslation(error.error.errorKey).subscribe(
        translatedErrorMessage => {
          this.openInfoDialog(translatedErrorMessage, 'alert alert-danger');
        },
        () => this.openInfoDialog(error.error.title, 'alert alert-danger')
      );
    }
  }

  private onPrintInvoice(): void {
    this.salesService.print(this.sale?.id!).subscribe(blod => {
      const blobUrl = URL.createObjectURL(blod);
      window.open(blobUrl);
    });
  }
}
