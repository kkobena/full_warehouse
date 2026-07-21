import {Component, ChangeDetectionStrategy} from '@angular/core';
import {ICellRendererAngularComp} from 'ag-grid-angular';
import {NgbTooltip} from '@ng-bootstrap/ng-bootstrap';
import {ButtonComponent} from '../../../../shared/ui';
import {IOrderLine} from '../../../../shared/model/order-line.model';

@Component({
  selector: 'app-commande-requested-line-actions',
  imports: [ButtonComponent, NgbTooltip],
  changeDetection: ChangeDetectionStrategy.Eager,
  template: `
    <div class="btn-group">
      <app-button
        [text]="true"
        [rounded]="true"
        severity="primary"
        icon="pi pi-pencil"
        ngbTooltip="Modifier"
        placement="top"
        size="small"
        (clicked)="onEdit($event)"
      />

      <app-button
        [text]="true"
        [rounded]="true"
        severity="danger"
        icon="pi pi-trash"
        ngbTooltip="Supprimer"
        placement="top"
        size="small"
        [disabled]="isLocked"
        (clicked)="onDelete($event)"
      />
    </div>
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
