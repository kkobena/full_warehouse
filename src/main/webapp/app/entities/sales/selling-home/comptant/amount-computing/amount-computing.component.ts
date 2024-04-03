import { Component, effect, Input } from '@angular/core';
import { ISales } from '../../../../../shared/model/sales.model';
import { WarehouseCommonModule } from '../../../../../shared/warehouse-common/warehouse-common.module';
import { IPaymentMode } from '../../../../../shared/model/payment-mode.model';
import { CurrentSaleService } from '../../../service/current-sale.service';
import { LastCurrencyGivenService } from '../../../service/last-currency-given.service';
import { SelectModeReglementService } from '../../../service/select-mode-reglement.service';

@Component({
  selector: 'jhi-amount-computing',
  standalone: true,
  imports: [WarehouseCommonModule],
  templateUrl: './amount-computing.component.html',
  styleUrl: './amount-computing.component.scss',
})
export class AmountComputingComponent {
  @Input() entryAmount: number = 0;
  monnaie = 0;
  modeReglementSelected: IPaymentMode[] = [];
  protected sale: ISales;
  /*
  derniere monnaie
   */
  protected derniereMonnaie: number = 0;

  constructor(
    private currentSaleService: CurrentSaleService,
    private lastCurrencyGivenService: LastCurrencyGivenService,
    private selectModeReglementService: SelectModeReglementService,
  ) {
    effect(() => {
      this.sale = this.currentSaleService.currentSale();
    });
    effect(() => {
      this.modeReglementSelected = this.selectModeReglementService.modeReglements();
    });
    effect(() => {
      this.derniereMonnaie = this.lastCurrencyGivenService.lastCurrency();
    });
  }

  computeMonnaie(amount: number | null): void {
    const thatentryAmount = amount || this.entryAmount;
    const thatMonnaie = thatentryAmount - this.sale?.amountToBePaid;
    this.monnaie = thatMonnaie > 0 ? thatMonnaie : 0;
    console.warn(this.monnaie, this.entryAmount, this.sale?.amountToBePaid, thatMonnaie);
  }
}
