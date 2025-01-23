import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { ICellRendererAngularComp } from 'ag-grid-angular';
import { NgbTooltipModule } from '@ng-bootstrap/ng-bootstrap';

@Component({
    imports: [CommonModule, FontAwesomeModule, NgbTooltipModule],
    selector: 'jhi-commande-btn',
    template: `
    <div class="btn-group btn-group-sm" role="group">
      @if (showEditBtn) {
        <button (click)="onEditLigneInfo()" class="btn-sm btn btn-success" [ngbTooltip]="showEditBtnTpl">
          <i class="pi pi-pencil"></i>
        </button>
      }
      @if (showLotBtn) {
        <button (click)="onAddLot()" class="btn-sm btn btn-primary" placement="top" [ngbTooltip]="showLotBtnTpl">
          <i class="pi pi-plus-circle"></i>
        </button>
      }
    </div>
  `
})
export class CommandeBtnComponent implements ICellRendererAngularComp {
  params!: any;
  showLotBtn = false;
  showEditBtn = false;
  showEditBtnTpl = 'Modifier le produit';
  showLotBtnTpl = 'Gérer le lot';

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
