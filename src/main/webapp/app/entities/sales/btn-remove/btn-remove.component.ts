import { Component } from '@angular/core';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { ICellRendererAngularComp } from 'ag-grid-angular';

@Component({
    selector: 'jhi-btn-remove',
    imports: [WarehouseCommonModule],
    template: `
    <button type="submit" (click)="btnClickedHandler()" class="btn btn-danger btn-sm">
      <fa-icon icon="times"></fa-icon>
    </button>
  `
})
export class BtnRemoveComponent implements ICellRendererAngularComp {
  params!: any;

  constructor() {}

  refresh(): boolean {
    return false;
  }

  agInit(params: any): void {
    this.params = params;
  }

  btnClickedHandler(): void {
    this.params.context.componentParent.removeLine(this.params.data);
  }
}
