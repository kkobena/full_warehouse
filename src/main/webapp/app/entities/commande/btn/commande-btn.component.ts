import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { ICellRendererAngularComp } from 'ag-grid-angular';
import { NgbTooltipModule } from '@ng-bootstrap/ng-bootstrap';
import { Button } from 'primeng/button';
import { Tooltip } from 'primeng/tooltip';

@Component({
  imports: [CommonModule, FontAwesomeModule, NgbTooltipModule, Button, Tooltip],
  selector: 'jhi-commande-btn',
  template: `
    <div class="btn-group btn-group-sm" role="group">
      @if (showEditBtn) {
        <p-button
          rounded="true"
          (click)="onEditLigneInfo()"
          icon="pi pi-pencil"
          size="small"
          class="mr-1"
          severity="success"
          pTooltip="Modifier le produit"
        >
        </p-button>
      }
      @if (showLotBtn) {
        <p-button
          rounded="true"
          (click)="onAddLot()"
          size="small"
          severity="info"
          icon="pi pi-plus-circle"
          pTooltip="Gérer le lot"
        ></p-button>
      }
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
