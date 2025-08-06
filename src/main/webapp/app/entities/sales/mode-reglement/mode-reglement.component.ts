import {
  Component,
  computed,
  effect,
  ElementRef,
  inject,
  input,
  OnInit,
  output,
  signal,
  viewChild,
  viewChildren
} from '@angular/core';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { KeyFilterModule } from 'primeng/keyfilter';
import { InputTextModule } from 'primeng/inputtext';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { FormsModule } from '@angular/forms';
import { IPaymentMode, PaymentModeControl } from '../../../shared/model/payment-mode.model';
import { CurrentSaleService } from '../service/current-sale.service';
import { SelectModeReglementService } from '../service/select-mode-reglement.service';
import {
  CustomerDataTableComponent
} from '../uninsured-customer-list/customer-data-table.component';
import { BaseSaleService } from '../service/base-sale.service';
import { IPayment, Payment } from '../../../shared/model/payment.model';
import { PopoverModule } from 'primeng/popover';
import { ToggleSwitch } from 'primeng/toggleswitch';
import { InputGroupAddonModule } from 'primeng/inputgroupaddon';
import { InputGroupModule } from 'primeng/inputgroup';
import { PaymentModeCode } from '../../../shared/payment-mode';
import { Card } from 'primeng/card';

@Component({
  selector: 'jhi-mode-reglement',
  imports: [
    WarehouseCommonModule,
    KeyFilterModule,
    InputTextModule,
    ButtonModule,
    RippleModule,
    FormsModule,
    CustomerDataTableComponent,
    PopoverModule,
    ToggleSwitch,
    InputGroupAddonModule,
    InputGroupModule,
    Card,
  ],
  templateUrl: './mode-reglement.component.html',
})
export class ModeReglementComponent implements OnInit {
  readonly showModeReglementCard = input<boolean>(true);
  readonly paymentModeControlEvent = output<PaymentModeControl>();
  readonly onSaveEvent = output<boolean>();
  readonly onCloseEvent = output<boolean>();
  readonly isDiffere = input<boolean>(true);
  commentaire: string | null = null;
  referenceBancaire: string | null = null;
  banque: string | null = null;
  lieux: string | null = null;
  reglementsModes: IPaymentMode[];
  commentaireInput = viewChild<ElementRef>('commentaireInput');
  addOverlayPanel = viewChild<any>('addOverlayPanel');
  removeOverlayPanel = viewChild<any>('removeOverlayPanel');
  paymentModeInputs = viewChildren<ElementRef<HTMLInputElement>>('paymentModeInput');
  isShowAddBtn = signal(false);
  forceFocus = signal(false);
  currentSaleService = inject(CurrentSaleService);
  baseSaleService = inject(BaseSaleService);
  selectModeReglementService = inject(SelectModeReglementService);
  readonly bankRelatedModes = [PaymentModeCode.CB, PaymentModeCode.VIREMENT, PaymentModeCode.CH];
  readonly manageShowInfosBancaire = computed(() =>
    this.selectModeReglementService.modeReglements()?.some(element => this.bankRelatedModes.includes(element?.code as PaymentModeCode)),
  );
  protected printInvoice = false;
  protected paymentModeToChange: IPaymentMode | null = null;
  protected isSmallScreen = false;
  private lastModesCount = 0;

  constructor() {
    this.updateAvailableMode();
    effect(() => {
      const currentModes = this.selectModeReglementService.modeReglements();
      const newCount = currentModes.length;
      if (newCount > 0 && newCount > this.lastModesCount) {
        console.warn('focus last input', newCount);
        this.focusLastAddInput();
      }
      this.lastModesCount = newCount;
    });
  }

  ngOnInit(): void {
    this.isSmallScreen = window.innerWidth <= 1280;
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
      const modeToRemove = this.selectModeReglementService.modeReglements().find(el => el.code === old.code);
      if (modeToRemove) {
        this.selectModeReglementService.remove(modeToRemove);
        this.updateAvailableMode();
        this.manageShowAddButton(this.getInputSum());
      }
    }
  }

  manageShowAddButton(inputAmount: number): void {
    this.isShowAddBtn.set(
      this.selectModeReglementService.modeReglements().length < this.baseSaleService.maxModePayementNumber() &&
        inputAmount > 0 &&
        inputAmount < this.currentSaleService.currentSale().amountToBePaid,
    );
  }

  buildPreventeReglementInput(): void {
    this.selectModeReglementService.paymentModes.forEach(mode => {
      const el = this.currentSaleService.currentSale().payments.find(payment => payment.paymentMode.code === mode.code);
      if (el) {
        mode.amount = el.paidAmount;
        if (mode.code === PaymentModeCode.CASH && el.montantVerse) {
          mode.amount = el.montantVerse;
          this.currentSaleService.currentSale().montantVerse = el.montantVerse;
        }
        this.currentSaleService.setCurrentSale(this.currentSaleService.currentSale());
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

  onAddPaymentMode(newMode: IPaymentMode): void {
    const oldModes = this.selectModeReglementService.modeReglements();
    if (oldModes.length < this.baseSaleService.maxModePayementNumber()) {
      this.forceFocus.set(true);
      this.selectModeReglementService.setModePayments([...oldModes, newMode]);
      this.updateAvailableMode();
      this.addOverlayPanel().hide();
    }
    this.isShowAddBtn.set(this.selectModeReglementService.modeReglements().length < this.baseSaleService.maxModePayementNumber());
  }

  focusLastAddInput(): void {
    const inputs = this.paymentModeInputs();
    const lastInput = inputs[inputs.length - 1]?.nativeElement;
    if (lastInput) {
      lastInput.focus();
      const amountOfOtherModes = this.selectModeReglementService
        .modeReglements()
        .filter(e => e.code !== lastInput.id)
        .reduce((acc, e) => acc + (e.amount || 0), 0);

      const newAmount = this.currentSaleService.currentSale().amountToBePaid - amountOfOtherModes;
      const mode = this.selectModeReglementService.modeReglements().find(e => e.code === lastInput.id);
      if (mode) {
        mode.amount = newAmount;
      }
      lastInput.value = String(newAmount);
      setTimeout(() => lastInput.select(), 50);
    }
  }

  onRemovePaymentMode(newMode: IPaymentMode): void {
    this.changePaimentMode(newMode);
    this.removeOverlayPanel().hide();
  }

  changePaimentMode(newPaymentMode: IPaymentMode): void {
    const oldModes = this.selectModeReglementService.modeReglements();
    const oldIndex = oldModes.findIndex(el => el.code === this.paymentModeToChange?.code);
    if (oldIndex !== -1) {
      const updatedModes = [...oldModes];
      updatedModes[oldIndex] = newPaymentMode;
      this.selectModeReglementService.setModePayments(updatedModes);
      this.updateAvailableMode();
      setTimeout(() => this.manageAmountDiv(), 100);
    }
  }

  manageAmountDiv(): void {
    const firstInput = this.paymentModeInputs()[0]?.nativeElement;
    if (firstInput) {
      const mode = this.selectModeReglementService.modeReglements().find(e => e.code === firstInput.id);
      if (mode) {
        mode.amount = this.currentSaleService.currentSale().amountToBePaid;
      }
      firstInput.focus();
      setTimeout(() => firstInput.select(), 100);
    }
  }

  commentaireInputGetFocus(): void {
    setTimeout(() => this.commentaireInput()?.nativeElement.focus(), 20);
  }

  getInputSum(): number {
    return this.selectModeReglementService.modeReglements().reduce((sum, mode) => sum + (mode.amount || 0), 0);
  }

  onSansBonChange(evt: any): void {
    this.currentSaleService.setVenteSansBon(evt.checked);
  }

  onPrintInvoiceChange(evt: any): void {
    this.currentSaleService.updatePrintInvoice(evt.checked);
  }

  onPrintReceiptChange(evt: any): void {
    this.currentSaleService.updatePrintReceipt(evt.checked);
  }

  buildPayment(entryAmount: number): IPayment[] {
    return this.selectModeReglementService
      .modeReglements()
      .filter(mode => (mode.amount || 0) > 0)
      .map(mode => this.buildModePayment(mode, mode.amount, entryAmount));
  }

  protected onClose(op: any): void {
    this.onCloseEvent.emit(true);
    op.hide();
  }

  private updateAvailableMode(): void {
    this.reglementsModes = this.selectModeReglementService.getaAvaillablePaymentsMode();
  }

  private setFirstInputFocused(): void {
    if (!this.forceFocus()) {
      return;
    }
    queueMicrotask(() => {
      const firstInput = this.paymentModeInputs()[0]?.nativeElement;
      if (firstInput) {
        firstInput.focus();
        setTimeout(() => firstInput.select(), 50);
      }
    });
  }

  private buildModePayment(mode: IPaymentMode, inputAmount: number, entryAmount: number): Payment {
    const amount =
      entryAmount > this.currentSaleService.currentSale().amountToBePaid
        ? this.currentSaleService.currentSale().amountToBePaid - (entryAmount - inputAmount)
        : inputAmount;
    return {
      ...new Payment(),
      paidAmount: amount,
      netAmount: amount,
      paymentMode: mode,
      montantVerse: inputAmount,
    };
  }
}
