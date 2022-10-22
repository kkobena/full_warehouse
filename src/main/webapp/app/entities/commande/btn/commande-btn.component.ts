import { Component } from '@angular/core';
import { ICellRendererAngularComp } from 'ag-grid-angular';

@Component({
  selector: 'jhi-commande-btn',
  template: `
    <div class="btn-group text-right pb-sm-1">
      <button
        type="submit"
        (click)="onEditLigneInfo()"
        class="p-button-rounded p-button-success p-button-sm"
        icon="pi pi-pencil"
        pButton
        pRipple
        pTooltip="Modifier la ligne"
      ></button>

      <button
        *ngIf="showLotBtn"
        type="submit"
        (click)="onAddLot()"
        class="p-button-rounded p-button-help p-button-sm"
        icon="pi pi-plus"
        pButton
        pRipple
        pTooltip="Gérer le lot"
      ></button>
    </div>
  `,
})
export class CommandeBtnComponent implements ICellRendererAngularComp {
  params!: any;
  showLotBtn = false;

  constructor() {}

  refresh(): boolean {
    return false;
  }

  agInit(params: any): void {
    this.params = params;

    this.showLotBtn = this.params.context.componentParent.showLotBtn;
    console.error(this.showLotBtn, 'showLotBtn========>>');
  }

  onAddLot(): void {
    this.params.context.componentParent.onAddLot(this.params.data);
  }

  onEditLigneInfo(): void {
    this.params.context.componentParent.editLigneInfos(this.params.data);
  }
}
