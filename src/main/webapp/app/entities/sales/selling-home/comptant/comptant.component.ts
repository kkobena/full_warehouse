import { Component, inject, input, output, signal, viewChild } from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { DividerModule } from 'primeng/divider';
import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';
import { WarehouseCommonModule } from '../../../../shared/warehouse-common/warehouse-common.module';
import { RouterModule } from '@angular/router';
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
import { ConfirmDialogComponent } from '../../../../shared/dialog/confirm-dialog/confirm-dialog.component';
import { SpinerService } from '../../../../shared/spiner.service';

@Component({
  selector: 'jhi-comptant',
  providers: [DialogService],
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
    ButtonModule,
    FormsModule,
    DividerModule,
    ProductTableComponent,
    AmountComputingComponent,
    ModeReglementComponent,
    ConfirmDialogComponent,
  ],
  templateUrl: './comptant.component.html',
})
export class ComptantComponent {
  readonly isPresale = input(false);
  readonly appendTo = 'body';
  readonly inputToFocusEvent = output<InputToFocus>();
  readonly saveResponse = output<SaveResponse>();
  readonly responseEvent = output<FinalyseSale>();
  readonly CASH = 'CASH';
  
  modeReglementComponent = viewChild<ModeReglementComponent>('modeReglement');
  amountComputingComponent = viewChild<AmountComputingComponent>('amountComputing');
  currentSaleService = inject(CurrentSaleService);

  protected isSaving = false;
  protected payments: IPayment[] = [];
  protected ref: DynamicDialogRef;
  protected remise?: IRemise | null;
  protected event: any;
  protected readonly hasAuthorityService = inject(HasAuthorityService);
  readonly canRemoveItem = signal(this.hasAuthorityService.hasAuthorities(Authority.PR_SUPPRIME_PRODUIT_VENTE));
  readonly canApplyDiscount = signal(this.hasAuthorityService.hasAuthorities(Authority.PR_AJOUTER_REMISE_VENTE));
  private readonly confimDialog = viewChild.required<ConfirmDialogComponent>('confirmDialog');
  private readonly selectedCustomerService = inject(SelectedCustomerService);
  private readonly typePrescriptionService = inject(TypePrescriptionService);
  private readonly userCaissierService = inject(UserCaissierService);
  private readonly userVendeurService = inject(UserVendeurService);
  private readonly selectModeReglementService = inject(SelectModeReglementService);
  private readonly salesService = inject(SalesService);
  private readonly modalService = inject(NgbModal);
  private readonly dialogService = inject(DialogService);
  private readonly baseSaleService = inject(BaseSaleService);
  private readonly spinner = inject(SpinerService);

  //private readonly scanDetectorService = inject(ScanDetectorService);

  constructor() {}

  /*
    @HostListener('window:keypress', ['$event'])
    handleKeyboardEvent(event: KeyboardEvent) {
      const result = this.scanDetectorService.keyPressed(event.key);

      if (result) {
        console.log('SCAN détecté :', result);
      }
    }*/

  protected get entryAmount(): number {
    return this.modeReglementComponent()?.getInputSum() || 0;
  }

  protected get isValidDiffere(): boolean {
    return this.currentSaleService.currentSale()?.differe;
  }

  manageAmountDiv(): void {
    this.modeReglementComponent().manageAmountDiv();
  }

  differeConfirmDialog(): void {
    this.confimDialog().onConfirm(
      () => {
        this.currentSaleService.currentSale().differe = true;
        if (!this.currentSaleService.currentSale().customerId) {
          this.openUninsuredCustomer(true);
        } else {
          this.finalyseSale();
        }
      },
      'Vente différé',
      'Voullez-vous regler le reste en différé ?',
    );
  }

  onOpenCustomer(putsOnStandby = false): void {
    this.confimDialog().onConfirm(
      () => this.openUninsuredCustomer(false, putsOnStandby),
      'Vente en avoir',
      'Vous devez ajouter un client à la vente ?',
    );
  }

  computExtraInfo(): void {
    this.currentSaleService.currentSale().commentaire = this.modeReglementComponent().commentaire;
  }

  finalyseSale(putsOnStandby = false): void {
    this.currentSaleService.currentSale().payments = this.modeReglementComponent().buildPayment(this.entryAmount);
    this.currentSaleService.currentSale().type = 'VNO';
    this.currentSaleService.currentSale().avoir = this.baseSaleService.isAvoir();
    this.computExtraInfo();
    if (this.currentSaleService.currentSale().avoir && !this.currentSaleService.currentSale().customerId) {
      this.onOpenCustomer(putsOnStandby);
    } else {
      if (this.isPresale() || putsOnStandby) {
        this.spinner.show();
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

  save(): void {
    this.isSaving = true;
    console.log(this.entryAmount);
    const restToPay = this.currentSaleService.currentSale().amountToBePaid - this.entryAmount;
    this.currentSaleService.currentSale().montantVerse = this.getCashAmount();
    if (restToPay > 0 && !this.isValidDiffere) {
      this.differeConfirmDialog();
    } else {
      this.finalyseSale();
    }
  }

  saveCashSale(): void {
    this.spinner.show();
    const currentSale = this.currentSaleService.currentSale();
    this.updateSaleAmounts(currentSale, this.entryAmount);
    this.subscribeToFinalyseResponse(this.salesService.saveCash(currentSale));
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
      if (this.canRemoveItem()) {
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
      this.salesService.printInvoice(this.currentSaleService.currentSale().id).subscribe(blod => {
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

  manageCashPaymentMode(paymentModeControl: PaymentModeControl): void {
    const modes = this.selectModeReglementService.modeReglements();
    if (modes.length >= this.baseSaleService.maxModePayementNumber()) {
      const amount = this.entryAmount;
      modes.find((e: IPaymentMode) => e.code !== paymentModeControl.control.target.id).amount =
        this.currentSaleService.currentSale().amountToBePaid - paymentModeControl.paymentMode.amount;

      this.amountComputingComponent().computeMonnaie(amount);
    } else {
      const inputAmount = Number(paymentModeControl.control.target.value?.replace(/\D/g, '')); //Number(paymentModeControl.control.target.value);
      this.amountComputingComponent().computeMonnaie(inputAmount);
      this.modeReglementComponent().manageShowAddButton(inputAmount);
    }
  }

  openUninsuredCustomer(isVenteDefferee: boolean, putsOnStandby = false): void {
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
    this.modeReglementComponent().buildPreventeReglementInput();
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
    if (this.canApplyDiscount()) {
      this.addRemise(remise);
    } else {
      if (remise) {
        this.onAddRmiseOpenActionAutorisationDialog(remise);
      } else {
        if (this.currentSaleService.currentSale().remise) {
          this.onAddRmiseOpenActionAutorisationDialog(remise);
        }
      }
    }
  }

  addRemise(remise: IRemise): void {
    if (remise) {
      this.spinner.show();
      this.salesService
        .addRemise({
          key: this.currentSaleService.currentSale()?.id,
          value: remise.id,
        })
        .subscribe({
          next: () => this.subscribeToSaveResponse(this.salesService.find(this.currentSaleService.currentSale().id)),
          error: (err: any) => this.onSaveError(err),
          complete: () => this.spinner.hide(),
        });
    } else {
      if (this.currentSaleService.currentSale().remise) {
        this.spinner.show();
        this.salesService.removeRemiseFromCashSale(this.currentSaleService.currentSale().id).subscribe({
          next: () => this.subscribeToSaveResponse(this.salesService.find(this.currentSaleService.currentSale().id)),
          error: (err: any) => this.onSaveError(err),
          complete: () => this.spinner.hide(),
        });
      }
    }
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<ISales>>): void {
    result.subscribe({
      next: (res: HttpResponse<ISales>) => this.onSaveSuccess(res.body),
      error: error => this.onSaveError(error),
      complete: () => this.spinner.hide(),
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

  protected onFinalyseSuccess(response: FinalyseSale | null, putOnStandBy = false): void {
    this.isSaving = false;
    this.responseEvent.emit({ saleId: response.saleId, success: true, putOnStandBy });
  }

  protected subscribeToFinalyseResponse(result: Observable<HttpResponse<FinalyseSale>>): void {
    result.subscribe({
      next: (res: HttpResponse<FinalyseSale>) => this.onFinalyseSuccess(res.body),
      error: err => this.onFinalyseError(err),
      complete: () => this.spinner.hide(),
    });
  }

  protected subscribeToPutOnHoldResponse(result: Observable<HttpResponse<FinalyseSale>>): void {
    result.subscribe({
      next: (res: HttpResponse<FinalyseSale>) => this.onFinalyseSuccess(res.body, true),
      error: err => this.onFinalyseError(err),
      complete: () => this.spinner.hide(),
    });
  }

  protected subscribeToCreateSaleComptantResponse(result: Observable<HttpResponse<ISales>>): void {
    result.subscribe({
      next: (res: HttpResponse<ISales>) => this.onSaleComptantResponseSuccess(res.body),
      error: error => this.onSaveSaveError(error, this.currentSaleService.currentSale()),
      complete: () => this.spinner.hide(),
    });
  }

  protected onSaleComptantResponseSuccess(sale: ISales | null): void {
    this.isSaving = false;
    this.currentSaleService.setCurrentSale(sale);
    this.saveResponse.emit({ success: true });
  }

  private updateSaleAmounts(sale: ISales, entryAmount: number): void {
    const restToPay = sale.amountToBePaid - entryAmount;
    sale.payrollAmount = restToPay <= 0 ? sale.amountToBePaid : entryAmount;
    sale.restToPay = restToPay <= 0 ? 0 : restToPay;
    sale.montantRendu = sale.montantVerse - sale.amountToBePaid;
    console.log(entryAmount, restToPay, sale.montantVerse);
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
