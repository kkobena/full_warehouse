import { Component, input, output } from '@angular/core';
import { Button } from 'primeng/button';
import { Tooltip } from 'primeng/tooltip';
import { ICommande } from 'app/shared/model/commande.model';
import { ButtonGroup } from 'primeng/buttongroup';

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
        (onClick)="receptionner.emit()"
      />
      <p-button
        [text]="true"
        [rounded]="true"
        severity="secondary"
        icon="pi pi-file-excel"
        pTooltip="Export CSV"
        tooltipPosition="top"
        size="small"
        (onClick)="exportCsv.emit()"
      />
      <p-button
        [text]="true"
        [rounded]="true"
        severity="secondary"
        icon="pi pi-print"
        pTooltip="Imprimer PDF"
        tooltipPosition="top"
        size="small"
        (onClick)="exportPdf.emit()"
      />
      <p-button
        [text]="true"
        [rounded]="true"
        severity="danger"
        icon="pi pi-trash"
        pTooltip="Supprimer"
        tooltipPosition="top"
        size="small"
        (onClick)="supprimer.emit()"
      />
    </p-buttonGroup>
  `,
})
export class CommandeRequestedActionsComponent {
  commande = input<ICommande | null>(null);

  receptionner = output<void>();
  exportCsv = output<void>();
  exportPdf = output<void>();
  supprimer = output<void>();
}
