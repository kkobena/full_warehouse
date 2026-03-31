import {Component} from '@angular/core';
import {ICellRendererAngularComp} from 'ag-grid-angular';
import {Button} from 'primeng/button';
import {Tooltip} from 'primeng/tooltip';
import {ButtonGroup} from 'primeng/buttongroup';
import {IOrderLine} from '../../../../shared/model/order-line.model';

@Component({
  selector: 'app-commande-received-actions',
  imports: [Button, Tooltip, ButtonGroup],
  template: `
    <p-buttonGroup>
      <p-button
        [text]="true"
        [rounded]="true"
        severity="secondary"
        icon="pi pi-chart-line"
        pTooltip="Historique des prix"
        tooltipPosition="top"
        size="small"
        (onClick)="onHistorique($event)"
      />
      <p-button
        [text]="true"
        [rounded]="true"
        severity="success"
        icon="pi pi-pencil"
        pTooltip="Modifier le produit"
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
          pTooltip="Gérer le lot"
          tooltipPosition="top"
          size="small"
          (onClick)="onLot($event)"
        />
        <p-button
          [text]="true"
          [rounded]="true"
          severity="danger"
          icon="pi pi-trash"
          pTooltip="Supprimer"
          tooltipPosition="top"
          size="small"
          (onClick)="onDelete($event)"
        />
      }
    </p-buttonGroup>
  `,
})
export class CommandeReceivedActionsComponent implements ICellRendererAngularComp {
  private params!: any;
  protected showLot = false;

  agInit(params: any): void {
    this.params = params;
    const line: IOrderLine = params.data;
    this.showLot = (line?.lots?.length ?? 0) > 0 || !!params.context.componentParent.showLotBtn;
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

  onHistorique(event: MouseEvent): void {
    event.stopPropagation();
    this.parent.onShowPriceHistory(this.line);
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
