import { Component } from '@angular/core';
import { ICellRendererAngularComp } from 'ag-grid-angular';

@Component({
  selector: 'jhi-commande-btn',
  template: `
    <div class="btn-group text-right pb-sm-1" style="padding: 0;">
      <button
        style="display: none;"
        type="submit"
        (click)="onEditLigneInfo()"
        class="btn btn-sm btn-success "
        icon="pi pi-pencil"
        pButton
        pRipple
        pTooltip="Modifier le produit"
      ></button>

      <button
        *ngIf="showLotBtn"
        type="submit"
        (click)="onAddLot()"
        class="btn btn-sm btn-info"
        icon="pi pi-plus"
        pButton
        pRipple
        pTooltip="Gérer le lot"
      ></button>
      <button
        type="submit"
        [hidden]="true"
        (click)="remove()"
        class="btn btn-sm btn-danger"
        icon="pi pi-times"
        pButton
        pRipple
        pTooltip="Supprimer"
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
  }

  onAddLot(): void {
    this.params.context.componentParent.onAddLot(this.params.data);
  }

  onEditLigneInfo(): void {
    this.params.context.componentParent.editLigneInfos(this.params.data);
  }

  remove(): void {
    this.params.context.componentParent.removeLine(this.params.data);
  }
}
