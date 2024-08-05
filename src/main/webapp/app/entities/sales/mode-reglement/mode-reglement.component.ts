import { Component, ElementRef, EventEmitter, inject, Inject, Input, OnInit, Output, signal, viewChild } from '@angular/core';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { InputSwitchModule } from 'primeng/inputswitch';
import { KeyFilterModule } from 'primeng/keyfilter';
import { InputTextModule } from 'primeng/inputtext';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { FormsModule } from '@angular/forms';
import { IPaymentMode, PaymentModeControl } from '../../../shared/model/payment-mode.model';
import { DOCUMENT } from '@angular/common';
import { OverlayPanel, OverlayPanelModule } from 'primeng/overlaypanel';
import { CurrentSaleService } from '../service/current-sale.service';
import { SelectModeReglementService } from '../service/select-mode-reglement.service';
import { CustomerDataTableComponent } from '../uninsured-customer-list/customer-data-table.component';
import { BaseSaleService } from '../service/base-sale.service';
import { IPayment, Payment } from '../../../shared/model/payment.model';

@Component({
  selector: 'jhi-mode-reglement',
  standalone: true,
  imports: [
    WarehouseCommonModule,
    InputSwitchModule,
    KeyFilterModule,
    InputTextModule,
    ButtonModule,
    RippleModule,
    FormsModule,
    OverlayPanelModule,
    CustomerDataTableComponent,
  ],
  templateUrl: './mode-reglement.component.html',
})
export class ModeReglementComponent implements OnInit {
  @Input() showModeReglementCard: boolean = true;
  @Output() paymentModeControlEvent = new EventEmitter<PaymentModeControl>();
  @Output() onSaveEvent = new EventEmitter<boolean>();
  @Output() onCloseEvent = new EventEmitter<boolean>();
  @Input('isDiffere') isDiffere: boolean = true;
  showInfosComplementaireReglementCard: boolean = false;
  showInfosBancaire: boolean = false;
  commentaire: string = null;
  referenceBancaire: string = null;
  banque: string = null;
  lieux: string = null;
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
  reglementsModes: IPaymentMode[];

  commentaireInput = viewChild<ElementRef>('commentaireInput');
  addOverlayPanel = viewChild<any>('addOverlayPanel');
  removeOverlayPanel = viewChild<any>('removeOverlayPanel');
  currentSaleService = inject(CurrentSaleService);
  baseSaleService = inject(BaseSaleService);
  selectModeReglementService = inject(SelectModeReglementService);
  isShowAddBtn = signal(false);
  protected printTicket = true;
  protected sansBon = false;
  protected printInvoice = false;
  protected paymentModeToChange: IPaymentMode | null;

  constructor(@Inject(DOCUMENT) private document: Document) {
    this.updateAvailableMode();
  }

  ngOnInit(): void {
    this.selectModeReglementService.resetAmounts();
    this.buildReglementInput();
  }

  save(): void {
    this.onSaveEvent.emit(true);
  }

  onAddPaymentModeToggle(old: IPaymentMode, evt: any): void {
    this.onModeBtnClick(old);
    this.addOverlayPanel().toggle(evt);
  }

  manageCashPaymentMode(evt: any, modePay: IPaymentMode): void {
    this.paymentModeControlEvent.emit({ paymentMode: modePay, control: evt });
  }

  onModeBtnClick(paymentMode: IPaymentMode): void {
    this.paymentModeToChange = paymentMode;
  }

  onRemovePaymentModeToggle(old: IPaymentMode, evt: any): void {
    if (this.selectModeReglementService.modeReglements().length === 1) {
      this.onModeBtnClick(old);
      this.removeOverlayPanel().toggle(evt);
    } else {
      const mds = this.selectModeReglementService.modeReglements();
      const modeToRemove = mds.find((el: IPaymentMode) => el.code === old.code);
      if (modeToRemove) {
        this.selectModeReglementService.remove(modeToRemove);
        this.updateAvailableMode();
        setTimeout(() => {
          this.manageShowAddButton(this.getInputSum());
        }, 30);
      }
    }
  }

  manageShowAddButton(inputAmount: number): void {
    this.isShowAddBtn.set(
      this.selectModeReglementService.modeReglements().length < this.baseSaleService.maxModePayementNumber() &&
        inputAmount > 0 &&
        inputAmount < this.currentSaleService.currentSale()?.amountToBePaid,
    );
  }

  manageShowInfosBancaire(): void {
    const mode = (element: IPaymentMode) => element.code === this.CB || this.VIREMENT || element.code === this.CH;
    this.showInfosBancaire = this.selectModeReglementService.modeReglements().some(mode);
  }

  buildPreventeReglementInput(): void {
    this.selectModeReglementService.paymentModes.forEach((mode: IPaymentMode) => {
      const el = this.currentSaleService.currentSale().payments.find(payment => payment.paymentMode.code === mode.code);
      if (el) {
        mode.amount = el.paidAmount;
        if (mode.code === this.CASH && el.montantVerse) {
          mode.amount = el.montantVerse;
          this.currentSaleService.currentSale().montantVerse = el.montantVerse;
        }
        this.currentSaleService.setCurrentSale(this.currentSaleService.currentSale());
        // this.selectModeReglementService.update(mode);
        this.updateAvailableMode();
      }
    });
    this.setFirstInputFocused();
  }

  buildReglementInput(): void {
    this.resetCashInput();
    this.setFirstInputFocused();
  }

  resetCashInput(): void {
    this.selectModeReglementService.selectCashModePayment();
  }

  getInputAtIndex(index: number | null): HTMLInputElement {
    const modeInputs = this.getInputElement();
    const indexAt = index === 0 ? index : modeInputs.length - 1;
    if (modeInputs && modeInputs.length > 0) {
      return modeInputs[indexAt];
    }
    return null;
  }

  onAddPaymentMode(newMode: IPaymentMode): void {
    const oldModes = this.selectModeReglementService.modeReglements();
    if (oldModes.length < this.baseSaleService.maxModePayementNumber()) {
      oldModes[oldModes.length++] = newMode;
      this.selectModeReglementService.setModePayments(oldModes);
      this.updateAvailableMode();
      this.addOverlayPanel().hide();
      setTimeout(() => {
        this.focusLastAddInput();
      }, 50);
    }
    this.isShowAddBtn.set(this.selectModeReglementService.modeReglements().length < this.baseSaleService.maxModePayementNumber());
  }

  focusLastAddInput(): void {
    const input = this.getInputAtIndex(null);
    if (input) {
      input.focus();
      const secondInputDefaultAmount =
        this.currentSaleService.currentSale().amountToBePaid -
        this.selectModeReglementService.modeReglements().find((e: IPaymentMode) => e.code !== input.id).amount;
      input.value = String(secondInputDefaultAmount);
      setTimeout(() => {
        input.select();
      }, 50);
    }
  }

  onRemovePaymentMode(newMode: IPaymentMode): void {
    this.changePaimentMode(newMode);
    this.removeOverlayPanel().hide();
  }

  changePaimentMode(newPaymentMode: IPaymentMode): void {
    const oldModes = this.selectModeReglementService.modeReglements();
    const oldIndex = oldModes.findIndex((el: IPaymentMode) => (el.code = this.paymentModeToChange.code));
    oldModes[oldIndex] = newPaymentMode;
    this.selectModeReglementService.setModePayments(oldModes);
    this.updateAvailableMode();
    setTimeout(() => {
      this.manageAmountDiv();
    }, 20);
  }

  manageAmountDiv(): void {
    const input = this.getInputAtIndex(0);
    if (input) {
      this.selectModeReglementService.modeReglements().find((e: IPaymentMode) => e.code === input.id).amount =
        this.currentSaleService.currentSale().amountToBePaid;
      input.focus();
      setTimeout(() => {
        input.select();
      }, 20);
    }
  }

  commentaireInputGetFocus(): void {
    setTimeout(() => {
      this.commentaireInput().nativeElement.focus();
    }, 20);
  }

  getInputSum(): number {
    const inputs = this.getInputElement();
    let sum = 0;
    inputs.forEach((input: HTMLInputElement) => {
      sum += Number(input.value);
    });
    return sum;
  }

  onSansBonChange(evt: any): void {
    this.currentSaleService.setVenteSansBon(evt.checked);
  }

  buildPayment(entryAmount: number): IPayment[] {
    const payments: IPayment[] = [];
    this.selectModeReglementService.modeReglements().forEach((mode: IPaymentMode) => {
      const input: HTMLInputElement = this.getInputElement().find((e: HTMLInputElement) => e.id === mode.code);
      if (input) {
        const amount = Number(input.value);
        if (amount > 0) {
          payments.push(this.buildModePayment(mode, amount, entryAmount));
        }
      }
    });

    return payments;
  }

  protected onClose(op: OverlayPanel): void {
    this.onCloseEvent.emit(true);
    op.hide();
  }

  private updateAvailableMode(): void {
    this.reglementsModes = this.selectModeReglementService.getaAvaillablePaymentsMode();
  }

  private setFirstInputFocused(): void {
    const input = this.getInputAtIndex(0);
    if (input) {
      input.focus();
      setTimeout(() => {
        input.select();
      }, 50);
    }
  }

  private getInputs(): Element[] {
    const inputs = this.document.querySelectorAll('.payment-mode-input');

    return Array.from(inputs);
  }

  private getInputElement(): HTMLInputElement[] {
    return this.getInputs() as HTMLInputElement[];
  }

  private buildModePayment(mode: IPaymentMode, inputAmount: number, entryAmount: number): Payment {
    const amount = this.currentSaleService.currentSale().amountToBePaid - (entryAmount - inputAmount);
    return {
      ...new Payment(),
      paidAmount: amount,
      netAmount: amount,
      paymentMode: mode,
      montantVerse: inputAmount,
    };
  }
}
