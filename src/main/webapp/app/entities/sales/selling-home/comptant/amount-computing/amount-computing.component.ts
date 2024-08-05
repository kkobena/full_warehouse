import { Component, inject, Input } from '@angular/core';
import { WarehouseCommonModule } from '../../../../../shared/warehouse-common/warehouse-common.module';
import { CurrentSaleService } from '../../../service/current-sale.service';
import { LastCurrencyGivenService } from '../../../service/last-currency-given.service';

@Component({
  selector: 'jhi-amount-computing',
  standalone: true,
  imports: [WarehouseCommonModule],
  templateUrl: './amount-computing.component.html',
})
export class AmountComputingComponent {
  @Input() entryAmount: number = 0;
  monnaie = 0;
  /*
  derniere monnaie
   */

  currentSaleService = inject(CurrentSaleService);
  lastCurrencyGivenService = inject(LastCurrencyGivenService);

  constructor() {}

  computeMonnaie(amount: number | null): void {
    const thatentryAmount = amount || this.entryAmount;
    const thatMonnaie = thatentryAmount - this.currentSaleService.currentSale()?.amountToBePaid;
    this.monnaie = thatMonnaie > 0 ? thatMonnaie : 0;
  }
}
