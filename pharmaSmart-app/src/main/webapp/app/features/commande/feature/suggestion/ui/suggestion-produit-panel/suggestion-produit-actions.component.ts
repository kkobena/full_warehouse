import {Component, ChangeDetectionStrategy} from '@angular/core';
import {ICellRendererAngularComp} from 'ag-grid-angular';
import {NgbTooltip} from '@ng-bootstrap/ng-bootstrap';
import {ButtonComponent} from 'app/shared/ui';
import {SuggestionLigneEnrichie} from '../../data-access/suggestion-enrichie.model';

@Component({
  selector: 'app-suggestion-produit-actions',
  imports: [ButtonComponent, NgbTooltip],
  changeDetection: ChangeDetectionStrategy.Eager,
  template: `
    <div class="d-flex gap-1">
      @if (showReset) {
        <app-button
          [text]="true"
          [rounded]="true"
          severity="warn"
          icon="pi pi-lock-open"
          ngbTooltip="Déverrouiller — laisser SEMOIS recalculer la quantité"
          placement="top"
          size="small"
          (clicked)="onReset($event)"
        />
      }
      @if (showCompare) {
        <app-button
          [text]="true"
          [rounded]="true"
          severity="info"
          icon="pi pi-search"
          ngbTooltip="Comparer les fournisseurs"
          placement="top"
          size="small"
          (clicked)="onCompare($event)"
        />
      }
      <app-button
        [text]="true"
        [rounded]="true"
        severity="danger"
        icon="pi pi-trash"
        ngbTooltip="Retirer de la suggestion"
        placement="top"
        size="small"
        (clicked)="onDelete($event)"
      />
    </div>
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
