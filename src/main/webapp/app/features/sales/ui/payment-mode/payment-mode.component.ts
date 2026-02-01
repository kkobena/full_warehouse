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
  viewChildren,
  ChangeDetectionStrategy,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { InputNumberModule } from 'primeng/inputnumber';
import { PopoverModule } from 'primeng/popover';
import { CheckboxModule } from 'primeng/checkbox';
import { IPaymentMode } from '../../../../shared/model/payment-mode.model';
import { PaymentModeCode } from '../../../../shared/payment-mode';
import { ModePaymentService } from '../../../../entities/mode-payments/mode-payment.service';

/**
 * PaymentModeComponent
 * 
 * Component for selecting and managing payment methods (cash, card, mobile money, etc.)
 * Uses the new architecture pattern with signals and direct API service.
 * 
 * Features:
 * - Multiple payment modes (up to maxModePayementNumber)
 * - Auto-calculate change for cash payments
 * - Validate total paid amount
 * - Bank reference for cards/checks/transfers
 * - Print options (receipt, invoice)
 * 
 * @example
 * <app-payment-mode
 *   [amountToBePaid]="1000"
 *   [maxPaymentModes]="3"
 *   (paymentComplete)="onPaymentComplete($event)"
 * />
 */
@Component({
  selector: 'app-payment-mode',

  imports: [
    CommonModule,
    FormsModule,
    ButtonModule,
    InputTextModule,
    InputNumberModule,
    PopoverModule,
    CheckboxModule,
  ],
  templateUrl: './payment-mode.component.html',
  styleUrls: ['./payment-mode.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaymentModeComponent implements OnInit {
  // ===== Inputs =====
  readonly amountToBePaid = input.required<number>();
  readonly maxPaymentModes = input<number>(2);
  readonly showBankFields = input<boolean>(true);
  readonly isDiffere = input<boolean>(false); // Vente différée nécessite commentaire obligatoire

  // ===== Outputs =====
  readonly paymentComplete = output<PaymentCompleteEvent>();
  readonly validationError = output<string>();

  // ===== Services =====
  private readonly modePaymentService = inject(ModePaymentService);

  // ===== View Children =====
  private readonly paymentInputs = viewChildren<ElementRef<HTMLInputElement>>('paymentInput');
  private readonly commentInput = viewChild<ElementRef>('commentInput');
  private readonly addModePanel = viewChild<any>('addModePanel');

  // ===== State Signals =====
  readonly selectedModes = signal<PaymentModeEntry[]>([]);
  readonly allPaymentModes = signal<IPaymentMode[]>([]); // All available modes from API
  readonly availableModes = signal<IPaymentMode[]>([]); // Filtered modes (not yet selected)
  readonly comment = signal<string>('');
  readonly bankReference = signal<string>('');
  readonly bank = signal<string>('');
  readonly location = signal<string>('');
  readonly printReceipt = signal<boolean>(false);
  readonly printInvoice = signal<boolean>(false);

  // ===== Computed =====
  readonly totalPaid = computed(() => {
    return this.selectedModes().reduce((sum, entry) => sum + (entry.amount || 0), 0);
  });

  readonly remainingAmount = computed(() => {
    return Math.max(0, this.amountToBePaid() - this.totalPaid());
  });

  readonly changeAmount = computed(() => {
    const total = this.totalPaid();
    const due = this.amountToBePaid();
    const cashEntry = this.selectedModes().find(m => m.mode.code === PaymentModeCode.CASH);
    
    if (cashEntry && cashEntry.amountEntered && cashEntry.amountEntered > cashEntry.amount!) {
      return cashEntry.amountEntered - cashEntry.amount!;
    }
    
    return total > due ? total - due : 0;
  });

  readonly canAddMore = computed(() => {
    return this.selectedModes().length < this.maxPaymentModes() && this.remainingAmount() > 0;
  });

  readonly isComplete = computed(() => {
    return this.totalPaid() >= this.amountToBePaid();
  });

  readonly needsBankFields = computed(() => {
    const bankCodes = [PaymentModeCode.CB, PaymentModeCode.VIREMENT, PaymentModeCode.CH];
    return this.selectedModes().some(entry => bankCodes.includes(entry.mode.code as PaymentModeCode));
  });

  readonly PaymentModeCode = PaymentModeCode;

  constructor() {
    // Auto-focus last input when new mode added
    effect(() => {
      const modes = this.selectedModes();
      if (modes.length > 1) {
        setTimeout(() => this.focusLastInput(), 0);
      }
    });
  }

  ngOnInit(): void {
    this.loadAvailableModes();
    this.initializeWithCash();
  }

  // ===== Initialization =====

  private loadAvailableModes(): void {
    this.modePaymentService.query().subscribe({
      next: res => {
        if (res.body) {
          this.allPaymentModes.set(res.body);
          this.availableModes.set(res.body); // Initially all modes are available
        }
      },
      error: () => {
        this.validationError.emit('Erreur lors du chargement des modes de paiement');
      },
    });
  }

  private initializeWithCash(): void {
    const cashMode = this.availableModes().find(m => m.code === PaymentModeCode.CASH);
    if (cashMode) {
      this.selectedModes.set([{
        mode: cashMode,
        amount: this.amountToBePaid(),
        amountEntered: this.amountToBePaid(),
      }]);
      setTimeout(() => this.focusFirstInput(), 100);
    }
  }

  // ===== Payment Mode Management =====

  onAddPaymentMode(mode: IPaymentMode): void {
    if (this.selectedModes().length >= this.maxPaymentModes()) {
      this.validationError.emit('Nombre maximum de modes de paiement atteint');
      return;
    }

    const remaining = this.remainingAmount();
    const newEntry: PaymentModeEntry = {
      mode,
      amount: remaining,
      amountEntered: mode.code === PaymentModeCode.CASH ? remaining : undefined,
    };

    this.selectedModes.update(modes => [...modes, newEntry]);
    this.updateAvailableModes();
    this.addModePanel()?.hide();
  }

  onRemovePaymentMode(entry: PaymentModeEntry): void {
    if (this.selectedModes().length === 1) {
      this.validationError.emit('Au moins un mode de paiement est requis');
      return;
    }

    this.selectedModes.update(modes => modes.filter(m => m !== entry));
    this.updateAvailableModes();
    
    // Redistribute amount to first mode
    if (this.selectedModes().length > 0) {
      const first = this.selectedModes()[0];
      first.amount = this.amountToBePaid() - this.selectedModes()
        .filter(m => m !== first)
        .reduce((sum, m) => sum + (m.amount || 0), 0);
    }
  }

  onChangePaymentMode(oldEntry: PaymentModeEntry, newMode: IPaymentMode): void {
    this.selectedModes.update(modes => {
      const index = modes.indexOf(oldEntry);
      if (index !== -1) {
        const updated = [...modes];
        updated[index] = {
          mode: newMode,
          amount: oldEntry.amount,
          amountEntered: newMode.code === PaymentModeCode.CASH ? oldEntry.amountEntered : undefined,
        };
        return updated;
      }
      return modes;
    });
    this.updateAvailableModes();
  }

  // ===== Amount Handling =====

  onAmountChange(entry: PaymentModeEntry, newAmount: number): void {
    entry.amount = newAmount;
    
    // For cash, entered amount = payment amount by default
    if (entry.mode.code === PaymentModeCode.CASH && !entry.amountEntered) {
      entry.amountEntered = newAmount;
    }
    
    this.selectedModes.update(m => [...m]); // Trigger change detection
  }

  onCashEnteredChange(entry: PaymentModeEntry, enteredAmount: number): void {
    if (entry.mode.code === PaymentModeCode.CASH) {
      entry.amountEntered = enteredAmount;
      entry.amount = Math.min(enteredAmount, this.remainingAmount() + (entry.amount || 0));
      this.selectedModes.update(m => [...m]);
    }
  }

  // ===== Validation & Submission =====

  validate(): boolean {
    if (!this.isComplete()) {
      this.validationError.emit(`Montant insuffisant. Reste à payer: ${this.remainingAmount()}`);
      return false;
    }

    if (this.needsBankFields() && !this.bankReference()) {
      this.validationError.emit('Référence bancaire requise');
      return false;
    }

    if (this.isDiffere() && !this.comment().trim()) {
      this.validationError.emit('Commentaire obligatoire pour les ventes différées');
      return false;
    }

    return true;
  }

  submit(): void {
    if (!this.validate()) {
      return;
    }

    const event: PaymentCompleteEvent = {
      payments: this.selectedModes().map(entry => ({
        mode: entry.mode,
        amount: entry.amount!,
        amountEntered: entry.amountEntered,
      })),
      totalPaid: this.totalPaid(),
      change: this.changeAmount(),
      comment: this.comment(),
      bankReference: this.needsBankFields() ? this.bankReference() : undefined,
      bank: this.needsBankFields() ? this.bank() : undefined,
      location: this.needsBankFields() ? this.location() : undefined,
      printReceipt: this.printReceipt(),
      printInvoice: this.printInvoice(),
    };

    this.paymentComplete.emit(event);
  }

  // ===== Helpers =====

  private updateAvailableModes(): void {
    const usedCodes = this.selectedModes().map(e => e.mode.code);
    const allModes = this.allPaymentModes();
    this.availableModes.set(allModes.filter((m: IPaymentMode) => !usedCodes.includes(m.code)));
  }

  private focusFirstInput(): void {
    setTimeout(() => {
      const inputs = this.paymentInputs();
      if (inputs.length > 0) {
        inputs[0].nativeElement.focus();
        inputs[0].nativeElement.select();
      }
    }, 0);
  }

  private focusLastInput(): void {
    setTimeout(() => {
      const inputs = this.paymentInputs();
      if (inputs.length > 0) {
        const lastInput = inputs[inputs.length - 1].nativeElement;
        lastInput.focus();
        lastInput.select();
      }
    }, 0);
  }

  getPaymentModeLabel(code: string): string {
    switch (code) {
      case PaymentModeCode.CASH: return 'Espèces';
      case PaymentModeCode.CB: return 'Carte Bancaire';
      case PaymentModeCode.OM: return 'Orange Money';
      case PaymentModeCode.WAVE: return 'Wave';
      case PaymentModeCode.MOOV: return 'Moov Money';
      case PaymentModeCode.MTN: return 'MTN Mobile Money';
      case PaymentModeCode.VIREMENT: return 'Virement';
      case PaymentModeCode.CH: return 'Chèque';
      default: return code;
    }
  }

  getPaymentModeIcon(code: string): string {
    switch (code) {
      case PaymentModeCode.CASH: return 'pi pi-money-bill';
      case PaymentModeCode.CB: return 'pi pi-credit-card';
      case PaymentModeCode.OM:
      case PaymentModeCode.WAVE:
      case PaymentModeCode.MOOV:
      case PaymentModeCode.MTN:
        return 'pi pi-mobile';
      case PaymentModeCode.VIREMENT: return 'pi pi-building';
      case PaymentModeCode.CH: return 'pi pi-file';
      default: return 'pi pi-wallet';
    }
  }
}

// ===== Types =====

export interface PaymentModeEntry {
  mode: IPaymentMode;
  amount?: number;
  amountEntered?: number; // For cash: montant versé
}

export interface PaymentCompleteEvent {
  payments: Array<{
    mode: IPaymentMode;
    amount: number;
    amountEntered?: number;
  }>;
  totalPaid: number;
  change: number;
  comment?: string;
  bankReference?: string;
  bank?: string;
  location?: string;
  printReceipt: boolean;
  printInvoice: boolean;
}
