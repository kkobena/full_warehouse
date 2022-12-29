import {Component} from '@angular/core';
import {NgbActiveModal} from '@ng-bootstrap/ng-bootstrap';


import {IOrderLine} from 'app/shared/model/order-line.model';
import {OrderLineService} from './order-line.service';

@Component({
  templateUrl: './order-line-delete-dialog.component.html',
})
export class OrderLineDeleteDialogComponent {
  orderLine?: IOrderLine;

  constructor(protected orderLineService: OrderLineService, public activeModal: NgbActiveModal) {
  }

  cancel(): void {
    this.activeModal.dismiss();
  }

  confirmDelete(id: number): void {
    this.orderLineService.delete(id).subscribe(() => {

      this.activeModal.close();
    });
  }
}
