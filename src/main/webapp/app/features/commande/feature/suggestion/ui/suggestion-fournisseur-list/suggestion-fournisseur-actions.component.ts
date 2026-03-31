import {Component} from '@angular/core';
import {ICellRendererAngularComp} from 'ag-grid-angular';
import {Button} from 'primeng/button';
import {Tooltip} from 'primeng/tooltip';
import {FournisseurSuggestionSummary} from '../../data-access/suggestion-enrichie.model';
import { ButtonGroup } from "primeng/buttongroup";

@Component({
  selector: 'app-suggestion-fournisseur-actions',
  imports: [Button, Tooltip, ButtonGroup],
  template: `
    <p-buttonGroup>
      @if (showValider) {
        <p-button
          [text]="true"
          [rounded]="true"
          severity="primary"
          icon="pi pi-check"
          pTooltip="Valider"
          tooltipPosition="top"
          size="small"
          (onClick)="onValider($event)"
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
        (onClick)="onCommander($event)"
      />
      <p-button
        [text]="true"
        [rounded]="true"
        severity="secondary"
        icon="pi pi-file-pdf"
        pTooltip="PDF"
        tooltipPosition="top"
        size="small"
        (onClick)="onPdf($event)"
      />
      <p-button
        [text]="true"
        [rounded]="true"
        severity="secondary"
        icon="pi pi-file-excel"
        pTooltip="CSV"
        tooltipPosition="top"
        size="small"
        (onClick)="onCsv($event)"
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
export class SuggestionFournisseurActionsComponent implements ICellRendererAngularComp {
  private params!: any;
  protected showValider = false;

  agInit(params: any): void {
    this.params = params;
    const data: FournisseurSuggestionSummary = params.data;
    this.showValider = data?.statut !== 'VALIDEE';
  }

  refresh(): boolean {
    return false;
  }

  private get data(): FournisseurSuggestionSummary {
    return this.params.data;
  }

  private get parent(): any {
    return this.params.context.componentParent;
  }

  onValider(event: MouseEvent): void {
    event.stopPropagation();
    this.parent.onValider(this.data);
  }

  onCommander(event: MouseEvent): void {
    event.stopPropagation();
    this.parent.onCommander(this.data);
  }

  onPdf(event: MouseEvent): void {
    event.stopPropagation();
    this.parent.onExportPdf(this.data);
  }

  onCsv(event: MouseEvent): void {
    event.stopPropagation();
    this.parent.onExportCsv(this.data);
  }

  onDelete(event: MouseEvent): void {
    event.stopPropagation();
    this.parent.onSupprimer(this.data);
  }
}
