import { Component, ChangeDetectionStrategy } from '@angular/core';
import { ICellRendererAngularComp } from 'ag-grid-angular';
import { Button } from 'primeng/button';
import { Tooltip } from 'primeng/tooltip';
import { ButtonGroup } from 'primeng/buttongroup';
import { IBedLigne } from '../../data-access/bed.model';

@Component({
  selector: 'app-bed-line-actions',
  imports: [Button, Tooltip, ButtonGroup],
  changeDetection: ChangeDetectionStrategy.Eager,
  template: `
    <p-buttongroup>
      @if (isBrouillon) {
        <p-button
          [text]="true"
          [rounded]="true"
          severity="danger"
          icon="pi pi-trash"
          pTooltip="Supprimer la ligne"
          tooltipPosition="top"
          size="small"
          (onClick)="onDelete($event)"
        />
      } @else {
        <i class="pi pi-lock text-muted" style="font-size:0.8rem;padding:0.4rem"></i>
      }
    </p-buttongroup>
  `,
})
export class BedLineActionsComponent implements ICellRendererAngularComp {
  private params!: any;
  protected isBrouillon = false;

  agInit(params: any): void {
    this.params = params;
    this.isBrouillon = !!params.context?.componentParent?.isBrouillon;
  }

  refresh(): boolean {
    return false;
  }

  private get ligne(): IBedLigne {
    return this.params.data;
  }

  private get parent(): any {
    return this.params.context.componentParent;
  }

  onDelete(event: MouseEvent): void {
    event.stopPropagation();
    this.parent.confirmDeleteLigne(this.ligne);
  }
}
