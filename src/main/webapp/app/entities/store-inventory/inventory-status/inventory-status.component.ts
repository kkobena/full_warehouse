import { Component, OnChanges, SimpleChanges } from '@angular/core';

import { IStoreInventoryLine } from '../../../shared/model/store-inventory-line.model';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { ICellRendererAngularComp } from 'ag-grid-angular';

@Component({
  selector: 'jhi-inventory-status',
  templateUrl: './inventory-status.component.html',
  standalone: true,
  imports: [WarehouseCommonModule],
})
export class InventoryStatusComponent implements ICellRendererAngularComp, OnChanges {
  params!: any;

  refresh(): boolean {
    return false;
  }

  agInit(params: any): void {
    this.params = params;
  }

  getStatus(): string {
    const data = this.params.data as IStoreInventoryLine;
    if (data.updated) {
      return 'badge text-bg-success';
    }
    return 'badge text-bg-secondary';
  }

  getText(): string {
    const data = this.params.data as IStoreInventoryLine;
    if (data.updated) {
      return 'Modifié';
    }
    return 'Non modifié';
  }

  ngOnChanges(changes: SimpleChanges): void {
    const status = changes.status;
    if (status) {
      console.log(status);
    }
  }
}
