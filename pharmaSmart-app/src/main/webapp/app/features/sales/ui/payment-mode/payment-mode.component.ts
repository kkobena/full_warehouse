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
import { ToggleSwitchModule } from 'primeng/toggleswitch';
import { InputGroupModule } from 'primeng/inputgroup';
import { InputGroupAddonModule } from 'primeng/inputgroupaddon';
import { KeyFilterModule } from 'primeng/keyfilter';
import { IPaymentMode } from '../../../../shared/model/payment-mode.model';
import { PaymentModeCode } from '../../../../shared/payment-mode';
import { PaymentModeManagerService } from '../../services/payment-mode-manager.service';

/**
 * Seuil de tolérance pour considérer qu'il y a de la monnaie à rendre
 * Si la monnaie est <= 5 FCFA, on ne la rend pas (arrondi/pourboire)
 */
const CHANGE_TOLERANCE_THRESHOLD = 5;

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
    ToggleSwitchModule,
    InputGroupModule,
    InputGroupAddonModule,
    KeyFilterModule,
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
  readonly saleType = input<string>('COMPTANT');
  readonly hasSansBon = input<boolean>(false);

  // ===== Local State =====
  readonly isSmallScreen = signal<boolean>(window.innerWidth <= 1280);
  readonly venteSansBon = signal<boolean>(false);

  // ===== Outputs =====
  readonly paymentComplete = output<PaymentCompleteEvent>();
  readonly validationError = output<string>();

  // ===== Services =====
  private readonly paymentModeManager = inject(PaymentModeManagerService);

  // ===== View Children =====
  private readonly paymentInputs = viewChildren<ElementRef<HTMLInputElement>>('paymentInput');
  private readonly commentInput = viewChild<ElementRef>('commentInput');
  private readonly addModePanel = viewChild<any>('addModePanel');
  private readonly removeModePanel = viewChild<any>('removeModePanel');

  // ===== State Signals =====
  readonly selectedModes = signal<PaymentModeEntry[]>([]);
  readonly isShowAddBtn = signal<boolean>(false);
  private readonly modeToReplace = signal<PaymentModeEntry | null>(null);
  
  // Use computed signals from the service
  readonly availableModes = computed(() => {
    const usedCodes = this.selectedModes().map(e => e.mode.code);
    return this.paymentModeManager.modes().filter((m: IPaymentMode) => !usedCodes.includes(m.code));
  })
  readonly comment = signal<string>('');
  readonly bankReference = signal<string>('');
  readonly bank = signal<string>('');
  readonly location = signal<string>('');
  readonly printReceipt = signal<boolean>(false);
  readonly printInvoice = signal<boolean>(false);

  // ===== Computed =====
  readonly totalPaid = computed(() => {
    return this.selectedModes().reduce((sum, entry) => {
      if (entry.mode.code === PaymentModeCode.CASH) {
        // Pour CASH, le montant effectif payé est min(amount, amountToBePaid)
        return sum + Math.min(entry.amount || 0, this.amountToBePaid());
      }
      return sum + (entry.amount || 0);
    }, 0);
  });

  readonly remainingAmount = computed(() => {
    return Math.max(0, this.amountToBePaid() - this.totalPaid());
  });

  readonly changeAmount = computed(() => {
    const cashEntry = this.selectedModes().find(m => m.mode.code === PaymentModeCode.CASH);
    
    if (cashEntry) {
      // Utiliser amountEntered (montant versé) pour calculer la monnaie
      const cashGiven = cashEntry.amountEntered ?? cashEntry.amount ?? 0;
      const change = cashGiven - this.amountToBePaid();
      
      // Seuil de tolérance: on ne rend pas la monnaie si <= 5 FCFA
      if (change > CHANGE_TOLERANCE_THRESHOLD) {
        // Arrondir au multiple de 5 supérieur (favorise le client)
        return Math.ceil(change / 5) * 5;
      }
    }
    
    return 0;
  });

  readonly changeExact = computed(() => {
    const cashEntry = this.selectedModes().find(m => m.mode.code === PaymentModeCode.CASH);
    
    if (cashEntry) {
      // Montant exact de monnaie (pour comptabilité backend)
      const cashGiven = cashEntry.amountEntered ?? cashEntry.amount ?? 0;
      const change = cashGiven - this.amountToBePaid();
      return Math.max(0, change);
    }
    
    return 0;
  });



  readonly isComplete = computed(() => {
    // Pour calculer si paiement complet, on considère le montant effectif payé
    // (pour CASH, c'est min(amount, amountToBePaid) car l'excédent = monnaie)
    const effectivePaid = this.selectedModes().reduce((sum, entry) => {
      if (entry.mode.code === PaymentModeCode.CASH) {
        // Pour CASH, ne compter que jusqu'à amountToBePaid (le reste = monnaie)
        return sum + Math.min(entry.amount || 0, this.amountToBePaid());
      }
      return sum + (entry.amount || 0);
    }, 0);
    return effectivePaid >= this.amountToBePaid();
  });

  readonly needsBankFields = computed(() => {
    const bankCodes = [PaymentModeCode.CB, PaymentModeCode.VIREMENT, PaymentModeCode.CH];
    return this.selectedModes().some(entry => bankCodes.includes(entry.mode.code as PaymentModeCode));
  });

  readonly PaymentModeCode = PaymentModeCode;

  constructor() {
    // Initialize with CASH as soon as modes are loaded
    effect(() => {
      const modes = this.paymentModeManager.modes();
      if (modes.length > 0 && this.selectedModes().length === 0) {
        this.initializeWithCash();
      }
    });

    // Auto-focus last input when new mode added
    effect(() => {
      const modes = this.selectedModes();
      if (modes.length > 1) {
        setTimeout(() => this.focusLastInput(), 0);
      }
    });
  }

  ngOnInit(): void {
    // Initialization handled by effect in constructor
  }

  // ===== Initialization =====

  private initializeWithCash(): void {
    // Get CASH mode from the manager service
    const cashMode = this.paymentModeManager.getCashMode();
    if (cashMode) {
      this.selectedModes.set([{
        mode: cashMode,
        amount: undefined, // Don't pre-fill the amount
        amountEntered: undefined,
        isReadonly: cashMode.isReadonly || false,
      }]);
      // Initialize add button state
      this.isShowAddBtn.set(false);
      // Focus immediately after initialization
      queueMicrotask(() => this.focusFirstInput());
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
      isReadonly: mode.isReadonly || false,
    };

    this.selectedModes.update(modes => [...modes, newEntry]);
    this.addModePanel()?.hide();
    // Update add button visibility
    this.isShowAddBtn.set(
      this.selectedModes().length < this.maxPaymentModes()
    );
  }

  onRemovePaymentMode(entry: PaymentModeEntry, event: Event): void {
    if (this.selectedModes().length === 1) {
      // Un seul mode → Ouvrir le popover pour REMPLACER
      this.modeToReplace.set(entry);
      this.removeModePanel()?.toggle(event);
    } else {
      // Plusieurs modes → SUPPRIMER directement
      this.selectedModes.update(modes => modes.filter(m => m !== entry));
      
      // Recalculate and update add button visibility
      this.manageShowAddButton(this.getInputSum());
      
      // Redistribute amount to first mode
      if (this.selectedModes().length > 0) {
        const first = this.selectedModes()[0];
        first.amount = this.amountToBePaid() - this.selectedModes()
          .filter(m => m !== first)
          .reduce((sum, m) => sum + (m.amount || 0), 0);
      }
    }
  }

  onConfirmReplaceMode(newMode: IPaymentMode): void {
    const oldEntry = this.modeToReplace();
    if (!oldEntry) return;

    // Remplacer le mode
    this.selectedModes.update(modes => {
      const index = modes.indexOf(oldEntry);
      if (index !== -1) {
        const updated = [...modes];
        // Si CASH → ne pas pré-remplir (undefined)
        // Si autre mode → pré-remplir avec le montant à payer
        const newAmount = newMode.code === PaymentModeCode.CASH ? undefined : this.amountToBePaid();
        updated[index] = {
          mode: newMode,
          amount: newAmount,
          amountEntered: newMode.code === PaymentModeCode.CASH ? undefined : undefined,
          isReadonly: newMode.isReadonly || false,
        };
        return updated;
      }
      return modes;
    });

    // Fermer le popover et réinitialiser
    this.removeModePanel()?.hide();
    this.modeToReplace.set(null);
    
    // Recalculate and update add button visibility
    this.manageShowAddButton(this.getInputSum());
    
    // Focus on the replaced input
    setTimeout(() => this.focusFirstInput(), 100);
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
    // NOTE: On ne bloque plus sur montant insuffisant car le parent (sale-creation)
    // gère la proposition de vente différée via processPaymentValidation()
    // if (!this.isComplete()) {
    //   this.validationError.emit(`Montant insuffisant. Reste à payer: ${this.remainingAmount()}`);
    //   return false;
    // }

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
    // Ne pas soumettre si un dialogue de confirmation est ouvert
    // Évite de finaliser la vente quand on confirme un dialogue (ex: annulation)
    if (document.querySelector('.confirm-dialog-modal')) {
      return;
    }

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
      changeExact: this.changeExact(),
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

  showPaymentCard(): boolean {
    return this.amountToBePaid() > 0;
  }

  showVenteSansBon(): boolean {
    return this.saleType() === 'VO' && this.hasSansBon();
  }

  onSansBonChange(event: any): void {
    // Émettre l'événement si nécessaire
    console.log('Vente sans bon:', this.venteSansBon());
  }

  onAmountInput(entry: PaymentModeEntry, event: any): void {
    const value = Number(event.target.value);
    if (entry.mode.code === PaymentModeCode.CASH) {
      // Pour espèces, accepter n'importe quel montant (pour gérer la monnaie)
      // On conserve le montant saisi dans amountEntered pour calculer la monnaie
      entry.amountEntered = value;
      entry.amount = value; // Ne plus limiter au montant à payer
    } else {
      entry.amount = value;
    }

    const modes = this.selectedModes();
    // Si on a atteint le nombre max de modes (répartition dynamique)
    if (modes.length >= this.maxPaymentModes()) {
      // Trouver l'autre mode et lui attribuer le reste
      const otherMode = modes.find(m => m !== entry);
      if (otherMode) {
        const remaining = this.amountToBePaid() - (entry.amount || 0);
        otherMode.amount = Math.max(0, remaining);
      }
    }
    
    // Forcer la détection des changements
    this.selectedModes.set([...this.selectedModes()]);
    // Gérer l'affichage du bouton add
    this.manageShowAddButton(this.getInputSum());
  }

  private manageShowAddButton(inputAmount: number): void {
    const numericAmount = this.parseAmount(inputAmount);
    this.isShowAddBtn.set(
      this.selectedModes().length < this.maxPaymentModes() &&
        numericAmount > 0 &&
        numericAmount < this.amountToBePaid()
    );
  }

  private getInputSum(): number {
    const modes = this.selectedModes() || [];
    return modes.reduce((sum, entry) => {
      const parsed = this.parseAmount(entry?.amount);
      return sum + parsed;
    }, 0);
  }

  private parseAmount(value: any): number {
    if (value === null || value === undefined || value === '') {
      return 0;
    }
    const num = typeof value === 'string' ? parseInt(value.trim(), 10) : value;
    return isNaN(num) ? 0 : num;
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
        // Delay select slightly to ensure focus is complete
        setTimeout(() => lastInput.select(), 50);
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
  
  /**
   * Méthode publique pour récupérer le montant total saisi
   * Utilisée par le composant parent pour récupérer le montant avant validation
   */
  getEntryAmount(): number {
    return this.totalPaid();
  }
  
  /**
   * Méthode publique pour récupérer le commentaire
   */
  getComment(): string {
    return this.comment();
  }
  
  /**
   * Méthode publique pour récupérer les informations bancaires
   */
  getBankInfo(): { reference: string; bank: string; location: string } {
    return {
      reference: this.bankReference(),
      bank: this.bank(),
      location: this.location(),
    };
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

  /**
   * ✅ AJOUT Phase 4.3: Méthode publique pour mettre le focus sur le premier mode (CASH)
   * Appelée depuis le parent après ouverture du modal paiement
   */
  public focusFirstMode(): void {
    // Le premier mode est toujours CASH, on cherche son input
    setTimeout(() => {
      const firstInput = document.querySelector('.payment-mode-input input') as HTMLInputElement;
      if (firstInput) {
        firstInput.focus();
        firstInput.select();
      } else {
        // Alternative: chercher par ID (code du mode CASH)
        const cashInput = document.getElementById('CASH') as HTMLInputElement;
        if (cashInput) {
          cashInput.focus();
          cashInput.select();
        }
      }
    }, 100);
  }

  /**
   * Méthode publique pour mettre le focus sur le champ commentaire
   * Utilisée après sélection client pour vente différée
   */
  public focusCommentInput(): void {
    setTimeout(() => {
      const input = this.commentInput()?.nativeElement;
      if (input) {
        input.focus();
      }
    }, 100);
  }
}

// ===== Types =====

export interface PaymentModeEntry {
  mode: IPaymentMode;
  amount?: number;
  amountEntered?: number; // For cash: montant versé
  isReadonly?: boolean; // Pour rendre le champ readonly
}

export interface PaymentCompleteEvent {
  payments: Array<{
    mode: IPaymentMode;
    amount: number;
    amountEntered?: number;
  }>;
  totalPaid: number;
  change: number; // Monnaie arrondie affichée
  changeExact: number; // Monnaie exacte pour comptabilité
  comment?: string;
  bankReference?: string;
  bank?: string;
  location?: string;
  printReceipt: boolean;
  printInvoice: boolean;
}

