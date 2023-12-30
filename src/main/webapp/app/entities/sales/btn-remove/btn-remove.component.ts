import { Component } from '@angular/core';
import { ICellRendererAngularComp } from 'ag-grid-angular';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';

@Component({
  selector: 'jhi-btn-remove',
  standalone: true,
  imports: [WarehouseCommonModule],
  template: `
    <button type="submit" (click)="btnClickedHandler()" class="btn btn-danger btn-sm">
      <fa-icon icon="times"></fa-icon>
    </button>
  `,
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
