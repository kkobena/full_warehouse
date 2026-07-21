import {Component, ChangeDetectionStrategy} from '@angular/core';
import {ICellRendererAngularComp} from 'ag-grid-angular';
import {NgbTooltip} from '@ng-bootstrap/ng-bootstrap';
import {ButtonComponent} from '../../../../shared/ui';
import {IOrderLine} from '../../../../shared/model/order-line.model';

@Component({
  selector: 'app-commande-received-actions',
  imports: [ButtonComponent, NgbTooltip],
  changeDetection: ChangeDetectionStrategy.Eager,
  template: `
    <div class="btn-group">
      <app-button
        [text]="true"
        [rounded]="true"
        severity="secondary"
        icon="pi pi-chart-line"
        ngbTooltip="Historique des prix"
        placement="top"
        size="small"
        (clicked)="onHistorique($event)"
      />
      <app-button
        [text]="true"
        [rounded]="true"
        severity="success"
        icon="pi pi-pencil"
        ngbTooltip="Modifier le produit"
        placement="top"
        size="small"
        (clicked)="onEdit($event)"
      />
      @if (showLot) {
        <app-button
          [text]="true"
          [rounded]="true"
          [hidden]="true"
          severity="info"
          icon="pi pi-box"
          ngbTooltip="Gérer le lot"
          placement="top"
          size="small"
          (clicked)="onLot($event)"
        />
        <app-button
          [text]="true"
          [rounded]="true"
          [hidden]="true"
          severity="danger"
          icon="pi pi-trash"
          ngbTooltip="Supprimer"
          placement="top"
          size="small"
          (clicked)="onDelete($event)"
        />
      }
    </div>
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
