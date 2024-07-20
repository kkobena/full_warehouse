import { Component, EventEmitter, inject, Input, Output, viewChild } from '@angular/core';
import { ButtonDirective } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogModule } from 'primeng/dialog';
import { ConfirmationService, Footer, PrimeTemplate } from 'primeng/api';
import { NgxSpinnerComponent } from 'ngx-spinner';
import { AmountComputingComponent } from '../comptant/amount-computing/amount-computing.component';
import { ModeReglementComponent } from '../../mode-reglement/mode-reglement.component';
import { FinalyseSale, InputToFocus, ISales, SaveResponse } from '../../../../shared/model/sales.model';
import { IPaymentMode } from '../../../../shared/model/payment-mode.model';
import { SelectModeReglementService } from '../../service/select-mode-reglement.service';
import { AlertInfoComponent } from '../../../../shared/alert/alert-info.component';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { DialogService } from 'primeng/dynamicdialog';
import { SelectedCustomerService } from '../../service/selected-customer.service';
import { TypePrescriptionService } from '../../service/type-prescription.service';
import { UserCaissierService } from '../../service/user-caissier.service';
import { UserVendeurService } from '../../service/user-vendeur.service';
import { SalesService } from '../../sales.service';
import { CurrentSaleService } from '../../service/current-sale.service';
import { CustomerService } from '../../../customer/customer.service';
import { ActivatedRoute, Router } from '@angular/router';
import { ErrorService } from '../../../../shared/error.service';
import { TranslateService } from '@ngx-translate/core';
import { Observable } from 'rxjs';
import { HttpResponse } from '@angular/common/http';
import { Payment } from '../../../../shared/model/payment.model';

@Component({
  selector: 'jhi-base-sale',
  standalone: true,
  providers: [ConfirmationService, DialogService],
  imports: [ButtonDirective, ConfirmDialogModule, DialogModule, Footer, NgxSpinnerComponent, PrimeTemplate],
  templateUrl: './base-sale.component.html',
  styleUrl: './base-sale.component.scss',
})
export class BaseSaleComponent {
  @Input('isPresale') isPresale = false;
  @Input('canUpdatePu') canUpdatePu: boolean = false;
  @Input('canForceStock') canForceStock: boolean = true;
  readonly appendTo = 'body';
  @Output() inputToFocusEvent = new EventEmitter<InputToFocus>();
  @Output('saveResponse') saveResponse = new EventEmitter<SaveResponse>();
  @Output('responseEvent') responseEvent = new EventEmitter<FinalyseSale>();
  commonDialog = false;
  readonly CASH = 'CASH';
  entryAmount?: number | null = null;
  sale?: ISales | null = null;
  displayErrorEntryAmountModal = false;
  amountComputingComponent = viewChild(AmountComputingComponent);
  modeReglementComponent = viewChild(ModeReglementComponent);
  selectModeReglementService = inject(SelectModeReglementService);
  selectedCustomerService = inject(SelectedCustomerService);
  typePrescriptionService = inject(TypePrescriptionService);
  userCaissierService = inject(UserCaissierService);
  userVendeurService = inject(UserVendeurService);
  modalService = inject(NgbModal);
  salesService = inject(SalesService);
  router = inject(Router);
  currentSaleService = inject(CurrentSaleService);
  customerService = inject(CustomerService);
  activatedRoute = inject(ActivatedRoute);
  confirmationService = inject(ConfirmationService);
  errorService = inject(ErrorService);
  dialogService = inject(DialogService);
  translate = inject(TranslateService);
  isSaving = false;

  onHidedisplayErrorEntryAmountModal(event: Event): void {
    // this.montantCashInput?.nativeElement.focus();
  }

  canceldisplayErrorEntryAmountModal(): void {
    this.displayErrorEntryAmountModal = false;
  }

  computExtraInfo(): void {
    this.sale.commentaire = this.modeReglementComponent().commentaire;
  }

  manageAmountDiv(): void {
    this.modeReglementComponent().manageAmountDiv();
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

  getEntryAmount(): number {
    return this.modeReglementComponent().getInputSum();
  }

  cancelCommonDialog(): void {
    this.commonDialog = false;
  }

  openInfoDialog(message: string, infoClass: string): void {
    const modalRef = this.modalService.open(AlertInfoComponent, {
      backdrop: 'static',
      centered: true,
    });
    modalRef.componentInstance.message = message;
    modalRef.componentInstance.infoClass = infoClass;
  }

  isAvoir(): boolean {
    return this.getTotalQtyProduit() - this.getTotalQtyServi() != 0;
  }

  getTotalQtyProduit(): number {
    return this.sale.salesLines.reduce((sum, current) => sum + current.quantityRequested, 0);
  }

  getTotalQtyServi(): number {
    return this.sale.salesLines.reduce((sum, current) => sum + current.quantitySold, 0);
  }

  onSaveSuccess(sale: ISales | null): void {
    this.isSaving = false;
    this.currentSaleService.setCurrentSale(sale);
    this.saveResponse.emit({ success: true });
    this.amountComputingComponent().computeMonnaie(null);
  }

  onSaveError(err: any): void {
    this.isSaving = false;
    this.saveResponse.emit({ success: true, error: err });
    /*const message = 'Une erreur est survenue';
    this.openInfoDialog(message, 'alert alert-danger');*/
  }

  subscribeToSaveResponse(result: Observable<HttpResponse<ISales>>): void {
    result.subscribe({
      next: (res: HttpResponse<ISales>) => this.onSaveSuccess(res.body),
      error: error => this.onSaveError(error),
    });
  }

  onLoadPrevente(): void {
    this.modeReglementComponent().buildPreventeReglementInput();
  }

  onFinalyseError(err: any): void {
    this.isSaving = false;
    this.responseEvent.emit({ error: err, success: false });
    const message = 'Une erreur est survenue';
    this.openInfoDialog(message, 'alert alert-danger');
  }

  onFinalyseSuccess(response: FinalyseSale | null, putOnStandBy: boolean = false): void {
    this.isSaving = false;
    this.responseEvent.emit({ saleId: response.saleId, success: true, putOnStandBy });
  }

  subscribeToFinalyseResponse(result: Observable<HttpResponse<FinalyseSale>>): void {
    result.subscribe({
      next: (res: HttpResponse<FinalyseSale>) => this.onFinalyseSuccess(res.body),
      error: err => this.onFinalyseError(err),
    });
  }

  subscribeToPutOnHoldResponse(result: Observable<HttpResponse<FinalyseSale>>): void {
    result.subscribe({
      next: (res: HttpResponse<FinalyseSale>) => this.onFinalyseSuccess(res.body, true),
      error: err => this.onFinalyseError(err),
    });
  }

  onSaveSaveError(err: any, sale?: ISales): void {
    this.isSaving = false;
    this.saveResponse.emit({ success: false, error: err });
    this.currentSaleService.setCurrentSale(sale);
  }

  buildModePayment(mode: IPaymentMode, entryAmount: number): Payment {
    const amount = this.sale.amountToBePaid - (entryAmount - mode.amount);
    return {
      ...new Payment(),
      paidAmount: amount,
      netAmount: amount,
      paymentMode: mode,
      montantVerse: mode.amount,
    };
  }

  protected subscribeToCreateSaleResponse(result: Observable<HttpResponse<ISales>>): void {
    result.subscribe({
      next: (res: HttpResponse<ISales>) => this.onSaleResponseSuccess(res.body),
      error: error => this.onSaveSaveError(error, this.sale),
    });
  }

  protected onSaleResponseSuccess(sale: ISales | null): void {
    this.isSaving = false;
    this.currentSaleService.setCurrentSale(sale);
    this.saveResponse.emit({ success: true });
  }
}
