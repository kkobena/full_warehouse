import { Component, ElementRef, inject, Input, viewChild } from '@angular/core';
import { ModeReglementComponent } from '../../mode-reglement/mode-reglement.component';
import { AmountComputingComponent } from '../comptant/amount-computing/amount-computing.component';
import { SelectedCustomerService } from '../../service/selected-customer.service';
import { TypePrescriptionService } from '../../service/type-prescription.service';
import { UserCaissierService } from '../../service/user-caissier.service';
import { UserVendeurService } from '../../service/user-vendeur.service';
import { SelectModeReglementService } from '../../service/select-mode-reglement.service';
import { VoSalesService } from '../../service/vo-sales.service';
import { CurrentSaleService } from '../../service/current-sale.service';
import { CustomerService } from '../../../customer/customer.service';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { ConfirmationService } from 'primeng/api';
import { ErrorService } from '../../../../shared/error.service';
import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';
import { TranslateService } from '@ngx-translate/core';
import { BaseSaleService } from '../../service/base-sale.service';
import { IPayment } from '../../../../shared/model/payment.model';
import { IRemise } from '../../../../shared/model/remise.model';
import { ISalesLine } from '../../../../shared/model/sales-line.model';
import { HttpResponse } from '@angular/common/http';
import { FinalyseSale, ISales } from '../../../../shared/model/sales.model';
import { Observable } from 'rxjs';
import { IClientTiersPayant } from '../../../../shared/model/client-tiers-payant.model';
import { IPaymentMode, PaymentModeControl } from '../../../../shared/model/payment-mode.model';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogModule } from 'primeng/dialog';
import { DividerModule } from 'primeng/divider';
import { DropdownModule } from 'primeng/dropdown';
import { WarehouseCommonModule } from '../../../../shared/warehouse-common/warehouse-common.module';
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
import { Authority } from '../../../../shared/constants/authority.constants';
import { HasAuthorityService } from '../../service/has-authority.service';
import { FormActionAutorisationComponent } from '../../form-action-autorisation/form-action-autorisation.component';
import { acceptButtonProps, rejectButtonProps } from '../../../../shared/util/modal-button-props';

@Component({
  templateUrl: './base-sale.component.html',
  providers: [ConfirmationService, DialogService],
  imports: [
    ConfirmDialogModule,
    DialogModule,

    AmountComputingComponent,
    DividerModule,
    DropdownModule,
    WarehouseCommonModule,
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
    ConfirmDialogModule,
    DialogModule,

    AmountComputingComponent,
    DividerModule,
    DropdownModule,
    WarehouseCommonModule,
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
})
export class BaseSaleComponent {
  differeConfirmDialogBtn = viewChild<ElementRef>('differeConfirmDialogBtn');
  modeReglementComponent = viewChild(ModeReglementComponent);
  amountComputingComponent = viewChild(AmountComputingComponent);
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
  hasAuthorityService = inject(HasAuthorityService);
  @Input('isPresale') isPresale = false;
  appendTo = 'body';
  CASH = 'CASH';
  protected entryAmount?: number | null = null;
  protected payments: IPayment[] = [];
  protected ref: DynamicDialogRef;
  protected remise?: IRemise | null;
  protected isSaving = false;
  private readonly canRemoveItem: boolean;
  private readonly canApplyDiscount: boolean;

  constructor() {
    this.canRemoveItem = this.hasAuthorityService.hasAuthorities(Authority.PR_SUPPRIME_PRODUIT_VENTE);
    this.canApplyDiscount = this.hasAuthorityService.hasAuthorities(Authority.PR_AJOUTER_REMISE_VENTE);
  }

  manageAmountDiv(): void {
    this.modeReglementComponent().manageAmountDiv();
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
    setTimeout(() => {
      this.baseSaleService.setInputBoxFocus('produitBox');
    }, 50);
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
      rejectButtonProps: rejectButtonProps(),
      acceptButtonProps: acceptButtonProps(),
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
    this.subscribeToCreateSaleResponse(
      this.salesService.create(
        this.baseSaleService.createSale(
          salesLine,
          tiersPayants,
          this.typePrescriptionService.typePrescription()?.code,
          this.userCaissierService.caissier()?.id,
          this.userVendeurService.vendeur()?.id,
          this.selectedCustomerService.selectedCustomerSignal()?.id,
          this.currentSaleService.typeVo(),
        ),
      ),
    );
  }

  onAddProduit(salesLine: ISalesLine): void {
    this.subscribeToSaveLineResponse(this.salesService.addItem(salesLine));
  }

  removeLine(salesLine: ISalesLine): void {
    this.removeItem(salesLine.id);
  }

  openActionAutorisationDialog(privilege: string, entityToProccess: any): void {
    const modalRef = this.modalService.open(FormActionAutorisationComponent, {
      backdrop: 'static',
      centered: true,
    });
    modalRef.componentInstance.entity = this.currentSaleService.currentSale();
    modalRef.componentInstance.privilege = privilege;
    modalRef.closed.subscribe(reason => {
      if (reason === true) {
        this.removeLine(entityToProccess);
      }
    });
  }

  confirmDeleteItem(item: ISalesLine): void {
    if (item) {
      if (this.canRemoveItem) {
        this.removeLine(item);
      } else {
        this.openActionAutorisationDialog(Authority.PR_SUPPRIME_PRODUIT_VENTE, item);
      }
    } else {
      this.baseSaleService.setInputBoxFocus('produitBox');
    }
  }

  updateItemQtyRequested(salesLine: ISalesLine): void {
    this.processQtyRequested(salesLine);
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

  printInvoice(): void {
    this.salesService.printInvoice(this.currentSaleService.currentSale()?.id).subscribe(blod => {
      const blobUrl = URL.createObjectURL(blod);
      window.open(blobUrl);
    });
  }

  print(sale: ISales | null): void {
    this.salesService.print(sale.id).subscribe(blod => {
      const blobUrl = URL.createObjectURL(blod);
      window.open(blobUrl);
    });
  }

  printSale(saleId: number): void {
    this.salesService.printReceipt(saleId).subscribe();
  }

  onAddRmiseOpenActionAutorisationDialog(remise: IRemise): void {
    const modalRef = this.modalService.open(FormActionAutorisationComponent, {
      backdrop: 'static',
      centered: true,
    });
    modalRef.componentInstance.entity = this.currentSaleService.currentSale();
    modalRef.componentInstance.privilege = Authority.PR_AJOUTER_REMISE_VENTE;
    modalRef.closed.subscribe(reason => {
      if (reason === true) {
        this.addRemise(remise);
      }
    });
  }

  addRemise(remise: IRemise): void {
    if (remise) {
      this.salesService
        .addRemise({
          key: this.currentSaleService.currentSale().id,
          value: remise.id,
        })
        .subscribe({
          next: () => this.subscribeToSaveResponse(this.salesService.find(this.currentSaleService.currentSale().id)),
          error: (err: any) => this.baseSaleService.onSaveError(err, this.currentSaleService.currentSale()),
        });
    } else {
      if (this.currentSaleService.currentSale()?.remise) {
        this.salesService.removeRemiseFromCashSale(this.currentSaleService.currentSale().id).subscribe({
          next: () => this.subscribeToSaveResponse(this.salesService.find(this.currentSaleService.currentSale().id)),
          error: (err: any) => this.baseSaleService.onSaveError(err, this.currentSaleService.currentSale()),
        });
      }
    }
  }

  onAddRemise(remise: IRemise): void {
    if (this.canApplyDiscount) {
      this.addRemise(remise);
    } else {
      if (remise) {
        this.onAddRmiseOpenActionAutorisationDialog(remise);
      } else {
        if (this.currentSaleService.currentSale()?.remise) {
          this.onAddRmiseOpenActionAutorisationDialog(remise);
        }
      }
    }
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
      error: (err: any) => {
        if (err.error?.errorKey === 'stock' || err.error?.errorKey === 'stockChInsufisant') {
          this.baseSaleService.onStockError(err, salesLine);
        } else {
          this.baseSaleService.onSaveError(err, sale);
        }
      },
      complete: () => {
        this.isSaving = false;
      },
    });
  }
}
