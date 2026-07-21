import { Component, ChangeDetectionStrategy } from '@angular/core';
import { ICellRendererAngularComp } from 'ag-grid-angular';
import { NgbTooltip } from '@ng-bootstrap/ng-bootstrap';
import { ButtonComponent } from '../../../../../../shared/ui';
import { IBedLigne } from '../../data-access/bed.model';

@Component({
  selector: 'app-bed-line-actions',
  imports: [ButtonComponent, NgbTooltip],
  changeDetection: ChangeDetectionStrategy.Eager,
  template: `
    @if (isBrouillon) {
      <app-button
        [text]="true"
        [rounded]="true"
        severity="danger"
        icon="pi pi-trash"
        ngbTooltip="Supprimer la ligne"
        placement="top"
        size="small"
        (clicked)="onDelete($event)"
      />
    } @else {
      <i class="pi pi-lock text-muted" style="font-size:0.8rem;padding:0.4rem"></i>
    }
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
