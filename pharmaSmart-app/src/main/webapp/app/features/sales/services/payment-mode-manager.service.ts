import { computed, inject, Injectable, signal } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { IPaymentMode } from '../../../shared/model/payment-mode.model';
import { ModePaymentService } from '../../../entities/mode-payments/mode-payment.service';

/**
 * Référentiel des modes de règlement : chargement depuis l'API et enrichissement
 * (classes CSS des images, caractère readonly du montant).
 *
 * La sélection des modes d'une vente (lignes, montants) appartient au composant
 * `PaymentModeComponent` — ce service ne porte volontairement aucun état de vente,
 * et la seule définition de `PaymentModeEntry` est celle du composant.
 */
@Injectable({
  providedIn: 'root',
})
export class PaymentModeManagerService {
  private readonly modePaymentService = inject(ModePaymentService);

  // All available payment modes from API
  private readonly allModes = signal<IPaymentMode[]>([]);

  // Computed: All modes (readonly)
  readonly modes = computed(() => this.allModes());

  constructor() {
    this.loadPaymentModes();
  }

  /**
   * Get the CASH payment mode
   */
  getCashMode(): IPaymentMode | undefined {
    return this.allModes().find(mode => mode.code === 'CASH');
  }

  /**
   * Load payment modes from API
   */
  private loadPaymentModes(): void {
    this.modePaymentService.query().subscribe((res: HttpResponse<IPaymentMode[]>) => {
      const modes = res.body?.map(mode => this.enrichPaymentMode(mode)) || [];
      this.allModes.set(modes);
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
}
