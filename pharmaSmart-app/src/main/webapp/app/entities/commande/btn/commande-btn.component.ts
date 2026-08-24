import { ChangeDetectionStrategy,Component, signal } from '@angular/core';

import {FontAwesomeModule} from '@fortawesome/angular-fontawesome';
import {ICellRendererAngularComp} from 'ag-grid-angular';
import {NgbTooltipModule} from '@ng-bootstrap/ng-bootstrap';
import {ButtonComponent} from '../../../shared/ui';

@Component({
  imports: [FontAwesomeModule, NgbTooltipModule, ButtonComponent],
  selector: 'app-commande-btn',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="btn-group btn-group-sm" role="group">
      @if (showEditBtn()) {
        <app-button
          [text]="true"
          [rounded]="true"
          (clicked)="onEditLigneInfo()"
          icon="pi pi-pencil"
          class="mr-1"
          severity="success"
          ngbTooltip="Modifier le produit"
        />
      }
      @if (showLotBtn()) {
        <app-button
          [text]="true"
          [rounded]="true"
          (clicked)="onAddLot()"
          severity="info"
          icon="pi pi-plus-circle"
          ngbTooltip="Gérer le lot"
        />
      }
    </div>
  `,
})
export class CommandeBtnComponent implements ICellRendererAngularComp {
  params!: any;
  protected readonly showLotBtn = signal(false);
  protected readonly showEditBtn = signal(false);

  refresh(): boolean {
    return false;
  }

  agInit(params: any): void {
    this.params = params;

    this.showLotBtn.set(this.params.context.componentParent.showLotBtn);
    this.showEditBtn.set(this.params.context.componentParent.showEditBtn);
  }

  onAddLot(): void {
    this.params.context.componentParent.onAddLot(this.params.data);
  }

  onEditLigneInfo(): void {
    this.params.context.componentParent.editLigneInfos(this.params.data);
  }

  remove(): void {
    this.params.context.componentParent.removeLine(this.params.data);
  }
}
