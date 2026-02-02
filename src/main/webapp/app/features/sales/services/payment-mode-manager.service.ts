import { computed, inject, Injectable, signal } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { toSignal } from '@angular/core/rxjs-interop';
import { IPaymentMode } from '../../../shared/model/payment-mode.model';
import { ModePaymentService } from '../../../entities/mode-payments/mode-payment.service';

export interface PaymentModeEntry {
  mode: IPaymentMode;
  amount?: number;
  amountEntered?: number;
  isReadonly: boolean;
}

/**
 * Modern payment mode manager service using Angular signals.
 * Manages payment mode selection, CSS class mapping, and amount tracking.
 */
@Injectable({
  providedIn: 'root',
})
export class PaymentModeManagerService {
  private readonly modePaymentService = inject(ModePaymentService);

  // All available payment modes from API
  private readonly allModes = signal<IPaymentMode[]>([]);
  
  // Currently selected payment modes
  private readonly selectedModes = signal<IPaymentMode[]>([]);

  // Computed: Available modes not yet selected
  readonly availableModes = computed(() => {
    const selected = this.selectedModes();
    return this.allModes().filter(mode => 
      !selected.some(s => s.code === mode.code)
    );
  });

  // Computed: Currently selected modes (readonly)
  readonly currentModes = computed(() => this.selectedModes());

  // Computed: All modes (readonly)
  readonly modes = computed(() => this.allModes());

  constructor() {
    this.loadPaymentModes();
  }

  /**
   * Load payment modes from API and initialize with CASH mode
   */
  private loadPaymentModes(): void {
    this.modePaymentService.query().subscribe((res: HttpResponse<IPaymentMode[]>) => {
      const modes = res.body?.map(mode => this.enrichPaymentMode(mode)) || [];
      this.allModes.set(modes);
      this.initializeWithCash();
    });
  }

  /**
   * Enrich payment mode with CSS classes and readonly settings
   */
  private enrichPaymentMode(mode: IPaymentMode): IPaymentMode {
    const enriched = { ...mode, disabled: false };
    
    switch (mode.code) {
      case 'CASH':
        enriched.styleImageClass = 'cash';
        enriched.styleBtnClass = 'cash-btn';
        enriched.isReadonly = false;
        break;
      case 'WAVE':
        enriched.styleImageClass = 'wave';
        enriched.styleBtnClass = 'wave-btn';
        enriched.isReadonly = true;
        break;
      case 'OM':
        enriched.styleImageClass = 'om';
        enriched.styleBtnClass = 'om-btn';
        enriched.isReadonly = true;
        break;
      case 'CB':
        enriched.styleImageClass = 'cb';
        enriched.styleBtnClass = 'cb-btn';
        enriched.isReadonly = true;
        break;
      case 'MOOV':
        enriched.styleImageClass = 'moov';
        enriched.styleBtnClass = 'moov-btn';
        enriched.isReadonly = true;
        break;
      case 'MTN':
        enriched.styleImageClass = 'mtn';
        enriched.styleBtnClass = 'mtn-btn';
        enriched.isReadonly = true;
        break;
      case 'CH':
        enriched.styleImageClass = 'cheque';
        enriched.styleBtnClass = 'cheque-btn';
        enriched.isReadonly = true;
        break;
      case 'VIREMENT':
        enriched.styleImageClass = 'virement';
        enriched.styleBtnClass = 'virement-btn';
        enriched.isReadonly = true;
        break;
      default:
        enriched.styleImageClass = 'default';
        enriched.styleBtnClass = 'default-btn';
        enriched.isReadonly = false;
        break;
    }
    
    return enriched;
  }

  /**
   * Initialize with CASH payment mode selected
   */
  private initializeWithCash(): void {
    const cashMode = this.allModes().find(mode => mode.code === 'CASH');
    if (cashMode) {
      this.selectedModes.set([{ ...cashMode, amount: undefined }]);
    }
  }

  /**
   * Add a payment mode to the selection
   */
  addMode(mode: IPaymentMode): void {
    const current = this.selectedModes();
    if (!current.some(m => m.code === mode.code)) {
      this.selectedModes.set([...current, { ...mode, amount: undefined }]);
    }
  }

  /**
   * Remove a payment mode from the selection
   */
  removeMode(modeCode: string): void {
    const current = this.selectedModes();
    this.selectedModes.set(current.filter(m => m.code !== modeCode));
  }

  /**
   * Update the amount for a specific payment mode
   */
  updateModeAmount(modeCode: string, amount: number | undefined): void {
    const current = this.selectedModes();
    this.selectedModes.set(
      current.map(m => m.code === modeCode ? { ...m, amount } : m)
    );
  }

  /**
   * Reset all amounts to undefined
   */
  resetAmounts(): void {
    const current = this.selectedModes();
    this.selectedModes.set(current.map((m: IPaymentMode): IPaymentMode => ({ ...m, amount: undefined })));
  }

  /**
   * Reset to initial state (only CASH selected, no amounts)
   */
  reset(): void {
    this.resetAmounts();
    this.initializeWithCash();
  }

  /**
   * Set the selected modes (useful for restoring state)
   */
  setSelectedModes(modes: IPaymentMode[]): void {
    this.selectedModes.set(modes);
  }

  /**
   * Get the CASH payment mode
   */
  getCashMode(): IPaymentMode | undefined {
    return this.allModes().find(mode => mode.code === 'CASH');
  }

  /**
   * Calculate total of all payment mode amounts
   */
  getTotalAmount(): number {
    return this.selectedModes()
      .reduce((sum, mode) => sum + (mode.amount || 0), 0);
  }
}
