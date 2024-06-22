import { Component } from '@angular/core';
import { ICellRendererAngularComp } from '@ag-grid-community/angular';
import { CommonModule } from '@angular/common';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';

@Component({
  standalone: true,
  imports: [CommonModule, FontAwesomeModule, ButtonModule, TooltipModule],
  selector: 'jhi-commande-btn',
  template: `
    <div class="btn-group btn-group-sm" role="group">
      <button
        *ngIf="showEditBtn"
        (click)="onEditLigneInfo()"
        class="p-button-success p-ripple p-button-rounded"
        icon="pi pi-pencil"
        pButton
        pTooltip="Modifier le produit"
      ></button>

      <button
        *ngIf="showLotBtn"
        (click)="onAddLot()"
        class="p-button-info p-ripple p-button-rounded"
        icon="pi pi-plus"
        pButton
        pTooltip="Gérer le lot"
      ></button>
    </div>
  `,
})
export class CommandeBtnComponent implements ICellRendererAngularComp {
  params!: any;
  showLotBtn = false;
  showEditBtn = false;

  refresh(): boolean {
    return false;
  }

  agInit(params: any): void {
    this.params = params;

    this.showLotBtn = this.params.context.componentParent.showLotBtn;
    this.showEditBtn = this.params.context.componentParent.showEditBtn;
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
