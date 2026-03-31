import {Component} from '@angular/core';
import {ICellRendererAngularComp} from 'ag-grid-angular';
import {Button} from 'primeng/button';
import {Tooltip} from 'primeng/tooltip';
import {ICommande} from 'app/shared/model/commande.model';
import { ButtonGroup } from "primeng/buttongroup";

@Component({
  selector: 'app-commande-requested-actions',
  imports: [Button, Tooltip, ButtonGroup],
  template: `
    <p-buttonGroup>
      <p-button
        [text]="true"
        [rounded]="true"
        severity="primary"
        icon="pi pi-inbox"
        pTooltip="Réceptionner"
        tooltipPosition="top"
        size="small"
        (onClick)="onReceive($event)"
      />
      <p-button
        [text]="true"
        [rounded]="true"
        severity="secondary"
        icon="pi pi-file-excel"
        pTooltip="Export CSV"
        tooltipPosition="top"
        size="small"
        (onClick)="onCsv($event)"
      />
      <p-button
        [text]="true"
        [rounded]="true"
        severity="secondary"
        icon="pi pi-print"
        pTooltip="Imprimer PDF"
        tooltipPosition="top"
        size="small"
        (onClick)="onPdf($event)"
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
    </p-buttonGroup>
  `,
})
export class CommandeRequestedActionsComponent implements ICellRendererAngularComp {
  private params!: any;

  agInit(params: any): void {
    this.params = params;
  }

  refresh(): boolean {
    return false;
  }

  private get commande(): ICommande {
    return this.params.data;
  }

  private get parent(): any {
    return this.params.context.componentParent;
  }

  onReceive(event: MouseEvent): void {
    event.stopPropagation();
    this.parent.onReceptionner(this.commande, event);
  }

  onCsv(event: MouseEvent): void {
    event.stopPropagation();
    this.parent.exportCsv(this.commande, event);
  }

  onPdf(event: MouseEvent): void {
    event.stopPropagation();
    this.parent.exportPdf(this.commande, event);
  }

  onDelete(event: MouseEvent): void {
    event.stopPropagation();
    this.parent.onSupprimerCommande(this.commande, event);
  }
}
