import { inject, Injectable, signal, WritableSignal } from '@angular/core';
import { CurrentSaleService } from './current-sale.service';
import { IPaymentMode, PaymentModeControl } from '../../../shared/model/payment-mode.model';
import { IPayment, Payment } from '../../../shared/model/payment.model';
import { SelectModeReglementService } from './select-mode-reglement.service';
import { ModeReglementComponent } from '../mode-reglement/mode-reglement.component';
import { AmountComputingComponent } from '../selling-home/comptant/amount-computing/amount-computing.component';
import { SaleEventManager } from './sale-event-manager.service';
import { FinalyseSale, InputToFocus, ISales, SaveResponse, StockError } from '../../../shared/model/sales.model';
import { ISalesLine } from '../../../shared/model/sales-line.model';

@Injectable({
  providedIn: 'root',
})
export class BaseSaleService {
  modeReglementComponent: WritableSignal<ModeReglementComponent> = signal<ModeReglementComponent>(null);
  amountComputingComponent: WritableSignal<AmountComputingComponent> = signal<AmountComputingComponent>(null);
  currentSaleService = inject(CurrentSaleService);
  selectModeReglementService = inject(SelectModeReglementService);
  entryAmount: number;
  readonly CASH = 'CASH';
  private readonly saleEventManager = inject(SaleEventManager);

  constructor() {}

  manageAmountDiv(): void {
    this.modeReglementComponent().manageAmountDiv();
  }

  computExtraInfo(): void {
    this.currentSaleService.currentSale().commentaire = this.modeReglementComponent().commentaire;
  }

  setAmountComputingComponent(amountComputingComponent: AmountComputingComponent): void {
    this.amountComputingComponent.set(amountComputingComponent);
  }

  setModeReglementComponent(modeReglementComponent: ModeReglementComponent): void {
    this.modeReglementComponent.set(modeReglementComponent);
  }

  isAvoir(): boolean {
    return this.getTotalQtyProduit() - this.getTotalQtyServi() != 0;
  }

  getTotalQtyProduit(): number {
    return this.currentSaleService.currentSale().salesLines.reduce((sum, current) => sum + current.quantityRequested, 0);
  }

  getTotalQtyServi(): number {
    return this.currentSaleService.currentSale().salesLines.reduce((sum, current) => sum + current.quantitySold, 0);
  }

  onLoadPrevente(): void {
    this.modeReglementComponent().buildPreventeReglementInput();
  }

  buildModePayment(mode: IPaymentMode, entryAmount: number): Payment {
    const amount = this.currentSaleService.currentSale().amountToBePaid - (entryAmount - mode.amount);
    return {
      ...new Payment(),
      paidAmount: amount,
      netAmount: amount,
      paymentMode: mode,
      montantVerse: mode.amount,
    };
  }

  getEntryAmount(): number {
    return this.modeReglementComponent().getInputSum();
  }

  manageCashPaymentMode(paymentModeControl: PaymentModeControl): void {
    const modes = this.selectModeReglementService.modeReglements();
    if (modes.length === 2) {
      const amount = this.getEntryAmount();
      modes.find((e: IPaymentMode) => e.code !== paymentModeControl.control.target.id).amount =
        this.currentSaleService.currentSale().amountToBePaid - paymentModeControl.paymentMode.amount;

      this.amountComputingComponent().computeMonnaie(amount);
    } else {
      this.amountComputingComponent().computeMonnaie(Number(paymentModeControl.control.target.value));
    }
    this.modeReglementComponent().showAddModePaymentButton(paymentModeControl.paymentMode);
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

  buildPayment(entryAmount: number): IPayment[] {
    return this.selectModeReglementService
      .modeReglements()
      .filter((m: IPaymentMode) => m.amount)
      .map((mode: IPaymentMode) => this.buildModePayment(mode, entryAmount));
  }

  setInputBoxFocus(input: string): void {
    this.saleEventManager.broadcast({
      name: 'inputBoxFocus',
      content: new InputToFocus(input),
    });
  }

  onError(err: any): void {
    this.saleEventManager.broadcast({
      name: 'saveResponse',
      content: new SaveResponse(false, err),
    });
  }

  onSaveSuccess(sale: ISales | null): void {
    this.currentSaleService.setCurrentSale(sale);
    this.saleEventManager.broadcast({
      name: 'saveResponse',
      content: new SaveResponse(true),
    });

    this.amountComputingComponent().computeMonnaie(null);
  }

  onSaveError(err: any, sale?: ISales): void {
    this.saleEventManager.broadcast({
      name: 'saveResponse',
      content: new SaveResponse(false, err),
    });
    this.currentSaleService.setCurrentSale(sale);
  }

  onFinalyseSuccess(response: FinalyseSale | null, putOnStandBy: boolean = false): void {
    this.saleEventManager.broadcast({
      name: 'responseEvent',
      content: new FinalyseSale(true, null, response.saleId, putOnStandBy),
    });
  }

  onSaleResponseSuccess(sale: ISales | null): void {
    this.currentSaleService.setCurrentSale(sale);
    this.saleEventManager.broadcast({
      name: 'saveResponse',
      content: new SaveResponse(true),
    });
  }

  onFinalyseError(err: any): void {
    this.saleEventManager.broadcast({
      name: 'responseEvent',
      content: new SaveResponse(false, err),
    });
  }

  onStockError(err: any, saleLine: ISalesLine): void {
    this.saleEventManager.broadcast({
      name: 'saveResponse',
      content: new StockError(err, saleLine),
    });
  }
}
