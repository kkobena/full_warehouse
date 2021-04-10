import { Component } from '@angular/core';
import { ICellRendererAngularComp } from 'ag-grid-angular';

@Component({
  selector: 'jhi-btn-remove',
  template: `
    <button type="submit" (click)="btnClickedHandler()" class="btn btn-danger btn-sm">
      <fa-icon icon="times"></fa-icon>
    </button>
  `,
  styleUrls: ['./btn-remove.component.scss'],
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
