import { Component, computed, input, output } from '@angular/core';
import { Button } from 'primeng/button';
import { Tooltip } from 'primeng/tooltip';
import { ButtonGroup } from 'primeng/buttongroup';
import { IDelivery } from '../../../../shared/model/delevery.model';

@Component({
  selector: 'app-list-bons-actions',
  imports: [Button, Tooltip, ButtonGroup],
  template: `
    <p-buttonGroup>
      @if (isReceived()) {
        <p-button
          [text]="true"
          [rounded]="true"
          severity="warn"
          icon="pi pi-inbox"
          pTooltip="Saisir la réception"
          tooltipPosition="top"
          size="small"
          (onClick)="receive.emit()"
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
        (onClick)="exportPdf.emit()"
      />
      @if (!isReceived()) {
        <p-button
          [text]="true"
          [rounded]="true"
          severity="secondary"
          icon="pi pi-print"
          pTooltip="Étiquettes"
          tooltipPosition="top"
          size="small"
          (onClick)="printEtiquette.emit()"
        />
        <p-button
          [text]="true"
          [rounded]="true"
          severity="danger"
          icon="pi pi-replay"
          pTooltip="Retour complet de ce bon"
          tooltipPosition="top"
          size="small"
          (onClick)="retourComplet.emit()"
        />
        <p-button
          [text]="true"
          [rounded]="true"
          severity="warn"
          icon="pi pi-list-check"
          pTooltip="Retour par ligne"
          tooltipPosition="top"
          size="small"
          (onClick)="retourParLigne.emit()"
        />
      }
    </p-buttonGroup>
  `,
})
export class ListBonsActionsComponent {
  delivery = input<IDelivery | null>(null);

  readonly isReceived = computed(() => {
    const d = this.delivery();
    return d?.orderStatus === 'RECEIVED' || (d as any)?.statut === 'RECEIVED';
  });

  receive = output<void>();
  exportPdf = output<void>();
  printEtiquette = output<void>();
  retourComplet = output<void>();
  retourParLigne = output<void>();
}
