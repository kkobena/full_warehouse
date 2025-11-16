import { Component, input, OnChanges, SimpleChanges } from '@angular/core';

import { IDeliveryItem } from '../../../shared/model/delivery-item';
import { CommonModule } from '@angular/common';
import { ICellRendererAngularComp } from 'ag-grid-angular';

@Component({
  imports: [CommonModule],
  selector: 'jhi-receipt-status-btn',
  template: `
    <div class="progress" style="width: 100%;text-align:center;height: 8px;">
      <div [ngClass]="getStatus()" role="progressbar" style="width:100%" aria-valuenow="100" aria-valuemin="0" aria-valuemax="100"></div>
    </div>
  `,
})
export class ReceiptStatusComponent implements ICellRendererAngularComp, OnChanges {
  params!: any;
  readonly status = input<boolean>(false);

  refresh(): boolean {
    return false;
  }

  agInit(params: any): void {
    this.params = params;
  }

  getStatus(): string {
    const receiptItem = this.params.data as IDeliveryItem;
    const mustBeUpdate =
      receiptItem.orderUnitPrice !== receiptItem.regularUnitPrice ||
      receiptItem.produitCip.length === 0 ||
      receiptItem.orderCostAmount !== receiptItem.costAmount ||
      (receiptItem.updated && receiptItem.quantityReceived !== receiptItem.quantityRequested);

    if (mustBeUpdate) {
      return 'progress-bar bg-warning';
    } else if (receiptItem.updated) {
      return 'progress-bar bg-success';
    } else {
      return 'progress-bar bg-secondary';
    }
  }

  ngOnChanges(changes: SimpleChanges): void {}
}
