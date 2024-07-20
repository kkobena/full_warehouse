import { Component, effect, ElementRef, EventEmitter, inject, Input, Output, viewChild } from '@angular/core';
import { AutoCompleteModule } from 'primeng/autocomplete';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogModule } from 'primeng/dialog';
import { DividerModule } from 'primeng/divider';
import { DropdownModule } from 'primeng/dropdown';
import { InputSwitchModule } from 'primeng/inputswitch';
import { InputTextModule } from 'primeng/inputtext';
import { KeyFilterModule } from 'primeng/keyfilter';
import { ConfirmationService } from 'primeng/api';
import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';
import { WarehouseCommonModule } from '../../../../shared/warehouse-common/warehouse-common.module';
import { PreventeModalComponent } from '../../prevente-modal/prevente-modal/prevente-modal.component';
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
import { UninsuredCustomerListComponent } from '../../uninsured-customer-list/uninsured-customer-list.component';
import { ProductTableComponent } from '../product-table/product-table.component';
import { IPaymentMode, PaymentModeControl } from '../../../../shared/model/payment-mode.model';
import { IPayment, Payment } from '../../../../shared/model/payment.model';
import { IRemise } from '../../../../shared/model/remise.model';
import { FinalyseSale, InputToFocus, ISales, Sales, SaveResponse } from '../../../../shared/model/sales.model';
import { ISalesLine } from '../../../../shared/model/sales-line.model';
import { Observable } from 'rxjs';
import { SalesService } from '../../sales.service';
import { CustomerService } from '../../../customer/customer.service';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { ErrorService } from '../../../../shared/error.service';
import { TranslateService } from '@ngx-translate/core';
import { HttpResponse } from '@angular/common/http';
import { AlertInfoComponent } from '../../../../shared/alert/alert-info.component';
import { AmountComputingComponent } from './amount-computing/amount-computing.component';
import { ModeReglementComponent } from '../../mode-reglement/mode-reglement.component';
import { CurrentSaleService } from '../../service/current-sale.service';
import { SelectModeReglementService } from '../../service/select-mode-reglement.service';
import { SelectedCustomerService } from '../../service/selected-customer.service';
import { TypePrescriptionService } from '../../service/type-prescription.service';
import { UserCaissierService } from '../../service/user-caissier.service';
import { UserVendeurService } from '../../service/user-vendeur.service';

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
  @Input('isPresale') isPresale = false;
  @Input('canUpdatePu') canUpdatePu: boolean = false;
  @Input('canForceStock') canForceStock: boolean = true;
  readonly appendTo = 'body';
  @Output() inputToFocusEvent = new EventEmitter<InputToFocus>();
  @Output('saveResponse') saveResponse = new EventEmitter<SaveResponse>();
  @Output('responseEvent') responseEvent = new EventEmitter<FinalyseSale>();
  readonly CASH = 'CASH';
  // commonDialogModalBtn = viewChild<ElementRef>('commonDialogModalBtn');
  differeConfirmDialogBtn = viewChild<ElementRef>('differeConfirmDialogBtn');
  avoirConfirmDialogBtn = viewChild<ElementRef>('avoirConfirmDialogBtn');
  amountComputingComponent = viewChild(AmountComputingComponent);
  modeReglementComponent = viewChild(ModeReglementComponent);
  //forcerStockBtn = viewChild<ElementRef>('forcerStockBtn');
  selectedCustomerService = inject(SelectedCustomerService);
  typePrescriptionService = inject(TypePrescriptionService);
  userCaissierService = inject(UserCaissierService);
  userVendeurService = inject(UserVendeurService);
  // addModePaymentConfirmDialogBtn = viewChild<ElementRef>('addModePaymentConfirmDialogBtn');
  protected isSaving = false;
  // protected displayErrorModal = false;

  protected displayErrorEntryAmountModal = false;
  protected payments: IPayment[] = [];
  protected modeReglementSelected: IPaymentMode[] = [];
  protected ref: DynamicDialogRef;
  protected remises: IRemise[] = [];
  protected remise?: IRemise | null;
  protected isDiffere: boolean = false;
  protected sale?: ISales | null = null;
  protected event: any;
  protected entryAmount?: number | null = null;

  constructor(
    protected selectModeReglementService: SelectModeReglementService,
    protected salesService: SalesService,
    protected currentSaleService: CurrentSaleService,
    protected customerService: CustomerService,
    protected activatedRoute: ActivatedRoute,
    protected router: Router,
    protected modalService: NgbModal,
    protected confirmationService: ConfirmationService,
    protected errorService: ErrorService,
    private dialogService: DialogService,
    public translate: TranslateService,
  ) {
    effect(() => {
      this.sale = this.currentSaleService.currentSale();
      this.isDiffere = this.sale?.differe;
    });
    effect(() => {
      this.modeReglementSelected = this.selectModeReglementService.modeReglements();
    });
  }

  manageAmountDiv(): void {
    this.modeReglementComponent().manageAmountDiv();
  }

  onHidedisplayErrorEntryAmountModal(event: Event): void {
    // this.montantCashInput?.nativeElement.focus();
  }

  differeConfirmDialog(): void {
    this.confirmationService.confirm({
      message: 'Voullez-vous regler le reste en différé ?',
      header: 'Vente différé',
      icon: 'pi pi-info-circle',
      accept: () => {
        if (!this.sale.customerId) {
          this.openUninsuredCustomer(true);
        } else {
          this.sale.differe = true;
          this.isDiffere = true;
          this.finalyseSale();
        }
      },
      reject: () => {},
      key: 'differeConfirmDialog',
    });

    setTimeout(() => {
      this.differeConfirmDialogBtn().nativeElement.focus();
    }, 10);
  }

  onOpenCustomer(putsOnStandby: boolean = false): void {
    this.confirmationService.confirm({
      message: 'Vous devez ajouter un client à la vente ?',
      header: 'Vente en avoir',
      icon: 'pi pi-info-circle',
      accept: () => {
        this.openUninsuredCustomer(false, putsOnStandby);
      },

      key: 'avoirConfirmDialog',
    });

    setTimeout(() => {
      this.avoirConfirmDialogBtn().nativeElement.focus();
    }, 10);
  }

  computExtraInfo(): void {
    this.sale.commentaire = this.modeReglementComponent().commentaire;
  }

  finalyseSale(putsOnStandby: boolean = false): void {
    const entryAmount = this.getEntryAmount();
    this.sale.payments = this.buildPayment(entryAmount);
    this.sale.type = 'VNO';
    this.sale.avoir = this.isAvoir();
    this.computExtraInfo();
    if (this.sale.avoir && !this.sale.customerId) {
      this.onOpenCustomer(putsOnStandby);
    } else {
      if (this.isPresale || putsOnStandby) {
        this.putCurrentCashSaleOnHold();
      } else {
        this.saveCashSale();
      }
    }
  }

  putCurrentSaleOnStandBy(): void {
    this.finalyseSale(true);
  }

  getCashAmount(): number {
    const modes = this.selectModeReglementService.modeReglements();
    let cashInput;
    this.entryAmount = this.getEntryAmount();
    if (modes.length > 0) {
      cashInput = modes.find((input: IPaymentMode) => input.code === this.CASH);
      if (cashInput) {
        return cashInput.amount;
      }
      return 0;
    } else {
      cashInput = modes[0];
      if (cashInput.code === this.CASH) {
        return cashInput.amount;
      }
      return 0;
    }
  }

  onKeyDown(event: any): void {
    this.save();
  }

  isValidDiffere(): boolean {
    return this.sale.differe /*&& !this.sale.customerId*/;
  }

  save(): void {
    this.isSaving = true;
    // this.sale.differe = this.isDiffere;
    const restToPay = this.sale.amountToBePaid - this.getEntryAmount();

    //   this.sale.montantRendu = this.monnaie;
    this.sale.montantVerse = this.getCashAmount();
    if (restToPay > 0 && !this.isValidDiffere()) {
      this.differeConfirmDialog();
    } else {
      this.finalyseSale();
    }
  }

  saveCashSale(): void {
    const entryAmount = this.getEntryAmount();
    const restToPay = this.sale.amountToBePaid - entryAmount;
    if (restToPay <= 0) {
      this.sale.payrollAmount = this.sale.amountToBePaid;
      this.sale.restToPay = 0;
    } else {
      this.sale.payrollAmount = entryAmount;
      this.sale.restToPay = restToPay;
    }
    this.sale.montantRendu = this.sale.montantVerse - this.sale.amountToBePaid;
    this.subscribeToFinalyseResponse(this.salesService.saveCash(this.sale));
  }

  canceldisplayErrorEntryAmountModal(): void {
    this.displayErrorEntryAmountModal = false;
  }

  putCurrentCashSaleOnHold(): void {
    this.subscribeToPutOnHoldResponse(this.salesService.putCurrentCashSaleOnStandBy(this.sale));
  }

  createComptant(salesLine: ISalesLine): void {
    this.subscribeToCreateSaleComptantResponse(this.salesService.createComptant(this.createSaleComptant(salesLine)));
  }

  onAddProduit(salesLine: ISalesLine): void {
    this.subscribeToSaveLineResponse(this.salesService.addItemComptant(salesLine));
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

  buildPayment(entryAmount: number): IPayment[] {
    return this.selectModeReglementService
      .modeReglements()
      .filter((m: IPaymentMode) => m.amount)
      .map((mode: IPaymentMode) => this.buildModePayment(mode, entryAmount));
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

  getEntryAmount(): number {
    return this.modeReglementComponent().getInputSum();
  }

  manageCashPaymentMode(paymentModeControl: PaymentModeControl): void {
    const modes = this.selectModeReglementService.modeReglements();
    if (modes.length === 2) {
      const amount = this.getEntryAmount();
      modes.find((e: IPaymentMode) => e.code !== paymentModeControl.control.target.id).amount =
        this.sale.amountToBePaid - paymentModeControl.paymentMode.amount;

      this.amountComputingComponent().computeMonnaie(amount);
    } else {
      this.amountComputingComponent().computeMonnaie(Number(paymentModeControl.control.target.value));
    }
    this.modeReglementComponent().showAddModePaymentButton(paymentModeControl.paymentMode);
  }

  openUninsuredCustomer(isVenteDefferee: boolean, putsOnStandby: boolean = false): void {
    this.ref = this.dialogService.open(UninsuredCustomerListComponent, {
      header: 'CLIENTS NON ASSURES',
      width: '60%',
      closeOnEscape: false,
    });
    this.ref.onDestroy.subscribe(() => {
      if (isVenteDefferee) {
        this.sale.differe = true;
        this.isDiffere = true;
        this.modeReglementComponent().commentaireInputGetFocus();
      } else {
        this.finalyseSale(putsOnStandby);
      }
    });
  }

  onLoadPrevente(): void {
    this.modeReglementComponent().buildPreventeReglementInput();
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<ISales>>): void {
    result.subscribe({
      next: (res: HttpResponse<ISales>) => this.onSaveSuccess(res.body),
      error: error => this.onSaveError(error),
    });
  }

  protected onSaveSuccess(sale: ISales | null): void {
    this.isSaving = false;
    this.currentSaleService.setCurrentSale(sale);
    this.saveResponse.emit({ success: true });
    this.amountComputingComponent().computeMonnaie(null);
  }

  protected onSaveError(err: any): void {
    this.isSaving = false;
    this.saveResponse.emit({ success: true, error: err });
    /*const message = 'Une erreur est survenue';
    this.openInfoDialog(message, 'alert alert-danger');*/
  }

  protected onFinalyseError(err: any): void {
    this.isSaving = false;
    this.responseEvent.emit({ error: err, success: false });
    const message = 'Une erreur est survenue';
    this.openInfoDialog(message, 'alert alert-danger');
  }

  protected onFinalyseSuccess(response: FinalyseSale | null, putOnStandBy: boolean = false): void {
    this.isSaving = false;
    this.responseEvent.emit({ saleId: response.saleId, success: true, putOnStandBy });
  }

  protected subscribeToFinalyseResponse(result: Observable<HttpResponse<FinalyseSale>>): void {
    result.subscribe({
      next: (res: HttpResponse<FinalyseSale>) => this.onFinalyseSuccess(res.body),
      error: err => this.onFinalyseError(err),
    });
  }

  protected subscribeToPutOnHoldResponse(result: Observable<HttpResponse<FinalyseSale>>): void {
    result.subscribe({
      next: (res: HttpResponse<FinalyseSale>) => this.onFinalyseSuccess(res.body, true),
      error: err => this.onFinalyseError(err),
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

  private createSaleComptant(salesLine: ISalesLine): ISales {
    let currentCustomer = this.selectedCustomerService.selectedCustomerSignal();
    if (currentCustomer && currentCustomer.type === 'ASSURE') {
      currentCustomer = null;
    }
    return {
      ...new Sales(),
      salesLines: [salesLine],
      customerId: currentCustomer?.id,
      natureVente: 'COMPTANT',
      typePrescription: this.typePrescriptionService.typePrescription()?.code,
      cassierId: this.userCaissierService.caissier()?.id,
      sellerId: this.userVendeurService.vendeur()?.id,
      type: 'VNO',
      categorie: 'VNO',
    };
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

  private buildModePayment(mode: IPaymentMode, entryAmount: number): Payment {
    const amount = this.sale.amountToBePaid - (entryAmount - mode.amount);
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

  private isAvoir(): boolean {
    return this.getTotalQtyProduit() - this.getTotalQtyServi() != 0;
  }

  private getTotalQtyProduit(): number {
    return this.sale.salesLines.reduce((sum, current) => sum + current.quantityRequested, 0);
  }

  private getTotalQtyServi(): number {
    return this.sale.salesLines.reduce((sum, current) => sum + current.quantitySold, 0);
  }
}
