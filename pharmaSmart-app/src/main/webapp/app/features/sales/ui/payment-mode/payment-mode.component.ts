import { Component, computed, effect, ElementRef, inject, input, output, signal, viewChild, viewChildren, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NgbDropdownModule, NgbTooltipModule } from '@ng-bootstrap/ng-bootstrap';
import { InputNumberComponent, SwitchComponent } from '../../../../shared/ui';
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
    InputNumberComponent,
    SwitchComponent,
    NgbDropdownModule,
    NgbTooltipModule,
  ],
  templateUrl: './payment-mode.component.html',
  styleUrls: ['./payment-mode.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaymentModeComponent {
  // ===== Inputs =====
  readonly amountToBePaid = input.required<number>();
  readonly maxPaymentModes = input<number>(2);
  readonly showBankFields = input<boolean>(true);
  readonly isDiffere = input<boolean>(false); // Vente différée nécessite commentaire obligatoire
  readonly saleType = input<string>('COMPTANT');
  readonly hasSansBon = input<boolean>(false);

  // ===== Local State =====
  readonly isSmallScreen = signal<boolean>(window.innerWidth <= 1280);
  protected venteSansBon = signal<boolean>(false);

  // ===== Outputs =====
  readonly paymentComplete = output<PaymentCompleteEvent>();
  readonly validationError = output<string>();

  // ===== Services =====
  private readonly paymentModeManager = inject(PaymentModeManagerService);

  // ===== View Children =====
  private readonly paymentInputs = viewChildren(InputNumberComponent);
  private readonly commentInput = viewChild<ElementRef>('commentInput');

  // ===== State Signals =====
  readonly selectedModes = signal<PaymentModeEntry[]>([]);
  readonly isShowAddBtn = signal<boolean>(false);

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

  /**
   * Nombre de modes lors du dernier passage de l'effect ci-dessous — sert à ne
   * déclencher le focus automatique que lors d'un véritable AJOUT de mode, pas à
   * chaque frappe. `onAmountChanged` remplace le tableau `selectedModes` (nouvelle
   * référence) à chaque saisie pour forcer la détection de changement ; sans ce
   * garde-fou, l'effect se redéclenchait sur CHAQUE frappe (la longueur restant
   * `> 1` avec deux modes) et volait le focus vers le dernier champ en pleine
   * saisie, empêchant de taper ailleurs que dans ce dernier champ.
   */
  private previousModesCount = 0;

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
      if (modes.length > this.previousModesCount) {
        setTimeout(() => this.focusLastInput(), 0);
      }
      this.previousModesCount = modes.length;
    });
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
    // Update add button visibility
    this.isShowAddBtn.set(
      this.selectedModes().length < this.maxPaymentModes()
    );
  }

  /**
   * Supprime une ligne de règlement. N'est appelée que lorsqu'il reste plusieurs
   * lignes : sur la dernière ligne, le bouton devient un dropdown de remplacement
   * (voir template) — on ne descend donc jamais à zéro mode.
   * Le montant libéré est reversé sur la ligne restante (règle « l'autre ligne »).
   */
  onRemovePaymentMode(entry: PaymentModeEntry): void {
    this.selectedModes.update(modes => {
      const remaining = modes.filter(m => m !== entry);
      if (remaining.length === 0) {
        return remaining;
      }
      return this.redistributeTo(remaining, remaining[remaining.length - 1]);
    });

    this.manageShowAddButton(this.getInputSum());
  }

  /** Remplace le mode d'une ligne (seul chemin possible sur la dernière ligne). */
  onReplaceMode(oldEntry: PaymentModeEntry, newMode: IPaymentMode): void {
    this.selectedModes.update(modes =>
      modes.map(m =>
        m === oldEntry
          ? {
              mode: newMode,
              // Si CASH → ne pas pré-remplir ; si autre mode → pré-remplir avec le montant à payer
              amount: newMode.code === PaymentModeCode.CASH ? undefined : this.amountToBePaid(),
              amountEntered: undefined,
              isReadonly: newMode.isReadonly || false,
            }
          : m,
      ),
    );

    this.manageShowAddButton(this.getInputSum());

    // Focus on the replaced input
    setTimeout(() => this.focusFirstInput(), 100);
  }

  // ===== Amount Handling =====

  /**
   * Saisie d'un montant sur une ligne. Toutes les mises à jour sont immutables
   * (nouveaux objets + nouveau tableau) : avec OnPush + signals, une mutation en
   * place après le `update()` ne serait pas rendue de manière fiable.
   */
  onAmountChanged(entry: PaymentModeEntry, value: number | null): void {
    const amount = value ?? undefined;

    this.selectedModes.update(modes => {
      let updated = modes.map(m =>
        m === entry
          ? {
              ...m,
              amount,
              // Pour espèces, le montant saisi est aussi le montant versé (calcul de la monnaie)
              amountEntered: m.mode.code === PaymentModeCode.CASH ? amount : m.amountEntered,
            }
          : m,
      );

      // Répartition automatique quand le nombre max de modes est atteint : le reste
      // à payer est reversé sur l'autre ligne (celle qu'on n'est pas en train de saisir).
      //
      // HYPOTHÈSE MÉTIER : maxPaymentModes = 2, « l'autre ligne » est donc unique.
      // Si le maximum passe un jour à 3+, cette répartition est à généraliser.
      if (updated.length >= this.maxPaymentModes()) {
        const other = updated.find(m => m.mode.code !== entry.mode.code);
        if (other) {
          updated = this.redistributeTo(updated, other);
        }
      }

      return updated;
    });

    // Gérer l'affichage du bouton add
    this.manageShowAddButton(this.getInputSum());
  }

  /**
   * Reverse sur la ligne `target` le solde du montant à payer non couvert par
   * les autres lignes, de façon immutable. Pour une ligne CASH, `amountEntered`
   * est aligné sur le nouveau montant afin que le calcul de la monnaie ne reste
   * pas basé sur une saisie périmée.
   */
  private redistributeTo(modes: PaymentModeEntry[], target: PaymentModeEntry): PaymentModeEntry[] {
    const othersSum = modes
      .filter(m => m !== target)
      .reduce((sum, m) => sum + (m.amount || 0), 0);
    const amount = Math.max(0, this.amountToBePaid() - othersSum);

    return modes.map(m =>
      m === target
        ? {
            ...m,
            amount,
            amountEntered: m.mode.code === PaymentModeCode.CASH ? amount : m.amountEntered,
          }
        : m,
    );
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
        inputs[0].focus();
        // La sélection attend que le champ soit passé en mode saisie (valeur brute)
        setTimeout(() => inputs[0].select(), 50);
      }
    }, 0);
  }

  private focusLastInput(): void {
    setTimeout(() => {
      const inputs = this.paymentInputs();
      if (inputs.length > 0) {
        const lastInput = inputs[inputs.length - 1];
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
   * Rendu générique des modes : icône de police + libellé du référentiel + couleur CSS.
   * Un mode ajouté au référentiel s'affiche sans créer d'image : icône et couleur par
   * défaut, libellé fourni par le backend.
   */
  modeLabel(mode: IPaymentMode): string {
    return mode.libelle || this.getPaymentModeLabel(mode.code || '');
  }

  modeIcon(code: string | undefined): string {
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

  /** Suffixe de classe couleur : code du mode en minuscules, `default` si inconnu. */
  modeColorSuffix(code: string | undefined): string {
    return (code || 'default').toLowerCase();
  }

  modeChipClasses(mode: IPaymentMode): string {
    return `mode-chip mode-chip--${this.modeColorSuffix(mode.code)}`;
  }

  modeItemIconClasses(mode: IPaymentMode): string {
    return `${this.modeIcon(mode.code)} mode-item-icon mode-item-icon--${this.modeColorSuffix(mode.code)}`;
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

  /**
   * Méthode publique pour mettre le focus sur le premier mode (CASH)
   * Appelée depuis le parent après ouverture du modal paiement
   */
  public focusFirstMode(): void {
    this.focusFirstInput();
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
