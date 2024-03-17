import { Component, EventEmitter, Input, Output } from '@angular/core';
import { ISales } from '../../../../../shared/model/sales.model';
import { WarehouseCommonModule } from '../../../../../shared/warehouse-common/warehouse-common.module';
import { IPaymentMode } from '../../../../../shared/model/payment-mode.model';

@Component({
  selector: 'jhi-amount-computing',
  standalone: true,
  imports: [WarehouseCommonModule],
  templateUrl: './amount-computing.component.html',
  styleUrl: './amount-computing.component.scss',
})
export class AmountComputingComponent {
  @Input('sale') sale: ISales;
  @Input('derniereMonnaie') derniereMonnaie: number = 0;
  @Input() entryAmount: number = 0;
  monnaie = 0;
  /*
  derniere monnaie
   */
  @Output() lastCurrencyGiven = new EventEmitter<number>();
  @Input() modeReglementSelected: IPaymentMode[] = [];

  computeMonnaie(amount: number | null): void {
    const thatentryAmount = amount || this.entryAmount;
    const thatMonnaie = thatentryAmount - this.sale?.amountToBePaid;
    this.monnaie = thatMonnaie > 0 ? thatMonnaie : 0;
  }
}
