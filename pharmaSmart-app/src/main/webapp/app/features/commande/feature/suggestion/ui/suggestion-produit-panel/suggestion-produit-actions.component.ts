import {Component, ChangeDetectionStrategy} from '@angular/core';
import {ICellRendererAngularComp} from 'ag-grid-angular';
import {Button} from 'primeng/button';
import {Tooltip} from 'primeng/tooltip';
import {ButtonGroup} from 'primeng/buttongroup';
import {SuggestionLigneEnrichie} from '../../data-access/suggestion-enrichie.model';

@Component({
  selector: 'app-suggestion-produit-actions',
  imports: [Button, Tooltip, ButtonGroup],
  changeDetection: ChangeDetectionStrategy.Eager,
  template: `
    <p-buttongroup>
      @if (showReset) {
        <p-button
          [text]="true"
          [rounded]="true"
          severity="warn"
          icon="pi pi-lock-open"
          pTooltip="Déverrouiller — laisser SEMOIS recalculer la quantité"
          tooltipPosition="top"
          size="small"
          (onClick)="onReset($event)"
        />
      }
      @if (showCompare) {
        <p-button
          [text]="true"
          [rounded]="true"
          severity="info"
          icon="pi pi-search"
          pTooltip="Comparer les fournisseurs"
          tooltipPosition="top"
          size="small"
          (onClick)="onCompare($event)"
        />
      }
      <p-button
        [text]="true"
        [rounded]="true"
        severity="danger"
        icon="pi pi-trash"
        pTooltip="Retirer de la suggestion"
        tooltipPosition="top"
        size="small"
        (onClick)="onDelete($event)"
      />
    </p-buttongroup>
  `,
})
export class SuggestionProduitActionsComponent implements ICellRendererAngularComp {
  private params!: any;
  protected showReset = false;
  protected showCompare = false;

  agInit(params: any): void {
    this.params = params;
    const ligne: SuggestionLigneEnrichie = params.data;
    this.showReset = !!ligne?.quantiteModifieeManuel;
    this.showCompare = !!ligne?.produitId;
  }

  refresh(): boolean {
    return false;
  }

  private get ligne(): SuggestionLigneEnrichie {
    return this.params.data;
  }

  private get parent(): any {
    return this.params.context.componentParent;
  }

  onReset(event: MouseEvent): void {
    event.stopPropagation();
    this.parent.onResetLigne(this.ligne);
    this.params.node.setDataValue('quantiteModifieeManuel', false);
    this.params.node.setDataValue('quantiteModifiee', false);
  }

  onCompare(event: MouseEvent): void {
    event.stopPropagation();
    this.parent.onComparerLigne(this.ligne);
  }

  onDelete(event: MouseEvent): void {
    event.stopPropagation();
    this.parent.onSupprimerLigne(this.ligne);
  }
}
