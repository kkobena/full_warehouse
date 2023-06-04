import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { ICellRendererAngularComp } from 'ag-grid-angular';
import { IDeliveryItem } from '../../../shared/model/delivery-item';

@Component({
  selector: 'jhi-receipt-status-btn',
  template: `
    <div class="progress" style="width: 100%;text-align:center;">
      <div [ngClass]="getStatus()" role="progressbar" style="width:100%" aria-valuenow="100" aria-valuemin="0" aria-valuemax="100"></div>
    </div>
  `,
})
export class ReceiptStatusComponent implements ICellRendererAngularComp, OnChanges {
  params!: any;
  @Input() status: boolean = false;

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
      receiptItem.fournisseurProduitCip?.length === 0 ||
      receiptItem.orderCostAmount !== receiptItem.costAmount;
    if (mustBeUpdate) {
      return 'progress-bar bg-warning';
    } else if (receiptItem.updated || !mustBeUpdate) return 'progress-bar bg-success';
    return 'progress-bar bg-secondary';
  }

  ngOnChanges(changes: SimpleChanges): void {
    const status = changes.status;
    if (status) {
      console.log(status);
    }
  }
}
