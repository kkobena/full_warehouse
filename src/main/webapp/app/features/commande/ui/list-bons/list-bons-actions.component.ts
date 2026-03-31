import {Component} from '@angular/core';
import {ICellRendererAngularComp} from 'ag-grid-angular';
import {Button} from 'primeng/button';
import {Tooltip} from 'primeng/tooltip';
import {ButtonGroup} from 'primeng/buttongroup';
import {IDelivery} from '../../../../shared/model/delevery.model';

@Component({
  selector: 'app-list-bons-actions',
  imports: [Button, Tooltip, ButtonGroup],
  template: `
    <p-buttonGroup>
      @if (isReceived) {
        <p-button
          [text]="true"
          [rounded]="true"
          severity="warn"
          icon="pi pi-inbox"
          pTooltip="Saisir la réception"
          tooltipPosition="top"
          size="small"
          (onClick)="onReceive($event)"
        />
      }
      <p-button
        [text]="true"
        [rounded]="true"
        severity="secondary"
        icon="pi pi-file-pdf"
        pTooltip="Imprimer BL"
        tooltipPosition="top"
        size="small"
        (onClick)="onPdf($event)"
      />
      @if (!isReceived) {
        <p-button
          [text]="true"
          [rounded]="true"
          severity="secondary"
          icon="pi pi-print"
          pTooltip="Étiquettes"
          tooltipPosition="top"
          size="small"
          (onClick)="onEtiquette($event)"
        />
      }
    </p-buttonGroup>
  `,
})
export class ListBonsActionsComponent implements ICellRendererAngularComp {
  private params!: any;
  protected isReceived = false;

  agInit(params: any): void {
    this.params = params;
    const status = params.data?.orderStatus ?? params.data?.statut;
    this.isReceived = status === 'RECEIVED';
  }

  refresh(): boolean {
    return false;
  }

  private get delivery(): IDelivery {
    return this.params.data;
  }

  private get parent(): any {
    return this.params.context.componentParent;
  }

  onReceive(event: MouseEvent): void {
    event.stopPropagation();
    this.parent.onEditerReceivedDelivery(this.delivery);
  }

  onPdf(event: MouseEvent): void {
    event.stopPropagation();
    this.parent.exportPdf(this.delivery, event);
  }

  onEtiquette(event: MouseEvent): void {
    event.stopPropagation();
    this.parent.printEtiquette(this.delivery, event);
  }
}
