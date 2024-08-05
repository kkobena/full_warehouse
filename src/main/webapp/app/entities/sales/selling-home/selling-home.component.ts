import { AfterViewInit, Component, effect, ElementRef, inject, OnDestroy, OnInit, viewChild } from '@angular/core';
import { AutoCompleteModule } from 'primeng/autocomplete';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogModule } from 'primeng/dialog';
import { DividerModule } from 'primeng/divider';
import { DropdownModule } from 'primeng/dropdown';
import { InputSwitchModule } from 'primeng/inputswitch';
import { InputTextModule } from 'primeng/inputtext';
import { KeyFilterModule } from 'primeng/keyfilter';
import { ConfirmationService, MessageService, PrimeNGConfig } from 'primeng/api';
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
import { AyantDroitCustomerListComponent } from '../ayant-droit-customer-list/ayant-droit-customer-list.component';
import { TiersPayantCustomerListComponent } from '../../customer/tiers-payant-customer-list/tiers-payant-customer-list.component';
import { FormAyantDroitComponent } from '../../customer/form-ayant-droit/form-ayant-droit.component';
import { INatureVente } from '../../../shared/model/nature-vente.model';
import { IUser, User } from '../../../core/user/user.model';
import { ITypePrescription } from '../../../shared/model/prescription-vente.model';
import { ICustomer } from '../../../shared/model/customer.model';
import { IProduit } from '../../../shared/model/produit.model';
import { IRemise } from '../../../shared/model/remise.model';
import { FinalyseSale, InputToFocus, ISales, SaveResponse, StockError } from '../../../shared/model/sales.model';
import { ISalesLine, SalesLine } from '../../../shared/model/sales-line.model';
import { PRODUIT_COMBO_MIN_LENGTH, PRODUIT_NOT_FOUND } from '../../../shared/constants/pagination.constants';
import { Observable, Subscription } from 'rxjs';
import { SalesService } from '../sales.service';
import { CustomerService } from '../../customer/customer.service';
import { ProduitService } from '../../produit/produit.service';
import { NgbModal, NgbNavChangeEvent } from '@ng-bootstrap/ng-bootstrap';
import { UserService } from '../../../core/user/user.service';
import { AccountService } from '../../../core/auth/account.service';
import { ErrorService } from '../../../shared/error.service';
import { DeconditionService } from '../../decondition/decondition.service';
import { TranslateService } from '@ngx-translate/core';
import { AssuranceService } from '../assurance.service';
import { Decondition, IDecondition } from '../../../shared/model/decondition.model';
import { HttpResponse } from '@angular/common/http';
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
import { AssuranceComponent } from './assurance/assurance.component';
import { AssuranceDataComponent } from './assurance/assurance-data/assurance-data.component';
import { TypePrescriptionService } from '../service/type-prescription.service';
import { UserCaissierService } from '../service/user-caissier.service';
import { UserVendeurService } from '../service/user-vendeur.service';
import { SaleEvent, SaleEventManager } from '../service/sale-event-manager.service';
import { VoSalesService } from '../service/vo-sales.service';
import { HasAuthorityService } from '../service/has-authority.service';
import { ToastModule } from 'primeng/toast';
import { IClientTiersPayant } from '../../../shared/model/client-tiers-payant.model';
import { BaseSaleService } from '../service/base-sale.service';

@Component({
  selector: 'jhi-selling-home',
  standalone: true,
  providers: [ConfirmationService, DialogService, MessageService],
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
    ToastModule,
  ],
  templateUrl: './selling-home.component.html',
})
export class SellingHomeComponent implements OnInit, AfterViewInit, OnDestroy {
  responseEvent: Subscription;
  saveResponse: Subscription;
  inputBoxFocus: Subscription;
  onCompleteSale: Subscription;
  readonly minLength = PRODUIT_COMBO_MIN_LENGTH;
  canForceStock: boolean;
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
  typePrescriptionService = inject(TypePrescriptionService);
  userCaissierService = inject(UserCaissierService);
  userVendeurService = inject(UserVendeurService);
  currentAccount = this.accountService.trackCurrentAccount();
  hasAuthorityService = inject(HasAuthorityService);
  messageService = inject(MessageService);
  voSalesService = inject(VoSalesService);
  baseSaleService = inject(BaseSaleService);
  protected check = true; // mis pour le focus produit et dialogue button
  protected printInvoice = false;
  protected naturesVentes: INatureVente[] = [];
  protected naturesVente: INatureVente | null = null;
  protected users: IUser[];
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
  protected showStock = true;
  protected canUpdatePu = true;
  protected printTicket = true;
  protected active = 'comptant';
  protected monnaie: number;
  private readonly saleEventManager = inject(SaleEventManager);

  constructor(
    protected selectModeReglementService: SelectModeReglementService,
    protected currentSaleService: CurrentSaleService,
    protected selectedCustomerService: SelectedCustomerService,
    protected lastCurrencyGivenService: LastCurrencyGivenService,
    protected salesService: SalesService,
    protected assuranceSalesService: VoSalesService,
    protected customerService: CustomerService,
    protected produitService: ProduitService,
    protected activatedRoute: ActivatedRoute,
    protected router: Router,
    protected modalService: NgbModal,
    protected userService: UserService,
    private accountService: AccountService,
    public confirmationService: ConfirmationService,
    protected errorService: ErrorService,
    protected decondtionService: DeconditionService,
    public dialogService: DialogService,
    public translate: TranslateService,
    public primeNGConfig: PrimeNGConfig,
    private assuranceService: AssuranceService,
  ) {
    // this. canForceStock=this.hasAuthorityService.hasAuthorities('FORCE_STOCK');
    this.canForceStock = true;
    this.onCompleteSale = this.saleEventManager.subscribe('completeSale', (response: SaleEvent<unknown>) => {
      if (this.assuranceComponent()) {
        const content = response.content;
        if (content === 'save') {
          this.saveAssurance();
        } else if (content === 'standby') {
          this.setEnAttenteAssurance();
        }
      }
    });

    this.responseEvent = this.saleEventManager.subscribe('responseEvent', (response: SaleEvent<unknown>) => {
      const content = response.content;
      if (content instanceof FinalyseSale) {
        this.onFinalyse(content);
      }
    });
    this.saveResponse = this.saleEventManager.subscribe('saveResponse', (response: SaleEvent<unknown>) => {
      const content = response.content;
      if (content instanceof SaveResponse) {
        this.onSave(content);
      } else if (content instanceof StockError) {
        this.onStockOutError(content);
      }
    });
    this.inputBoxFocus = this.saleEventManager.subscribe('inputBoxFocus', (response: SaleEvent<unknown>) => {
      const content = response.content;
      if (content instanceof InputToFocus) {
        this.getControlToFocus(content);
      }
    });

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
      if (this.selectedCustomerService.selectedCustomerSignal() && this.currentSaleService.currentSale()) {
        if (this.currentSaleService.currentSale().type === 'VNO') {
          this.onAddCustommer();
        } else {
          if (!this.currentSaleService.currentSale().customerId && this.currentSaleService.voFromCashSale()) {
            this.updateTransformedSales();
          } else if (this.currentSaleService.currentSale().customerId !== this.selectedCustomerService.selectedCustomerSignal().id) {
            this.changeCustomer();
          }
        }
      } else {
        if (this.currentSaleService.currentSale()) {
          if (this.currentSaleService.currentSale().type === 'VNO') {
            this.onRemoveCustomer();
          }
        }
      }
    });

    effect(() => {
      this.monnaie = this.lastCurrencyGivenService.givenCurrency();
    });
  }

  onRemoveCustomer(): void {
    if (this.active === 'comptant') {
      this.salesService.removeCustommerToCashSale(this.currentSaleService.currentSale().id).subscribe(() => {});
    } else {
      console.log('remove customer');
    }
  }

  onAddCustommer(): void {
    if (this.active === 'comptant') {
      this.salesService
        .addCustommerToCashSale({
          key: this.currentSaleService.currentSale().id,
          value: this.selectedCustomerService.selectedCustomerSignal().id,
        })
        .subscribe(() => {
          this.currentSaleService.currentSale().customerId = this.selectedCustomerService.selectedCustomerSignal().id;

          this.currentSaleService.setCurrentSale(this.sale);
        });
    }
  }

  onLoadPrevente(sales: ISales, toEdit: boolean = true): void {
    if (sales.statut === SalesStatut.CLOSED) {
      this.router.navigate(['/sales', false, 'new']);
    } else {
      this.currentSaleService.setCurrentSale(sales);
      this.currentSaleService.setIsEdit(toEdit);
      if (sales && sales.type === 'VNO') {
        this.active = 'comptant';
      } else if (sales && sales.type === 'VO') {
        this.active = 'assurance';
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
    this.loadAllUsers();
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
    } else if (this.active === 'assurance') {
      this.assuranceComponent().manageAmountDiv();
    }
  }

  previousState(): void {
    this.resetAll();
    window.history.back();
  }

  save(): void {
    console.log('on save');
    if (this.currentSaleService.currentSale()) {
      if (this.active === 'comptant') {
        this.comptantComponent().save();
      } else if (this.active === 'assurance') {
        this.saveAssurance();
      }
    }
  }

  saveAssurance(): void {
    this.currentSaleService.currentSale().tiersPayants = this.assuranceDataComponent().buildIClientTiersPayantFromInputs();
    if (this.checkEmptyBon()) {
      this.messageService.add({
        severity: 'warn',
        summary: 'Alerte',
        detail: 'Veuillez saisir le numéro de bon pour tous les tiers payants',
      });
      //this.openInfoDialog('Veuillez saisir le numéro de bon pour tous les tiers payants', 'alert alert-danger');
    } else {
      this.assuranceComponent().save();
    }
  }

  setEnAttenteAssurance(): void {
    this.currentSaleService.currentSale().tiersPayants = this.assuranceDataComponent().buildIClientTiersPayantFromInputs();
    this.assuranceComponent().finalyseSale(true);
  }

  onHideHideDialog(): void {}

  cancelCommonDialog(): void {
    this.commonDialog = false;
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
    } else if (event.key === 'Enter' && this.currentSaleService.currentSale()?.salesLines.length > 0) {
      if (this.active !== 'comptant' && this.currentSaleService.currentSale().amountToBePaid === 0) {
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
    //   this.onProcessAddQty(qytMvt);
  }

  onAddNewQty(qytMvt: number): void {
    if (qytMvt <= 0) {
      return;
    }
    const qtyMaxToSel = this.baseSaleService.quantityMax();
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

  updateTransformedSales(): void {
    const curr = this.currentSaleService.currentSale();
    const cust = this.selectedCustomerService.selectedCustomerSignal();
    curr.tiersPayants = cust.tiersPayants;
    curr.customerId = cust.id;
    curr.customer = cust;
    this.voSalesService.updateTransformedSale(curr).subscribe({
      next: () => {
        this.currentSaleService.setVoFromCashSale(false);
        this.voSalesService.find(curr.id).subscribe({
          next: res => {
            this.currentSaleService.setCurrentSale(res.body);
          },
        });
      },
      error: (err: any) => this.onCommonError(err),
    });
  }

  changeCustomer(): void {
    const curr = this.currentSaleService.currentSale();
    const oldCustomer = curr.customer;
    const cust = this.selectedCustomerService.selectedCustomerSignal();
    curr.tiersPayants = cust.tiersPayants;
    curr.customerId = cust.id;
    curr.customer = cust;
    this.voSalesService
      .changeCustomer({
        key: curr.id,
        value: cust.id,
      })
      .subscribe({
        next: () => {
          this.voSalesService.find(curr.id).subscribe({
            next: res => {
              this.currentSaleService.setCurrentSale(res.body);
            },
          });
        },
        error: (err: any) => this.onChangeCustomerError(err, oldCustomer),
      });
  }

  onQuantityBoxAction(event: any): void {
    const qytMvt = Number(event.target.value);
    this.onAddNewQty(qytMvt);
    //  this.onProcessAddQty(qytMvt);
  }

  onAddProduit(qytMvt: number): void {
    if (this.produitSelected) {
      if (this.active === 'comptant') {
        if (this.currentSaleService.currentSale()) {
          this.comptantComponent().onAddProduit(this.createSalesLine(this.produitSelected, qytMvt));
        } else {
          this.comptantComponent().createComptant(this.createSalesLine(this.produitSelected, qytMvt));
        }
      } else if (this.active === 'assurance') {
        if (this.selectedCustomerService.selectedCustomerSignal()) {
          if (this.currentSaleService.currentSale()) {
            this.assuranceComponent().onAddProduit(this.createSalesLine(this.produitSelected, qytMvt));
          } else {
            this.assuranceComponent().create(
              this.createSalesLine(this.produitSelected, qytMvt),
              this.assuranceDataComponent().buildIClientTiersPayantFromInputs(),
            );
          }
        } else {
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: 'Veuillez selectionner un client',
          });
          this.assuranceDataComponent().searchInput().nativeElement.focus();
        }
      }
    }
  }

  print(sale: ISales | null): void {
    if (sale !== null && sale !== undefined) {
      if (this.active === 'comptant') {
        this.comptantComponent().print(sale);
      } else {
        this.assuranceComponent().print(sale);
      }
      this.currentSaleService.setCurrentSale(null);
      this.selectedCustomerService.setCustomer(null);
      this.loadProduits();
    }
  }

  printSale(saleId: number): void {
    if (this.active === 'comptant') {
      this.comptantComponent().printSale(saleId);
    } else if (this.active === 'assurance') {
      this.assuranceComponent().printSale(saleId);
    }
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
                this.openInfoDialog(this.errorService.getErrorMessage(error), 'alert alert-danger');
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
    this.lastCurrencyGivenService.setLastCurrency(this.monnaie);
    this.lastCurrencyGivenService.setGivenCurrentSale(0);
    this.showAddModePaimentBtn = false;
    if (this.active !== 'comptant') {
      this.active = 'comptant';
    }
    this.updateProduitQtyBox();
    this.loadProduits();
  }

  onChangeCashSaleToVo(): void {
    this.selectedCustomerService.setCustomer(null);
    this.salesService
      .transform({
        natureVente: 'ASSURANCE',
        saleId: this.currentSaleService.currentSale().id,
      })
      .subscribe({
        next: res => {
          this.salesService.find(res.body).subscribe({
            next: res => {
              this.currentSaleService.setVoFromCashSale(true);
              this.onLoadPrevente(res.body, false);
            },
          });
        },
        error: error => {
          this.onCommonError(error);
        },
      });
  }

  onChangeCashSaleToCarnet(): void {
    this.selectedCustomerService.setCustomer(null);
    this.salesService
      .transform({
        natureVente: 'CARNET',
        saleId: this.currentSaleService.currentSale().id,
      })
      .subscribe({
        next: res => {
          this.salesService.find(res.body).subscribe({
            next: res => {
              this.currentSaleService.setVoFromCashSale(true);
              this.onLoadPrevente(res.body, false);
            },
          });
        },
        error: error => {
          this.onCommonError(error);
        },
      });
  }

  openPindingSide(): void {
    this.pendingSalesSidebar = true;
  }

  closeSideBar(booleanValue: boolean): void {
    this.pendingSalesSidebar = booleanValue;
    if (this.currentSaleService.currentSale()) {
      this.onLoadPrevente(this.currentSaleService.currentSale());
    }
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
      if (!finalyseSale.putOnStandBy) {
        if (this.printTicket) {
          this.printSale(finalyseSale.saleId);
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

  onTypePrescriptionChange(event: any): void {
    this.typePrescriptionService.setTypePrescription(event.value);
  }

  ngOnDestroy(): void {
    this.saleEventManager.destroy(this.responseEvent);
    this.saleEventManager.destroy(this.saveResponse);
    this.saleEventManager.destroy(this.inputBoxFocus);
    this.saleEventManager.destroy(this.onCompleteSale);
  }

  onNavChange(evt: NgbNavChangeEvent): void {
    const currentSale = this.currentSaleService.currentSale();
    if (currentSale) {
      evt.preventDefault();
      const message = this.getMessateOnNavChange(evt);
      this.confirmationService.confirm({
        message,
        header: 'Changement de type de vente',
        icon: 'pi pi-info-circle',
        accept: () => {
          if (evt.nextId === 'comptant') {
            if (currentSale.categorie === 'VO') {
              this.setEnAttenteAssurance();
              this.active = 'comptant';
            }
          } else if (evt.nextId === 'assurance') {
            this.onChangeCashSaleToVo();
          } else if (evt.nextId === 'carnet') {
            this.onChangeCashSaleToCarnet();
            this.active = 'carnet';
          }
        },
        reject: () => {
          evt.preventDefault();
        },
        key: 'changeTab',
      });
    }
  }

  onCustomerOverlay(evnt: boolean): void {
    this.produitbox().inputEL.nativeElement.focus();
  }

  private updateProduitQtyBox(): void {
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

  private processQtyRequested(salesLine: ISalesLine): void {
    if (this.active === 'comptant') {
      this.processQtyRequestedForVNO(salesLine);
    } else if (this.active === 'assurance') {
      this.processQtyRequestedVo(salesLine);
    }
  }

  private processQtyRequestedVo(salesLine: ISalesLine): void {
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

  private subscribeToSaveResponse(result: Observable<HttpResponse<ISales>>): void {
    result.subscribe({
      next: (res: HttpResponse<ISales>) => this.onSaveSuccess(res.body),
      error: () => this.onSaveError(),
    });
  }

  private onProduitSuccess(data: IProduit[] | null): void {
    this.produits = data || [];
  }

  private onSaveSuccess(sale: ISales | null): void {
    this.isSaving = false;
    this.sale = sale!;

    this.updateProduitQtyBox();
  }

  private processQtyRequestedForVNO(salesLine: ISalesLine): void {
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

  private onSaveError(): void {
    this.isSaving = false;
    const message = 'Une erreur est survenue';
    this.openInfoDialog(message, 'alert alert-danger');
  }

  private onStockError(salesLine: ISalesLine, error: any): void {
    if (error.error) {
      if (error.error.errorKey === 'stock') {
        if (this.canForceStock) {
          salesLine.forceStock = true;
          this.onUpdateConfirmForceStock(
            salesLine,
            'La quantité saisie est supérieure à la quantité stock du produit. Voullez-vous continuer ?',
          );
        } else {
          this.openInfoDialog(this.errorService.getErrorMessage(error), 'alert alert-danger');
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

  private onStockOutError(stockError: StockError): void {
    const error = stockError.err.error;
    const salesLine = stockError.saleLine;
    if (error) {
      if (error.error.errorKey === 'stock') {
        if (this.canForceStock) {
          salesLine.forceStock = true;
          this.onUpdateConfirmForceStock(
            salesLine,
            'La quantité saisie est supérieure à la quantité stock du produit. Voullez-vous continuer ?',
          );
        } else {
          this.openInfoDialog(this.errorService.getErrorMessage(error), 'alert alert-danger');
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

  private onCommonError(error: any): void {
    if (error.status === 412) {
      this.updateProduitQtyBox();
      if (!this.currentSaleService.plafondIsReached()) {
        this.currentSaleService.setPlafondIsReached(true);
        this.openInfoDialog(this.errorService.getErrorMessage(error), 'alert alert-danger');
      }
      if (this.currentSaleService.voFromCashSale()) {
        this.currentSaleService.setVoFromCashSale(false);
      }
    } else {
      this.openInfoDialog(this.errorService.getErrorMessage(error), 'alert alert-danger');
    }
  }

  private onChangeCustomerError(error: any, old: ICustomer): void {
    if (error.status === 412) {
      this.updateProduitQtyBox();
      if (!this.currentSaleService.plafondIsReached()) {
        this.currentSaleService.setPlafondIsReached(true);
        this.openInfoDialog(this.errorService.getErrorMessage(error), 'alert alert-danger');
      }
    } else {
      this.selectedCustomerService.setCustomer(old);
      this.openInfoDialog(this.errorService.getErrorMessage(error), 'alert alert-danger');
    }
  }

  private checkEmptyBon(): boolean {
    if (this.isPresale) {
      return false;
    }
    if (this.currentSaleService.currentSale() && this.currentSaleService.isVenteSansBon()) {
      return false;
    }
    const emptyBon = (element: IClientTiersPayant) =>
      element.numBon === undefined || element.numBon === null || element.numBon?.trim() === '';
    return this.currentSaleService.currentSale().tiersPayants?.some(emptyBon);
  }

  private getMessateOnNavChange(evt: NgbNavChangeEvent): string {
    let message = '';
    if (evt.nextId === 'comptant') {
      message = 'La vente sera mise en attente. Voulez-vous continuer ? ';
    } else if (evt.nextId === 'assurance' || evt.nextId === 'carnet') {
      message = 'La vente sera transformée en vente assurance. Voulez-vous continuer ? ';
    }
    return message;
  }

  private loadPrevente(): void {
    setTimeout(() => {
      if (this.sale?.payments?.length > 0) {
        if (this.active === 'comptant') {
          this.comptantComponent().onLoadPrevente();
        } else if (this.active === 'assurance') {
          this.assuranceComponent().onLoadPrevente();
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
    if (this.active === 'comptant') {
      this.comptantComponent().printInvoice();
    } else {
      this.assuranceComponent().printInvoice();
    }
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
