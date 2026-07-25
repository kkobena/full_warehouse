import { computed, inject, Injectable, signal } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { IPaymentMode } from '../../../shared/model/payment-mode.model';
import { ModePaymentService } from '../../../entities/mode-payments/mode-payment.service';

/**
 * Référentiel des modes de règlement : chargement depuis l'API et enrichissement
 * (caractère readonly du montant).
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
   * Enrich payment mode with readonly settings. Le rendu (icône, couleur, libellé)
   * est entièrement dérivé en CSS/composant — plus d'images par mode à maintenir.
   * Seules les espèces acceptent un montant versé supérieur au dû (monnaie) ;
   * tout autre mode — y compris un nouveau mode du référentiel — est readonly.
   */
  private enrichPaymentMode(mode: IPaymentMode): IPaymentMode {
    return { ...mode, disabled: false, isReadonly: mode.code !== 'CASH' };
  }
}
