import { Component, inject } from '@angular/core';
import { ILot } from '../../../shared/model/lot.model';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { Button } from 'primeng/button';

@Component({
  selector: 'jhi-order-line-lots',
  imports: [WarehouseCommonModule, Button],
  templateUrl: './order-line-lots.component.html'
})
export class OrderLineLotsComponent {
  protected lots: ILot[] = [];
  protected produitLibelle: string | null = null;
  private readonly activeModal = inject(NgbActiveModal);

  cancel(): void {
    this.activeModal.dismiss();
  }
}
