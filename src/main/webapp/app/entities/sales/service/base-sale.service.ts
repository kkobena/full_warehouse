import { computed, effect, inject, Injectable, signal, WritableSignal } from '@angular/core';
import { CurrentSaleService } from './current-sale.service';
import { IPaymentMode } from '../../../shared/model/payment-mode.model';
import { SelectModeReglementService } from './select-mode-reglement.service';
import { FinalyseSale, InputToFocus, ISales, SaleId, Sales, SaveResponse, StockError } from '../../../shared/model/sales.model';
import { ISalesLine } from '../../../shared/model/sales-line.model';
import { VoSalesService } from './vo-sales.service';
import { ConfigurationService } from '../../../shared/configuration.service';
import { IClientTiersPayant } from '../../../shared/model/client-tiers-payant.model';
import { Observable, switchMap } from 'rxjs';
import { HttpResponse } from '@angular/common/http';
import { SaleEventSignal } from '../selling-home/sale-event';

@Injectable({
  providedIn: 'root',
})
export class BaseSaleService {
  quantityMax: WritableSignal<number> = signal<number>(null);
  maxModePayementNumber: WritableSignal<number> = signal<number>(2);
  hasSansBon: WritableSignal<boolean> = signal<boolean>(null);
  selectModeReglementService = inject(SelectModeReglementService);
  entryAmount: number;
  readonly CASH = 'CASH';
  private readonly salesService = inject(VoSalesService);
  private readonly configurationService = inject(ConfigurationService);
  private readonly currentSaleService = inject(CurrentSaleService);
  totalQtyProduit = computed(() =>
    this.currentSaleService.currentSale().salesLines.reduce((sum, current) => sum + current.quantityRequested, 0),
  );
  totalQtyServi = computed(() => this.currentSaleService.currentSale().salesLines.reduce((sum, current) => sum + current.quantitySold, 0));
  private readonly saleEventManager = inject(SaleEventSignal);

  constructor() {
    effect(() => this.hasSaleWithoutBon());
    effect(() => this.getMaxToSale());
  }

  createSale(
    salesLine: ISalesLine,
    tiersPayants: IClientTiersPayant[],
    typePrescription: string,
    cassierId: number,
    sellerId: number,
    customerId: number,
    natureVente: string,
  ): ISales {
    return {
      ...new Sales(),
      salesLines: [salesLine],
      customerId,
      natureVente,
      typePrescription,
      cassierId,
      sellerId,
      type: 'VO',
      categorie: 'VO',
      tiersPayants,
    };
  }

  isAvoir(): boolean {
    return this.totalQtyProduit() - this.totalQtyServi() != 0;
  }

  getCashAmount(entryAmount: number): number {
    const modes = this.selectModeReglementService.modeReglements();
    let cashInput;
    this.entryAmount = entryAmount;
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
  }

  onSaveError(err: any, sale?: ISales): void {
    if (err.status === 412) {
      const errorPayload = err.error?.payload as ISales;
      this.currentSaleService.setCurrentSale(errorPayload);
    } else {
      this.currentSaleService.setCurrentSale(sale);
    }
    this.saleEventManager.broadcast({
      name: 'saveResponse',
      content: new SaveResponse(false, err),
    });
  }

  onCompleteSale(): void {
    this.saleEventManager.broadcast({
      name: 'completeSale',
      content: 'save',
    });
  }

  onStandby(): void {
    this.saleEventManager.broadcast({
      name: 'completeSale',
      content: 'standby',
    });
  }

  onFinalyseSuccess(response: FinalyseSale | null, putOnStandBy = false): void {
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
      // content: new SaveResponse(false, err),
      content: new FinalyseSale(false, err),
    });
  }

  onStockError(err: any, saleLine: ISalesLine): void {
    this.saleEventManager.broadcast({
      name: 'saveResponse',
      content: new StockError(err, saleLine),
    });
  }

  onRemoveThirdPartySaleLineToSalesSuccess(id: number): void {
    this.onSaveResponse(this.salesService.removeThirdPartySaleLineToSales(id, this.currentSaleService.currentSale().saleId));
  }

  getMaxToSale(): void {
    this.configurationService.find('APP_QTY_MAX').subscribe({
      next: res => this.quantityMax.set(Number(res.body?.value ?? 10000)),
      error: () => this.quantityMax.set(10000),
    });
  }

  hasSaleWithoutBon(): void {
    this.configurationService.find('APP_SANS_NUM_BON').subscribe({
      next: res => this.hasSansBon.set(Number(res.body?.value) === 1),
      error: () => this.hasSansBon.set(false),
    });
  }

  getMaxModePaymentNumber(): void {
    this.configurationService.find('APP_MODE_REGL_NUMBER').subscribe({
      next: res => this.maxModePayementNumber.set(Number(res.body?.value ?? 2)),
      error: () => this.maxModePayementNumber.set(2),
    });
  }

  onAddThirdPartySale(id: SaleId, clientTiersPayant: IClientTiersPayant): void {
    this.onSaveResponse(this.salesService.addComplementaireSales(id, clientTiersPayant));
  }

  private onSaveResponse(result: Observable<HttpResponse<ISales>>): void {
    result.pipe(switchMap(() => this.salesService.findForEdit(this.currentSaleService.currentSale().saleId))).subscribe({
      next: res => {
        this.currentSaleService.setCurrentSale(res.body);
        this.saleEventManager.broadcast({
          name: 'saveResponse',
          content: new SaveResponse(true),
        });
      },
      error: err => {
        this.saleEventManager.broadcast({
          name: 'saveResponse',
          content: new SaveResponse(false, err),
        });
      },
    });
  }
}
