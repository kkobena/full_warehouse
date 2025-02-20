import { Component, ElementRef, inject, input, output, viewChild } from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DividerModule } from 'primeng/divider';
import { ConfirmationService } from 'primeng/api';
import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';
import { WarehouseCommonModule } from '../../../../shared/warehouse-common/warehouse-common.module';
import { RouterModule } from '@angular/router';
import { NgxSpinnerModule } from 'ngx-spinner';
import { FormsModule } from '@angular/forms';
import { UninsuredCustomerListComponent } from '../../uninsured-customer-list/uninsured-customer-list.component';
import { ProductTableComponent } from '../product-table/product-table.component';
import { IPaymentMode, PaymentModeControl } from '../../../../shared/model/payment-mode.model';
import { IPayment } from '../../../../shared/model/payment.model';
import { IRemise } from '../../../../shared/model/remise.model';
import { FinalyseSale, InputToFocus, ISales, Sales, SaveResponse } from '../../../../shared/model/sales.model';
import { ISalesLine } from '../../../../shared/model/sales-line.model';
import { Observable } from 'rxjs';
import { SalesService } from '../../sales.service';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { HttpResponse } from '@angular/common/http';
import { AmountComputingComponent } from './amount-computing/amount-computing.component';
import { ModeReglementComponent } from '../../mode-reglement/mode-reglement.component';
import { CurrentSaleService } from '../../service/current-sale.service';
import { SelectModeReglementService } from '../../service/select-mode-reglement.service';
import { SelectedCustomerService } from '../../service/selected-customer.service';
import { TypePrescriptionService } from '../../service/type-prescription.service';
import { UserCaissierService } from '../../service/user-caissier.service';
import { UserVendeurService } from '../../service/user-vendeur.service';
import { BaseSaleService } from '../../service/base-sale.service';
import { FormActionAutorisationComponent } from '../../form-action-autorisation/form-action-autorisation.component';
import { Authority } from '../../../../shared/constants/authority.constants';
import { HasAuthorityService } from '../../service/has-authority.service';
import { acceptButtonProps, rejectButtonProps } from '../../../../shared/util/modal-button-props';

@Component({
  selector: 'jhi-comptant',
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
    RouterModule,
    NgxSpinnerModule,
    ButtonModule,
    FormsModule,
    ConfirmDialogModule,
    DividerModule,
    ProductTableComponent,
    AmountComputingComponent,
    ModeReglementComponent,
  ],
  templateUrl: './comptant.component.html',
})
export class ComptantComponent {
  readonly isPresale = input(false);
  readonly appendTo = 'body';
  readonly inputToFocusEvent = output<InputToFocus>();
  readonly saveResponse = output<SaveResponse>({ alias: 'saveResponse' });
  readonly responseEvent = output<FinalyseSale>({ alias: 'responseEvent' });
  readonly CASH = 'CASH';
  avoirConfirmDialogBtn = viewChild<ElementRef>('avoirConfirmDialogBtn');
  amountComputingComponent = viewChild(AmountComputingComponent);
  modeReglementComponent = viewChild(ModeReglementComponent);
  currentSaleService = inject(CurrentSaleService);
  protected isSaving = false;
  protected payments: IPayment[] = [];
  protected ref: DynamicDialogRef;
  protected remise?: IRemise | null;
  protected event: any;
  protected entryAmount?: number | null = null;
  protected canRemoveItem: boolean;
  protected canApplyDiscount: boolean;
  private selectedCustomerService = inject(SelectedCustomerService);
  private typePrescriptionService = inject(TypePrescriptionService);
  private userCaissierService = inject(UserCaissierService);
  private userVendeurService = inject(UserVendeurService);
  private selectModeReglementService = inject(SelectModeReglementService);
  private salesService = inject(SalesService);

  private modalService = inject(NgbModal);
  private confirmationService = inject(ConfirmationService);
  private dialogService = inject(DialogService);

  private baseSaleService = inject(BaseSaleService);
  private hasAuthorityService = inject(HasAuthorityService);

  constructor() {
    this.canRemoveItem = this.hasAuthorityService.hasAuthorities(Authority.PR_SUPPRIME_PRODUIT_VENTE);
    this.canApplyDiscount = this.hasAuthorityService.hasAuthorities(Authority.PR_AJOUTER_REMISE_VENTE);
  }

  manageAmountDiv(): void {
    this.modeReglementComponent().manageAmountDiv();
  }

  differeConfirmDialog(): void {
    this.confirmationService.confirm({
      message: 'Voullez-vous regler le reste en différé ?',
      header: 'Vente différé',
      icon: 'pi pi-info-circle',
      rejectButtonProps: rejectButtonProps(),
      acceptButtonProps: acceptButtonProps(),
      accept: () => {
        if (!this.currentSaleService.currentSale().customerId) {
          this.openUninsuredCustomer(true);
        } else {
          this.currentSaleService.currentSale().differe = true;
          this.finalyseSale();
        }
      },
      reject: () => {},
      key: 'differeConfirmDialog',
    });
  }

  onOpenCustomer(putsOnStandby: boolean = false): void {
    this.confirmationService.confirm({
      message: 'Vous devez ajouter un client à la vente ?',
      header: 'Vente en avoir',
      icon: 'pi pi-info-circle',
      rejectButtonProps: rejectButtonProps(),
      acceptButtonProps: acceptButtonProps(),
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
    this.currentSaleService.currentSale().commentaire = this.modeReglementComponent().commentaire;
  }

  finalyseSale(putsOnStandby: boolean = false): void {
    const entryAmount = this.getEntryAmount();
    this.currentSaleService.currentSale().payments = this.modeReglementComponent().buildPayment(entryAmount);
    this.currentSaleService.currentSale().type = 'VNO';
    this.currentSaleService.currentSale().avoir = this.baseSaleService.isAvoir();
    this.computExtraInfo();
    if (this.currentSaleService.currentSale().avoir && !this.currentSaleService.currentSale().customerId) {
      this.onOpenCustomer(putsOnStandby);
    } else {
      if (this.isPresale() || putsOnStandby) {
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
    return this.currentSaleService.currentSale().differe /*&& !this.sale.customerId*/;
  }

  save(): void {
    this.isSaving = true;
    const restToPay = this.currentSaleService.currentSale().amountToBePaid - this.getEntryAmount();
    this.currentSaleService.currentSale().montantVerse = this.getCashAmount();
    if (restToPay > 0 && !this.isValidDiffere()) {
      this.differeConfirmDialog();
    } else {
      this.finalyseSale();
    }
  }

  saveCashSale(): void {
    const entryAmount = this.getEntryAmount();
    const currtSale = this.currentSaleService.currentSale();
    const restToPay = currtSale.amountToBePaid - entryAmount;
    if (restToPay <= 0) {
      currtSale.payrollAmount = currtSale.amountToBePaid;
      currtSale.restToPay = 0;
    } else {
      currtSale.payrollAmount = entryAmount;
      currtSale.restToPay = restToPay;
    }
    currtSale.montantRendu = currtSale.montantVerse - currtSale.amountToBePaid;
    this.subscribeToFinalyseResponse(this.salesService.saveCash(currtSale));
  }

  putCurrentCashSaleOnHold(): void {
    this.subscribeToPutOnHoldResponse(this.salesService.putCurrentCashSaleOnStandBy(this.currentSaleService.currentSale()));
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
    if (salesLine) {
      this.processQtyRequested(salesLine);
    }
  }

  updateItemQtySold(salesLine: ISalesLine): void {
    const sale = this.currentSaleService.currentSale();
    this.salesService.updateItemQtySold(salesLine).subscribe({
      next: () => this.subscribeToSaveResponse(this.salesService.find(sale.id)),
      error: (err: any) => this.onSaveSaveError(err, sale),
    });
  }

  updateItemPrice(salesLine: ISalesLine): void {
    this.processItemPrice(salesLine);
  }

  printInvoice(): void {
    if (this.selectedCustomerService.selectedCustomerSignal()) {
      this.salesService.printInvoice(this.currentSaleService.currentSale()?.id).subscribe(blod => {
        const blobUrl = URL.createObjectURL(blod);
        window.open(blobUrl);
      });
    }
  }

  subscribeToSaveLineResponse(result: Observable<HttpResponse<ISalesLine>>): void {
    result.subscribe({
      next: (res: HttpResponse<ISalesLine>) => this.subscribeToSaveResponse(this.salesService.find(res.body.saleId)),
      error: err => this.onSaveSaveError(err, this.currentSaleService.currentSale()),
    });
  }

  getEntryAmount(): number {
    return this.modeReglementComponent().getInputSum() || 0;
  }

  manageCashPaymentMode(paymentModeControl: PaymentModeControl): void {
    const modes = this.selectModeReglementService.modeReglements();
    if (modes.length >= this.baseSaleService.maxModePayementNumber()) {
      const amount = this.getEntryAmount();
      modes.find((e: IPaymentMode) => e.code !== paymentModeControl.control.target.id).amount =
        this.currentSaleService.currentSale().amountToBePaid - paymentModeControl.paymentMode.amount;

      this.amountComputingComponent().computeMonnaie(amount);
    } else {
      const inputAmount = Number(paymentModeControl.control.target.value);
      this.amountComputingComponent().computeMonnaie(inputAmount);
      this.modeReglementComponent().manageShowAddButton(inputAmount);
    }
  }

  openUninsuredCustomer(isVenteDefferee: boolean, putsOnStandby: boolean = false): void {
    this.ref = this.dialogService.open(UninsuredCustomerListComponent, {
      header: 'CLIENTS NON ASSURES',
      width: '60%',
      closeOnEscape: false,
    });
    this.ref.onDestroy.subscribe(() => {
      if (isVenteDefferee && this.selectedCustomerService.selectedCustomerSignal()) {
        this.currentSaleService.currentSale().differe = isVenteDefferee;
        this.modeReglementComponent().commentaireInputGetFocus();
      } else {
        if (!isVenteDefferee) {
          this.finalyseSale(putsOnStandby);
        }
      }
    });
  }

  onLoadPrevente(): void {
    this.modeReglementComponent()?.buildPreventeReglementInput();
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

  addRemise(remise: IRemise): void {
    if (remise) {
      this.salesService
        .addRemise({
          key: this.currentSaleService.currentSale().id,
          value: remise.id,
        })
        .subscribe({
          next: () => this.subscribeToSaveResponse(this.salesService.find(this.currentSaleService.currentSale().id)),
          error: (err: any) => this.onSaveError(err),
        });
    } else {
      if (this.currentSaleService.currentSale()?.remise) {
        this.salesService.removeRemiseFromCashSale(this.currentSaleService.currentSale().id).subscribe({
          next: () => this.subscribeToSaveResponse(this.salesService.find(this.currentSaleService.currentSale().id)),
          error: (err: any) => this.onSaveError(err),
        });
      }
    }
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
    this.saveResponse.emit({ success: false, error: err });
  }

  protected onFinalyseError(err: any): void {
    this.isSaving = false;
    this.responseEvent.emit({ error: err, success: false });
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
      error: error => this.onSaveSaveError(error, this.currentSaleService.currentSale()),
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

  private onSaveSaveError(err: any, sale?: ISales, payload: any = null): void {
    this.isSaving = false;
    this.saveResponse.emit({ success: false, error: err, payload });
    this.currentSaleService.setCurrentSale(sale);
  }

  private processItemPrice(salesLine: ISalesLine): void {
    const sale = this.currentSaleService.currentSale();
    this.salesService.updateItemPrice(salesLine).subscribe({
      next: () => this.subscribeToSaveResponse(this.salesService.find(sale.id)),
      error: (err: any) => this.onSaveSaveError(err, sale),
    });
  }

  private removeItem(id: number): void {
    const sale = this.currentSaleService.currentSale();
    this.salesService.deleteItem(id).subscribe({
      next: () => this.subscribeToSaveResponse(this.salesService.find(sale.id)),
      error: (err: any) => this.onSaveSaveError(err, sale),
    });
  }

  private processQtyRequested(salesLine: ISalesLine): void {
    const sale = this.currentSaleService.currentSale();
    this.salesService.updateItemQtyRequested(salesLine).subscribe({
      next: () => this.subscribeToSaveResponse(this.salesService.find(sale.id)),
      error: (err: any) => this.onSaveSaveError(err, sale, salesLine),
    });
  }
}
