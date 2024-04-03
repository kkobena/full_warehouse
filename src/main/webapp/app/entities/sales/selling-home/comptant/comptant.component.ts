import { Component, effect, ElementRef, EventEmitter, Input, Output, ViewChild } from '@angular/core';
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
import { WarehouseCommonModule } from '../../../../shared/warehouse-common/warehouse-common.module';
import { PreventeModalComponent } from '../../prevente-modal/prevente-modal/prevente-modal.component';
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
import { UninsuredCustomerListComponent } from '../../uninsured-customer-list/uninsured-customer-list.component';
import { ProductTableComponent } from '../product-table/product-table.component';
import { INatureVente } from '../../../../shared/model/nature-vente.model';
import { IUser } from '../../../../core/user/user.model';
import { IPaymentMode, PaymentModeControl } from '../../../../shared/model/payment-mode.model';
import { IPayment, Payment } from '../../../../shared/model/payment.model';
import { ICustomer } from '../../../../shared/model/customer.model';
import { IProduit } from '../../../../shared/model/produit.model';
import { IRemiseProduit } from '../../../../shared/model/remise-produit.model';
import { InputToFocus, ISales, SaveResponse } from '../../../../shared/model/sales.model';
import { ISalesLine } from '../../../../shared/model/sales-line.model';
import { Observable, Subscription } from 'rxjs';
import { SalesService } from '../../sales.service';
import { CustomerService } from '../../../customer/customer.service';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { AccountService } from '../../../../core/auth/account.service';
import { ErrorService } from '../../../../shared/error.service';
import { TranslateService } from '@ngx-translate/core';
import { HttpResponse } from '@angular/common/http';
import { saveAs } from 'file-saver';
import { AlertInfoComponent } from '../../../../shared/alert/alert-info.component';
import { IResponseDto } from '../../../../shared/util/response-dto';
import { Decondition, IDecondition } from '../../../../shared/model/decondition.model';
import { AmountComputingComponent } from './amount-computing/amount-computing.component';
import { ModeReglementComponent } from '../../mode-reglement/mode-reglement.component';
import { CurrentSaleService } from '../../service/current-sale.service';

type SelectableEntity = ICustomer | IProduit;

@Component({
  selector: 'jhi-comptant',
  standalone: true,
  providers: [ConfirmationService, DialogService],
  styles: [
    `
      .table tr:hover {
        cursor: pointer;
      }
    `,
  ],
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
    ProductTableComponent,
    AmountComputingComponent,
    ModeReglementComponent,
  ],
  templateUrl: './comptant.component.html',
})
export class ComptantComponent {
  isSaving = false;
  showInfosCustomer: boolean = false;
  isPresale = false;
  displayErrorModal = false;
  commonDialog = false;
  displayErrorEntryAmountModal = false;
  @Input('canUpdatePu') canUpdatePu: boolean = false;
  isDiffere = false;
  printTicket = true;
  printInvoice = false;
  @Input('canForceStock') canForceStock: boolean = true;
  payments: IPayment[] = [];
  modeReglementSelected: IPaymentMode[] = [];
  @Input('userCaissier') userCaissier?: IUser | null;
  @Input('userSeller') userSeller?: IUser;
  customers: ICustomer[] = [];
  searchValue?: string;
  appendTo = 'body';
  imagesPath!: string;
  selectedRowIndex?: number;
  remiseProduits: IRemiseProduit[] = [];
  remiseProduit?: IRemiseProduit | null;
  @Output() inputToFocusEvent = new EventEmitter<InputToFocus>();
  @Output('saveResponse') saveResponse = new EventEmitter<SaveResponse>();
  sale?: ISales | null = null;
  base64!: string;
  event: any;
  @ViewChild('clientSearchBox')
  clientSearchBox?: ElementRef;
  @Input('qtyMaxToSel') qtyMaxToSel: number;
  @ViewChild('forcerStockBtn')
  forcerStockBtn?: ElementRef;
  @ViewChild('addModePaymentConfirmDialogBtn')
  addModePaymentConfirmDialogBtn?: ElementRef;
  entryAmount?: number | null = null;
  commentaire?: string;
  telephone?: string;
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
  @ViewChild('clientSearchModalBtn', { static: false })
  clientSearchModalBtn?: ElementRef;
  @ViewChild('errorEntryAmountBtn', { static: false })
  errorEntryAmountBtn?: ElementRef;
  @ViewChild('commonDialogModalBtn', { static: false })
  commonDialogModalBtn?: ElementRef;
  ref!: DynamicDialogRef;
  primngtranslate: Subscription;
  showAddModePaimentBtn = false;
  pendingSalesSidebar = false;
  @ViewChild(AmountComputingComponent)
  amountComputingComponent?: AmountComputingComponent;
  @ViewChild(ModeReglementComponent)
  modeReglementComponent?: ModeReglementComponent;

  constructor(
    protected salesService: SalesService,
    protected currentSaleService: CurrentSaleService,
    protected customerService: CustomerService,
    protected activatedRoute: ActivatedRoute,
    protected router: Router,
    protected modalService: NgbModal,
    private accountService: AccountService,
    protected confirmationService: ConfirmationService,
    protected errorService: ErrorService,
    private dialogService: DialogService,
    public translate: TranslateService,
    public primeNGConfig: PrimeNGConfig,
    private spinner: NgxSpinnerService,
  ) {
    this.imagesPath = 'data:image/';
    this.base64 = ';base64,';
    this.selectedRowIndex = 0;
    this.searchValue = '';
    this.translate.use('fr');
    this.primngtranslate = this.translate.stream('primeng').subscribe(data => {
      this.primeNGConfig.setTranslation(data);
    });
    effect(() => {
      this.sale = this.currentSaleService.currentSale();
      //  this.showInfosCustomer = this.modeReglementSelected.some((element: IPaymentMode) => element.code !== this.CASH);
    });
  }

  onLoadPrevente(): void {}

  ngOnInit(): void {
    this.accountService.identity().subscribe(account => {
      if (account) {
        this.userCaissier = account;
      }
    });

    this.activatedRoute.data.subscribe(({ sales }) => {
      if (sales.id) {
        // this.onLoadPrevente(sales);
      }
    });
    this.activatedRoute.paramMap.subscribe(params => {
      if (params.has('isPresale')) {
        this.isPresale = params.get('isPresale') === 'true';
      }
    });
  }

  ngAfterViewInit(): void {}

  manageAmountDiv(): void {
    this.modeReglementComponent.manageAmountDiv();
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
        //  this.addOverlayPanel.toggle(this.getAddModePaymentButton());
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
      this.sale.differe = this.isDiffere;
      if (this.isPresale === false) {
        const thatentryAmount = this.getEntryAmount();

        const restToPay = this.sale.amountToBePaid - thatentryAmount;
        //   this.sale.montantRendu = this.monnaie;
        //    this.sale.montantVerse = this.getCashAmount();
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
          this.saveCashSale();
        }
      } else if (this.isPresale === true) {
        this.putCurrentSaleOnHold();
      }
    }
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
    this.putCurrentCashSaleOnHold();
  }

  putCurrentCashSaleOnHold(): void {
    this.sale.payments = this.buildPayment();
    this.sale.type = 'VNO';
    this.subscribeToPutOnHoldResponse(this.salesService.putCurrentCashSaleOnHold(this.sale));
  }

  saveAntPrint(): void {
    this.isSaving = true;
    //  this.subscribeToFinalyseResponse(this.salesService.saveComptant(this.sale!));
  }

  onSelectKeyDow(): void {
    this.save();
  }

  showClientSearch(isDiff: boolean): void {
    this.isDiffere = isDiff;
  }

  trackId(index: number, item: IProduit): number {
    // eslint-disable-next-line @typescript-eslint/no-unnecessary-type-assertion
    return item.id!;
  }

  trackById(index: number, item: SelectableEntity): any {
    return item.id;
  }

  getEntryAmount(): number {
    return this.modeReglementSelected.reduce((sum, current) => sum + Number(current.amount), 0);
  }

  clickRow(item: IProduit): void {
    this.selectedRowIndex = item.id;
  }

  createComptant(sale: ISales): void {
    this.subscribeToCreateSaleComptantResponse(this.salesService.createComptant(sale));
  }

  onAddProduit(salesLine: ISalesLine): void {
    this.subscribeToSaveLineResponse(this.salesService.addItemComptant(salesLine));
  }

  print(sale: ISales | null): void {
    if (sale !== null && sale !== undefined) {
      this.salesService.print(sale.id).subscribe(blod => saveAs(blod));
      this.sale = null;
    }
  }

  removeLine(salesLine: ISalesLine): void {
    this.removeItem(salesLine.id);
  }

  openInfoDialog(message: string, infoClass: string): void {
    const modalRef = this.modalService.open(AlertInfoComponent, {
      backdrop: 'static',
      centered: true,
    });
    modalRef.componentInstance.message = message;
    modalRef.componentInstance.infoClass = infoClass;
  }

  resetAll(): void {
    this.showAddModePaimentBtn = false;
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

  setClientSearchBoxFocus(): void {
    setTimeout(() => {
      this.clientSearchBox.nativeElement.focus();
    }, 50);
  }

  onNatureVenteChange(event: any): void {
    const selectNature = event.value;

    if (selectNature.code !== this.COMPTANT && this.sale) {
      const nature = (element: INatureVente) => element.code === this.COMPTANT;

      // TODO si vente en cours et vente est de type VO, alors message si la nvelle est COMPTANT, ON ne peut transformer une VO en VNO
    }
    if (selectNature.code !== this.COMPTANT) {
      this.setClientSearchBoxFocus();
    } else {
    }
  }

  confirmDeleteItem(item: ISalesLine): void {
    if (item) {
      this.removeLine(item);
    } else {
      //  this.check = true;
      this.inputToFocusEvent.emit({ control: 'produitBox' });
    }
  }

  updateItemQtyRequested(salesLine: ISalesLine): void {
    if (salesLine) {
      this.processQtyRequested(salesLine);
    }
  }

  updateItemQtySold(salesLine: ISalesLine): void {
    this.salesService.updateItemQtySold(salesLine).subscribe({
      next: () => this.subscribeToSaveResponse(this.salesService.find(this.sale.id)),
      error: (err: any) => this.onSaveSaveError(err, this.sale),
    });
  }

  updateItemPrice(salesLine: ISalesLine): void {
    this.processItemPrice(salesLine);
  }

  subscribeToSaveLineResponse(result: Observable<HttpResponse<ISalesLine>>): void {
    result.subscribe({
      next: (res: HttpResponse<ISalesLine>) => this.subscribeToSaveResponse(this.salesService.find(res.body.saleId)),
      error: err => this.onSaveSaveError(err, this.sale),
    });
  }

  setModeRegelementSelectionnes(modes: IPaymentMode[]): void {
    this.modeReglementSelected = modes;
    const mode = (element: IPaymentMode) => element.code !== this.CASH;
    this.showInfosCustomer = this.modeReglementSelected.some((element: IPaymentMode) => element.code !== this.CASH);
    console.log(this.showInfosCustomer, modes);
  }

  manageCashPaymentMode(paymentModeControl: PaymentModeControl): void {
    if (this.modeReglementSelected.length === 2) {
      const amount = this.getEntryAmount();
      const secondInputDefaultAmount = this.sale.amountToBePaid - paymentModeControl.paymentMode.amount; /* Number(evt.target.value)*/
      this.modeReglementSelected.find((e: IPaymentMode) => e.code !== paymentModeControl.control.target.id).amount =
        secondInputDefaultAmount;

      this.amountComputingComponent.computeMonnaie(amount);
    } else {
      this.amountComputingComponent.computeMonnaie(Number(paymentModeControl.control.target.value));
    }
    this.modeReglementComponent.showAddModePaymentButton(paymentModeControl.paymentMode);
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<ISales>>): void {
    result.subscribe({
      next: (res: HttpResponse<ISales>) => this.onSaveSuccess(res.body),
      error: () => this.onSaveError(),
    });
  }

  protected onSaveSuccess(sale: ISales | null): void {
    this.isSaving = false;
    this.currentSaleService.setCurrentSale(sale);
    this.saveResponse.emit({ success: true });
    this.amountComputingComponent.computeMonnaie(null);
  }

  protected onSaveError(): void {
    this.isSaving = false;
    const message = 'Une erreur est survenue';
    this.openInfoDialog(message, 'alert alert-danger');
  }

  protected onFinalyseSuccess(response: IResponseDto | null): void {
    this.isSaving = false;
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

  protected subscribeToCreateSaleComptantResponse(result: Observable<HttpResponse<ISales>>): void {
    result.subscribe({
      next: (res: HttpResponse<ISales>) => this.onSaleComptantResponseSuccess(res.body),
      error: error => this.onSaveSaveError(error, this.sale),
    });
  }

  protected onSaleComptantResponseSuccess(sale: ISales | null): void {
    this.isSaving = false;
    this.currentSaleService.setCurrentSale(sale);
    this.saveResponse.emit({ success: true });
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

  private onSaveSaveError(err: any, sale?: ISales): void {
    this.isSaving = false;
    this.saveResponse.emit({ success: false, error: err });
    this.currentSaleService.setCurrentSale(sale);
  }

  private processItemPrice(salesLine: ISalesLine): void {
    this.salesService.updateItemPrice(salesLine).subscribe({
      next: () => this.subscribeToSaveResponse(this.salesService.find(this.sale.id)),
      error: (err: any) => this.onSaveSaveError(err, this.sale),
    });
  }

  private createDecondition(qtyDeconditione: number, produitId: number): IDecondition {
    return {
      ...new Decondition(),
      qtyMvt: qtyDeconditione,
      produitId,
    };
  }

  private buildModePayment(mode: IPaymentMode): Payment {
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

  private removeItem(id: number): void {
    this.salesService.deleteItem(id).subscribe({
      next: () => this.subscribeToSaveResponse(this.salesService.find(this.sale.id)),
      error: (err: any) => this.onSaveSaveError(err, this.sale),
    });
  }

  private processQtyRequested(salesLine: ISalesLine): void {
    this.salesService.updateItemQtyRequested(salesLine).subscribe({
      next: () => this.subscribeToSaveResponse(this.salesService.find(this.sale.id)),
      error: (err: any) => this.onSaveSaveError(err, this.sale),
    });
  }
}
