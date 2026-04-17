import { Component, input, output } from '@angular/core';
import { Button } from 'primeng/button';
import { Tooltip } from 'primeng/tooltip';
import { FournisseurSuggestionSummary } from '../../data-access/suggestion-enrichie.model';
import { ButtonGroup } from 'primeng/buttongroup';

@Component({
  selector: 'app-suggestion-fournisseur-actions',
  imports: [Button, Tooltip, ButtonGroup],
  template: `
    <p-buttonGroup>
      @if (fournisseur()?.statut !== 'VALIDEE') {
        <p-button
          [text]="true"
          [rounded]="true"
          severity="primary"
          icon="pi pi-check"
          pTooltip="Valider"
          tooltipPosition="top"
          size="small"
          (onClick)="valider.emit()"
        />
      }
      <p-button
        [text]="true"
        [rounded]="true"
        severity="primary"
        icon="pi pi-shopping-cart"
        pTooltip="Commander"
        tooltipPosition="top"
        size="small"
        (onClick)="commander.emit()"
      />
      <p-button
        [text]="true"
        [rounded]="true"
        severity="secondary"
        icon="pi pi-file-pdf"
        pTooltip="PDF"
        tooltipPosition="top"
        size="small"
        (onClick)="exportPdf.emit()"
      />
      <p-button
        [text]="true"
        [rounded]="true"
        severity="secondary"
        icon="pi pi-file-excel"
        pTooltip="CSV"
        tooltipPosition="top"
        size="small"
        (onClick)="exportCsv.emit()"
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
export class SuggestionFournisseurActionsComponent {
  fournisseur = input<FournisseurSuggestionSummary | null>(null);

  valider = output<void>();
  commander = output<void>();
  exportPdf = output<void>();
  exportCsv = output<void>();
  supprimer = output<void>();
}
