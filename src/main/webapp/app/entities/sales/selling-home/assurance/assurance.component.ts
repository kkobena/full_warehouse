import { AfterViewInit, Component, ElementRef, inject, Input, viewChild } from '@angular/core';
import { ButtonDirective, ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogModule } from 'primeng/dialog';
import { ConfirmationService, Footer, PrimeTemplate } from 'primeng/api';
import { FinalyseSale, ISales, Sales } from '../../../../shared/model/sales.model';
import { AmountComputingComponent } from '../comptant/amount-computing/amount-computing.component';
import { ModeReglementComponent } from '../../mode-reglement/mode-reglement.component';
import { SelectedCustomerService } from '../../service/selected-customer.service';
import { TypePrescriptionService } from '../../service/type-prescription.service';
import { UserCaissierService } from '../../service/user-caissier.service';
import { UserVendeurService } from '../../service/user-vendeur.service';
import { IPayment } from '../../../../shared/model/payment.model';
import { IPaymentMode, PaymentModeControl } from '../../../../shared/model/payment-mode.model';
import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';
import { IRemise } from '../../../../shared/model/remise.model';
import { SelectModeReglementService } from '../../service/select-mode-reglement.service';
import { CurrentSaleService } from '../../service/current-sale.service';
import { CustomerService } from '../../../customer/customer.service';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { ErrorService } from '../../../../shared/error.service';
import { TranslateService } from '@ngx-translate/core';
import { ISalesLine } from '../../../../shared/model/sales-line.model';
import { Observable } from 'rxjs';
import { HttpResponse } from '@angular/common/http';
import { BaseSaleService } from '../../service/base-sale.service';
import { VoSalesService } from '../../service/vo-sales.service';
import { saveAs } from 'file-saver';
import { DividerModule } from 'primeng/divider';
import { DropdownModule } from 'primeng/dropdown';
import { WarehouseCommonModule } from '../../../../shared/warehouse-common/warehouse-common.module';
import { PreventeModalComponent } from '../../prevente-modal/prevente-modal/prevente-modal.component';
import { SidebarModule } from 'primeng/sidebar';
import { NgxSpinnerModule } from 'ngx-spinner';
import { TableModule } from 'primeng/table';
import { InputTextModule } from 'primeng/inputtext';
import { RippleModule } from 'primeng/ripple';
import { FormsModule } from '@angular/forms';
import { PanelModule } from 'primeng/panel';
import { SelectButtonModule } from 'primeng/selectbutton';
import { TooltipModule } from 'primeng/tooltip';
import { KeyFilterModule } from 'primeng/keyfilter';
import { TagModule } from 'primeng/tag';
import { InputSwitchModule } from 'primeng/inputswitch';
import { OverlayPanelModule } from 'primeng/overlaypanel';
import { ProductTableComponent } from '../product-table/product-table.component';
import { IClientTiersPayant } from '../../../../shared/model/client-tiers-payant.model';

@Component({
  selector: 'jhi-assurance',
  standalone: true,
  providers: [ConfirmationService, DialogService],
  imports: [
    ButtonDirective,
    ConfirmDialogModule,
    DialogModule,
    Footer,
    PrimeTemplate,
    AmountComputingComponent,
    DividerModule,
    DropdownModule,
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
    TooltipModule,
    DividerModule,
    KeyFilterModule,
    TagModule,
    DropdownModule,
    InputSwitchModule,
    OverlayPanelModule,
    ProductTableComponent,
    ModeReglementComponent,
  ],
  templateUrl: './assurance.component.html',
})
export class AssuranceComponent implements AfterViewInit {
  commonDialog = false;
  @Input('isPresale') isPresale = false;
  readonly appendTo = 'body';
  readonly CASH = 'CASH';
  // commonDialogModalBtn = viewChild<ElementRef>('commonDialogModalBtn');
  differeConfirmDialogBtn = viewChild<ElementRef>('differeConfirmDialogBtn');
  avoirConfirmDialogBtn = viewChild<ElementRef>('avoirConfirmDialogBtn');
  modeReglementComponent = viewChild(ModeReglementComponent);
  amountComputingComponent = viewChild(AmountComputingComponent);
  //forcerStockBtn = viewChild<ElementRef>('forcerStockBtn');
  selectedCustomerService = inject(SelectedCustomerService);
  typePrescriptionService = inject(TypePrescriptionService);
  userCaissierService = inject(UserCaissierService);
  userVendeurService = inject(UserVendeurService);
  selectModeReglementService = inject(SelectModeReglementService);
  salesService = inject(VoSalesService);
  currentSaleService = inject(CurrentSaleService);
  customerService = inject(CustomerService);
  activatedRoute = inject(ActivatedRoute);
  router = inject(Router);
  modalService = inject(NgbModal);
  confirmationService = inject(ConfirmationService);
  errorService = inject(ErrorService);
  dialogService = inject(DialogService);
  translate = inject(TranslateService);
  baseSaleService = inject(BaseSaleService);
  protected entryAmount?: number | null = null;
  protected isSaving = false;
  protected payments: IPayment[] = [];
  protected ref: DynamicDialogRef;
  protected remises: IRemise[] = [];
  protected remise?: IRemise | null;

  protected event: any;

  constructor() {
    this.currentSaleService.setTypeVo('ASSURANCE');
  }

  manageAmountDiv(): void {
    this.modeReglementComponent().manageAmountDiv();
  }

  onHideHideDialog() {
    this.commonDialog = false;
  }

  cancelCommonDialog(): void {
    this.commonDialog = false;
  }

  manageCashPaymentMode(paymentModeControl: PaymentModeControl): void {
    const modes = this.selectModeReglementService.modeReglements();
    if (modes.length >= this.baseSaleService.maxModePayementNumber()) {
      const amount = this.getEntryAmount();
      modes.find((e: IPaymentMode) => e.code !== paymentModeControl.control.target.id).amount =
        this.currentSaleService.currentSale().amountToBePaid - paymentModeControl.paymentMode.amount;

      this.amountComputingComponent()?.computeMonnaie(amount);
    } else {
      const inputAmount = Number(paymentModeControl.control.target.value);
      this.amountComputingComponent()?.computeMonnaie(inputAmount);
      this.modeReglementComponent().manageShowAddButton(inputAmount);
    }
  }

  finalyseSale(putsOnStandby: boolean = false): void {
    const entryAmount = this.getEntryAmount();
    this.currentSaleService.currentSale().payments = this.modeReglementComponent().buildPayment(entryAmount);
    this.currentSaleService.currentSale().type = 'VO';
    this.currentSaleService.currentSale().avoir = this.baseSaleService.isAvoir();
    this.computExtraInfo();
    if (this.isPresale || putsOnStandby) {
      this.putCurrentSaleOnHold();
    } else {
      this.saveSale();
    }
  }

  putCurrentSaleOnStandBy(): void {
    this.baseSaleService.onStandby();
    //  this.finalyseSale(true);
  }

  onKeyDown(event: any): void {
    this.onCompleteSale();
  }

  onCompleteSale(): void {
    this.baseSaleService.onCompleteSale();
  }

  isValidDiffere(): boolean {
    return this.currentSaleService.currentSale().differe /*&& !this.sale.customerId*/;
  }

  onLoadPrevente(): void {
    this.modeReglementComponent()?.buildPreventeReglementInput();
  }

  getEntryAmount(): number {
    return this.modeReglementComponent()?.getInputSum() || 0;
  }

  computExtraInfo(): void {
    this.currentSaleService.currentSale().commentaire = this.modeReglementComponent()?.commentaire || null;
  }

  save(): void {
    this.isSaving = true;
    if (this.currentSaleService.currentSale()?.amountToBePaid > 0) {
      const entryAmount = this.getEntryAmount();
      const restToPay = this.currentSaleService.currentSale().amountToBePaid - entryAmount;
      this.currentSaleService.currentSale().montantVerse = this.baseSaleService.getCashAmount(entryAmount);
      if (restToPay > 0 && !this.isValidDiffere()) {
        this.differeConfirmDialog();
      } else {
        this.finalyseSale();
      }
    } else {
      this.currentSaleService.currentSale().montantVerse = 0;
      this.finalyseSale();
    }
  }

  differeConfirmDialog(): void {
    this.confirmationService.confirm({
      message: 'Voullez-vous regler le reste en différé ?',
      header: 'Vente différé',
      icon: 'pi pi-info-circle',
      accept: () => {
        this.currentSaleService.currentSale().differe = true;
        this.finalyseSale();
      },
      reject: () => {},
      key: 'differeConfirmDialog',
    });

    setTimeout(() => {
      this.differeConfirmDialogBtn().nativeElement.focus();
    }, 10);
  }

  saveSale(): void {
    const sale = this.currentSaleService.currentSale();
    const entryAmount = this.getEntryAmount();
    const restToPay = sale.amountToBePaid - entryAmount;
    if (restToPay <= 0) {
      sale.payrollAmount = sale.amountToBePaid;
      sale.restToPay = 0;
    } else {
      sale.payrollAmount = entryAmount;
      sale.restToPay = restToPay;
    }
    sale.montantRendu = sale.montantVerse - sale.amountToBePaid;
    this.subscribeToFinalyseResponse(this.salesService.save(sale));
  }

  putCurrentSaleOnHold(): void {
    this.subscribeToPutOnHoldResponse(this.salesService.putCurrentOnStandBy(this.currentSaleService.currentSale()));
  }

  create(salesLine: ISalesLine, tiersPayants: IClientTiersPayant[]): void {
    this.subscribeToCreateSaleResponse(this.salesService.create(this.createSale(salesLine, tiersPayants)));
  }

  onAddProduit(salesLine: ISalesLine): void {
    this.subscribeToSaveLineResponse(this.salesService.addItem(salesLine));
  }

  removeLine(salesLine: ISalesLine): void {
    this.removeItem(salesLine.id);
  }

  confirmDeleteItem(item: ISalesLine): void {
    if (item) {
      this.removeLine(item);
    } else {
      this.baseSaleService.setInputBoxFocus('produitBox');
      //  this.check = true;s
    }
  }

  updateItemQtyRequested(salesLine: ISalesLine): void {
    if (salesLine) {
      this.processQtyRequested(salesLine);
    }
  }

  updateItemQtySold(salesLine: ISalesLine): void {
    const sale = this.currentSaleService.currentSale();
    this.salesService.updateItemQtySold(salesLine).subscribe({
      next: () => this.subscribeToSaveResponse(this.salesService.find(sale.id)),
      error: (err: any) => this.baseSaleService.onSaveError(err, sale),
      complete: () => {
        this.isSaving = false;
      },
    });
  }

  updateItemPrice(salesLine: ISalesLine): void {
    this.processItemPrice(salesLine);
  }

  subscribeToSaveLineResponse(result: Observable<HttpResponse<ISalesLine>>): void {
    result.subscribe({
      next: (res: HttpResponse<ISalesLine>) => this.subscribeToSaveResponse(this.salesService.find(res.body.saleId)),
      error: (err: any) => this.baseSaleService.onSaveError(err, this.currentSaleService.currentSale()),
      complete: () => {
        this.isSaving = false;
      },
    });
  }

  ngAfterViewInit(): void {}

  printInvoice(): void {
    this.salesService.print(this.currentSaleService.currentSale()?.id).subscribe(blod => {
      const blobUrl = URL.createObjectURL(blod);
      window.open(blobUrl);
    });
  }

  print(sale: ISales | null): void {
    this.salesService.print(sale.id).subscribe(blod => saveAs(blod));
  }

  printSale(saleId: number): void {
    this.salesService.printReceipt(saleId).subscribe();
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<ISales>>): void {
    result.subscribe({
      next: (res: HttpResponse<ISales>) => this.baseSaleService.onSaveSuccess(res.body),
      error: error => this.baseSaleService.onError(error),
      complete: () => {
        this.isSaving = false;
      },
    });
  }

  protected subscribeToFinalyseResponse(result: Observable<HttpResponse<FinalyseSale>>): void {
    result.subscribe({
      next: (res: HttpResponse<FinalyseSale>) => this.baseSaleService.onFinalyseSuccess(res.body),
      error: err => this.baseSaleService.onFinalyseError(err),
      complete: () => {
        this.isSaving = false;
      },
    });
  }

  protected subscribeToPutOnHoldResponse(result: Observable<HttpResponse<FinalyseSale>>): void {
    result.subscribe({
      next: (res: HttpResponse<FinalyseSale>) => this.baseSaleService.onFinalyseSuccess(res.body, true),
      error: err => this.baseSaleService.onFinalyseError(err),
      complete: () => {
        this.isSaving = false;
      },
    });
  }

  protected subscribeToCreateSaleResponse(result: Observable<HttpResponse<ISales>>): void {
    result.subscribe({
      next: (res: HttpResponse<ISales>) => this.baseSaleService.onSaleResponseSuccess(res.body),
      error: (err: any) => this.baseSaleService.onSaveError(err, this.currentSaleService.currentSale()),
      complete: () => {
        this.isSaving = false;
      },
    });
  }

  private createSale(salesLine: ISalesLine, tiersPayants: IClientTiersPayant[]): ISales {
    const currentCustomer = this.selectedCustomerService.selectedCustomerSignal();
    return {
      ...new Sales(),
      salesLines: [salesLine],
      customerId: currentCustomer.id,
      natureVente: 'ASSURANCE',
      typePrescription: this.typePrescriptionService.typePrescription()?.code,
      cassierId: this.userCaissierService.caissier()?.id,
      sellerId: this.userVendeurService.vendeur()?.id,
      type: 'VO',
      categorie: 'VO',
      tiersPayants,
    };
  }

  private processItemPrice(salesLine: ISalesLine): void {
    const sale = this.currentSaleService.currentSale();
    this.salesService.updateItemPrice(salesLine).subscribe({
      next: () => this.subscribeToSaveResponse(this.salesService.find(sale.id)),
      error: (err: any) => this.baseSaleService.onSaveError(err, sale),
      complete: () => {
        this.isSaving = false;
      },
    });
  }

  private removeItem(id: number): void {
    const sale = this.currentSaleService.currentSale();
    this.salesService.deleteItem(id).subscribe({
      next: () => this.subscribeToSaveResponse(this.salesService.find(sale.id)),
      error: (err: any) => this.baseSaleService.onSaveError(err, sale),
      complete: () => {
        this.isSaving = false;
      },
    });
  }

  private processQtyRequested(salesLine: ISalesLine): void {
    const sale = this.currentSaleService.currentSale();
    this.salesService.updateItemQtyRequested(salesLine).subscribe({
      next: () => this.subscribeToSaveResponse(this.salesService.find(sale.id)),
      error: (err: any) => this.baseSaleService.onSaveError(err, sale),
      complete: () => {
        this.isSaving = false;
      },
    });
  }
}
