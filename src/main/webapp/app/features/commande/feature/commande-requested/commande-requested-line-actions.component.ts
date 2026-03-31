import {Component} from '@angular/core';
import {ICellRendererAngularComp} from 'ag-grid-angular';
import {Button} from 'primeng/button';
import {Tooltip} from 'primeng/tooltip';
import {IOrderLine} from '../../../../shared/model/order-line.model';
import { ButtonGroup } from "primeng/buttongroup";

@Component({
  selector: 'app-commande-requested-line-actions',
  imports: [Button, Tooltip, ButtonGroup],
  template: `
    <p-buttonGroup>
      <p-button
        [text]="true"
        [rounded]="true"
        severity="primary"
        icon="pi pi-pencil"
        pTooltip="Modifier"
        tooltipPosition="top"
        size="small"
        (onClick)="onEdit($event)"
      />
      @if (showLot) {
        <p-button
          [text]="true"
          [rounded]="true"
          severity="info"
          icon="pi pi-box"
          pTooltip="Lots"
          tooltipPosition="top"
          size="small"
          (onClick)="onLot($event)"
        />
      }
      <p-button
        [text]="true"
        [rounded]="true"
        severity="danger"
        icon="pi pi-trash"
        pTooltip="Supprimer"
        tooltipPosition="top"
        size="small"
        [disabled]="isLocked"
        (onClick)="onDelete($event)"
      />
    </p-buttonGroup>
  `,
})
export class CommandeRequestedLineActionsComponent implements ICellRendererAngularComp {
  private params!: any;
  protected showLot = false;
  protected isLocked = false;

  agInit(params: any): void {
    this.params = params;
    const line: IOrderLine = params.data;
    const parent = params.context.componentParent;
    this.showLot = (line?.lots?.length ?? 0) > 0 || !!parent.showLotBtn;
    this.isLocked = !!parent.isLocked;
  }

  refresh(): boolean {
    return false;
  }

  private get line(): IOrderLine {
    return this.params.data;
  }

  private get parent(): any {
    return this.params.context.componentParent;
  }

  onEdit(event: MouseEvent): void {
    event.stopPropagation();
    this.parent.editLigneInfos(this.line);
  }

  onLot(event: MouseEvent): void {
    event.stopPropagation();
    this.parent.onAddLot(this.line);
  }

  onDelete(event: MouseEvent): void {
    event.stopPropagation();
    this.parent.confirmDeleteItem(this.line);
  }
}
