import { AfterViewInit, Component, effect, ElementRef, inject, OnDestroy, OnInit, viewChild } from '@angular/core';
import { AutoComplete } from 'primeng/autocomplete';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { ConfirmationService, MessageService } from 'primeng/api';
import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { PreventeModalComponent } from '../prevente-modal/prevente-modal/prevente-modal.component';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { NgxSpinnerModule } from 'ngx-spinner';
import { FormsModule } from '@angular/forms';
import { PanelModule } from 'primeng/panel';
import { TooltipModule } from 'primeng/tooltip';
import { INatureVente } from '../../../shared/model/nature-vente.model';
import { IUser } from '../../../core/user/user.model';
import { ITypePrescription } from '../../../shared/model/prescription-vente.model';
import { ICustomer } from '../../../shared/model/customer.model';
import { IProduit } from '../../../shared/model/produit.model';
import { GroupRemise, IRemise } from '../../../shared/model/remise.model';
import { FinalyseSale, InputToFocus, ISales, SaveResponse, StockError } from '../../../shared/model/sales.model';
import { ISalesLine, SalesLine } from '../../../shared/model/sales-line.model';
import { PRODUIT_COMBO_MIN_LENGTH, PRODUIT_NOT_FOUND } from '../../../shared/constants/pagination.constants';
import { Observable, Subscription } from 'rxjs';
import { SalesService } from '../sales.service';
import { CustomerService } from '../../customer/customer.service';
import { ProduitService } from '../../produit/produit.service';
import { NgbModal, NgbNavChangeEvent } from '@ng-bootstrap/ng-bootstrap';
import { AccountService } from '../../../core/auth/account.service';
import { ErrorService } from '../../../shared/error.service';
import { DeconditionService } from '../../decondition/decondition.service';
import { TranslateService } from '@ngx-translate/core';
import { Decondition, IDecondition } from '../../../shared/model/decondition.model';
import { HttpResponse } from '@angular/common/http';
import { AlertInfoComponent } from '../../../shared/alert/alert-info.component';
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
import { SaleEvent, SaleEventManager } from '../service/sale-event-manager.service';
import { VoSalesService } from '../service/vo-sales.service';
import { HasAuthorityService } from '../service/has-authority.service';
import { ToastModule } from 'primeng/toast';
import { IClientTiersPayant } from '../../../shared/model/client-tiers-payant.model';
import { BaseSaleService } from '../service/base-sale.service';
import { CarnetComponent } from './carnet/carnet.component';
import { Authority } from '../../../shared/constants/authority.constants';
import { RemiseCacheService } from '../service/remise-cache.service';
import { PrimeNG } from 'primeng/config';
import { acceptButtonProps, rejectButtonProps } from '../../../shared/util/modal-button-props';
import { Select } from 'primeng/select';
import { InputGroupAddonModule } from 'primeng/inputgroupaddon';
import { DrawerModule } from 'primeng/drawer';

@Component({
  selector: 'jhi-selling-home',
  providers: [ConfirmationService, DialogService, MessageService],
  imports: [
    WarehouseCommonModule,
    PreventeModalComponent,
    RouterModule,
    NgxSpinnerModule,
    InputTextModule,
    ButtonModule,
    FormsModule,
    DialogModule,
    ConfirmDialogModule,
    PanelModule,
    TooltipModule,
    CardModule,
    ComptantComponent,
    CustomerOverlayPanelComponent,
    InputGroupModule,
    AssuranceComponent,
    AssuranceDataComponent,
    ToastModule,
    CarnetComponent,
    Select,
    InputGroupAddonModule,
    AutoComplete,
    DrawerModule,
  ],
  templateUrl: './selling-home.component.html',
})
export class SellingHomeComponent implements OnInit, AfterViewInit, OnDestroy {
  readonly minLength = PRODUIT_COMBO_MIN_LENGTH;
  readonly COMPTANT = 'COMPTANT';
  readonly CARNET = 'CARNET';
  readonly ASSURANCE = 'ASSURANCE';
  readonly notFoundText = PRODUIT_NOT_FOUND;
  quantyBox = viewChild<ElementRef>('quantyBox');
  comptantComponent = viewChild(ComptantComponent);
  assuranceComponent = viewChild(AssuranceComponent);
  carnetComponent = viewChild(CarnetComponent);
  assuranceDataComponent = viewChild(AssuranceDataComponent);
  produitbox = viewChild<any>('produitbox');
  userBox = viewChild<any>('userBox');
  accountService = inject(AccountService);
  currentAccount = this.accountService.trackCurrentAccount();
  remiseCacheService = inject(RemiseCacheService);
  remises: GroupRemise[] = this.remiseCacheService.remises();
  protected isLargeScreen = true;
  protected canForceStock: boolean;
  protected check = true; // mis pour le focus produit et dialogue button
  protected naturesVentes: INatureVente[] = [];
  protected naturesVente: INatureVente | null = null;
  protected userCaissier?: IUser | null;
  protected userSeller?: IUser;
  protected typePrescription?: ITypePrescription | null;
  protected customers: ICustomer[] = [];
  protected produits: IProduit[] = [];
  protected produitSelected?: IProduit | null = null;
  protected searchValue?: string;
  protected appendTo = 'body';
  protected imagesPath!: string;
  protected remise: IRemise[] = [];
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
  protected printTicket = true;
  protected active = 'comptant';
  protected currentSaleService = inject(CurrentSaleService);
  protected userVendeurService = inject(UserVendeurService);
  private typePrescriptionService = inject(TypePrescriptionService);
  private userCaissierService = inject(UserCaissierService);
  private hasAuthorityService = inject(HasAuthorityService);
  private messageService = inject(MessageService);
  private voSalesService = inject(VoSalesService);
  private baseSaleService = inject(BaseSaleService);
  private selectModeReglementService = inject(SelectModeReglementService);
  private selectedCustomerService = inject(SelectedCustomerService);
  private lastCurrencyGivenService = inject(LastCurrencyGivenService);
  private salesService = inject(SalesService);
  private customerService = inject(CustomerService);
  private produitService = inject(ProduitService);
  private activatedRoute = inject(ActivatedRoute);
  private router = inject(Router);
  private modalService = inject(NgbModal);
  private confirmationService = inject(ConfirmationService);
  private errorService = inject(ErrorService);
  private decondtionService = inject(DeconditionService);
  private translate = inject(TranslateService);
  private primeNGConfig = inject(PrimeNG);
  private readonly responseEvent: Subscription;
  private readonly saveResponse: Subscription;
  private readonly inputBoxFocus: Subscription;
  private readonly onCompleteSale: Subscription;
  private readonly saleEventManager = inject(SaleEventManager);
  private readonly quantityMessage = 'La quantité saisie est supérieure à la quantité stock du produit';

  constructor() {
    this.canForceStock = this.hasAuthorityService.hasAuthorities(Authority.PR_FORCE_STOCK);

    this.onCompleteSale = this.saleEventManager.subscribe('completeSale', (response: SaleEvent<unknown>) => {
      if (this.isAssurance() || this.isCartnet()) {
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
          if (this.currentSaleService.currentSale().type === 'VNO' && this.currentSaleService.currentSale().customerId) {
            this.onRemoveCustomer();
          }
        }
      }
    });
  }

  onRemoveCustomer(): void {
    if (this.isComptant()) {
      this.salesService.removeCustommerToCashSale(this.currentSaleService.currentSale().id).subscribe(() => {});
    }
  }

  onAddCustommer(): void {
    if (this.isComptant()) {
      this.salesService
        .addCustommerToCashSale({
          key: this.currentSaleService.currentSale().id,
          value: this.selectedCustomerService.selectedCustomerSignal().id,
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
      if (sales && sales.type === 'VNO') {
        this.active = 'comptant';
      } else if (sales && sales.type === 'VO') {
        if (sales.natureVente === 'CARNET') {
          this.active = 'carnet';
        } else {
          this.active = 'assurance';
        }
      }

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

    this.activatedRoute.data.subscribe(({ sales, mode }) => {
      if (sales.id) {
        if (sales.customer) {
          this.customerService
            .find(sales.customer.id)
            .subscribe({ next: (resp: HttpResponse<ICustomer>) => this.selectedCustomerService.setCustomer(resp.body) });
        }
        this.onLoadPrevente(sales, this.isEditionClosedSale(mode));
      }
      //  this.loadProduits();
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
        this.confirmationService.confirm({
          message: 'Voullez-vous continuer la vente sans numéro de bon ?',
          header: 'Vente sans numéro de bon',
          icon: 'pi pi-info-circle',
          rejectButtonProps: rejectButtonProps(),
          acceptButtonProps: acceptButtonProps(),
          accept: () => {
            if (this.isAssurance()) {
              this.assuranceComponent().save();
            } else if (this.isCartnet()) {
              this.carnetComponent().save();
            }
          },
        });
      } else {
        this.messageService.add({
          severity: 'error',
          summary: 'Alerte',
          detail: 'Veuillez saisir le numéro de bon pour tous les tiers payants',
        });
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

  onHideHideDialog(): void {}

  cancelCommonDialog(): void {
    this.commonDialog = false;
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
    } else if (event.key === 'Enter' && this.currentSaleService.currentSale().salesLines.length > 0) {
      if (this.isVoSale() && this.currentSaleService.currentSale().amountToBePaid === 0) {
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
      return this.currentSaleService.currentSale()?.salesLines.find(e => e.produitId === this.produitSelected.id)?.quantityRequested || 0;
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
      console.log('inStock', inStock, 'currentStock', currentStock, 'qytMvt', qytMvt, 'qtyAlreadyRequested', qtyAlreadyRequested);
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
                this.showError(this.quantityMessage);
                //    this.openInfoDialog('La quantité saisie est supérieure à la quantité stock du produit', 'alert alert-danger');
              }
            });
          } else {
            this.confirmForceStock(qytMvt, ' La quantité saisie est supérieure à la quantité stock du produit. Voullez-vous continuer ?');
          }
        } else {
          console.log('onAddNewQty', qytMvt, qtyMaxToSel);
          this.showError(this.quantityMessage);
          // this.openInfoDialog('La quantité saisie est supérieure à la quantité stock du produit', 'alert alert-danger');
        }
      } else {
        if (qytMvt >= qtyMaxToSel) {
          if (this.canForceStock) {
            this.confirmForceStock(qytMvt, ' La quantité saisie est supérieure à maximale à vendre. Voullez-vous continuer ?');
          } else {
            this.showError('La quantité saisie est supérieure à maximale à vendre');
            //  this.openInfoDialog('La quantité saisie est supérieure à maximale à vendre', 'alert alert-danger');
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
    console.log('onQuantityBoxAction', qytMvt);
    this.onAddNewQty(qytMvt);
    //  this.onProcessAddQty(qytMvt);
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
                this.assuranceDataComponent().buildIClientTiersPayantFromInputs(),
              );
            }
          } else if (this.isCartnet()) {
            if (this.currentSaleService.currentSale()) {
              this.carnetComponent().onAddProduit(this.createSalesLine(this.produitSelected, qytMvt));
            } else {
              this.carnetComponent().create(
                this.createSalesLine(this.produitSelected, qytMvt),
                this.assuranceDataComponent().buildIClientTiersPayantFromInputs(),
              );
            }
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
      //  this.loadProduits();
    }
  }

  printSale(saleId: number): void {
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
    this.messageService.add({
      severity: 'error',
      summary: 'Alerte',
      detail: message,
    });
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
      rejectButtonProps: rejectButtonProps(),
      acceptButtonProps: acceptButtonProps(),
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
      rejectButtonProps: rejectButtonProps(),
      acceptButtonProps: acceptButtonProps(),
      accept: () => {
        this.processQtyRequested(salesLine);
      },
      reject: () => {
        this.check = true;
        if (this.isComptant()) {
          this.subscribeToSaveResponse(this.salesService.find(this.currentSaleService.currentSale().id));
        } else if (this.isVoSale()) {
          this.subscribeToSaveResponse(this.voSalesService.find(this.currentSaleService.currentSale().id));
        }
        this.updateProduitQtyBox();
      },
    });
  }

  resetAll(): void {
    this.currentSaleService.reset();
    if (this.assuranceDataComponent()) {
      this.assuranceDataComponent().reset();
    }
    this.searchValue = '';
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
    if (!this.isComptant()) {
      this.active = 'comptant';
    }
    this.updateProduitQtyBox();
    // this.loadProduits();
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
              if (res.body.customer) {
                this.customerService
                  .find(res.body.customer.id)
                  .subscribe({ next: (resp: HttpResponse<ICustomer>) => this.selectedCustomerService.setCustomer(resp.body) });
              }
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
              if (res.body.customer) {
                this.customerService
                  .find(res.body.customer.id)
                  .subscribe({ next: (resp: HttpResponse<ICustomer>) => this.selectedCustomerService.setCustomer(resp.body) });
              }
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
      this.produitbox().inputEL.nativeElement.focus();
      if (this.quantyBox()) {
        this.quantyBox().nativeElement.value = 1;
      }
    }
  }

  getToolBarCssClass(): string {
    const cust = this.selectedCustomerService.selectedCustomerSignal();
    let css = 'col-md-5';
    if (this.isComptant() && cust) {
      css = 'col-md-4';
    } else if (this.isComptant() && !cust && !this.currentSaleService.currentSale()) {
      css = 'col-md-7';
    }
    return css;
  }

  getToolBarActionCssClass(): string {
    let css = 'col-md-6';
    if (this.isComptant() && !this.currentSaleService.currentSale()) {
      css = 'col-md-5';
    }
    if (this.isComptant() && this.currentSaleService.currentSale()) {
      css = 'col-md-2';
    }
    return css;
  }

  getToolBarCustomerCssClass(): string {
    const css = 'col-md-6';
    if (this.currentSaleService.currentSale() && !this.selectedCustomerService.selectedCustomerSignal()) {
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
    // eslint-disable-next-line @typescript-eslint/no-unnecessary-condition
    if (currentSale) {
      evt.preventDefault();
      const message = this.getMessateOnNavChange(evt);
      this.confirmationService.confirm({
        message,
        header: 'Changement de type de vente',
        icon: 'pi pi-info-circle',
        rejectButtonProps: rejectButtonProps(),
        acceptButtonProps: acceptButtonProps(),
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
        reject() {
          evt.preventDefault();
        },
      });
    }
  }

  onCustomerOverlay(evnt: boolean): void {
    this.produitbox().inputEL.nativeElement.focus();
  }

  private isEditionClosedSale(mode: string): boolean {
    return mode === 'edit';
  }

  private isCartnet(): boolean {
    return this.active === 'carnet';
  }

  private isAssurance(): boolean {
    return this.active === 'assurance';
  }

  private isComptant(): boolean {
    return this.active === 'comptant';
  }

  private updateProduitQtyBox(): void {
    if (this.quantyBox()) {
      this.quantyBox().nativeElement.value = 1;
    }
    if (this.check) {
      this.produitbox().inputEL.nativeElement.focus();
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
          this.subscribeToSaveResponse(this.voSalesService.find(this.currentSaleService.currentSale().id));
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

  private onProduitSuccess(data: IProduit[] | null): void {
    this.produits = data || [];
    if (this.produits.length === 1) {
      this.produitSelected = this.produits[0];
      this.produitbox().hide();
      this.onSelect();
    }
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
          this.subscribeToSaveResponse(this.salesService.find(this.currentSaleService.currentSale().id));
        }
        this.check = true;
      },
      error: error => {
        this.check = false;
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
          if (this.isComptant()) {
            this.subscribeToSaveResponse(this.salesService.find(this.currentSaleService.currentSale().id));
          } else if (this.isVoSale()) {
            this.subscribeToSaveResponse(this.voSalesService.find(this.currentSaleService.currentSale().id));
          }
        }
      } else if (error.error.errorKey === 'stockChInsufisant') {
        this.produitService.find(Number(error.error.payload)).subscribe(res => {
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
    const salesLine = stockError.saleLine;
    this.onStockError(salesLine, stockError.err);
  }

  private onCommonError(error: any): void {
    if (error.error.status === 412) {
      this.updateProduitQtyBox();
      if (!this.currentSaleService.plafondIsReached()) {
        this.currentSaleService.setPlafondIsReached(true);
        this.openInfoDialog(this.errorService.getErrorMessage(error.error), 'alert alert-danger');
      }
      if (this.currentSaleService.voFromCashSale()) {
        this.currentSaleService.setVoFromCashSale(false);
      }
    } else {
      this.openInfoDialog(this.errorService.getErrorMessage(error.error), 'alert alert-danger');
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
      element.numBon === undefined || element.numBon === null || element.numBon.trim() === '';
    return this.currentSaleService.currentSale().tiersPayants.some(emptyBon);
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

  private isVoSale(): boolean {
    return this.isAssurance() || this.isCartnet();
  }

  private loadPrevente(): void {
    setTimeout(() => {
      if (this.isComptant()) {
        this.comptantComponent().onLoadPrevente();
      } else if (this.isVoSale()) {
        if (this.isAssurance()) {
          this.assuranceComponent().onLoadPrevente();
        } else if (this.isCartnet()) {
          this.carnetComponent().onLoadPrevente();
        }
      }
    }, 60);
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
      saleId: this.currentSaleService.currentSale()?.id,
      quantitySold: quantityRequested,
      quantityRequested,
      sales: this.currentSaleService.currentSale(),
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

  private confirmForceStock(qytMvt: number, message: string): void {
    this.confirmationService.confirm({
      message,
      header: 'FORCER LE STOCK',
      icon: 'pi pi-info-circle',
      rejectButtonProps: rejectButtonProps(),
      acceptButtonProps: acceptButtonProps(),
      accept: () => {
        this.onAddProduit(qytMvt);
      },
      reject: () => {
        this.check = true;
        this.updateProduitQtyBox();
      },
    });
  }
}
