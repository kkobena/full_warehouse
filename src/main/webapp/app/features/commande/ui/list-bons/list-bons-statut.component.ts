import {Component} from '@angular/core';
import {ICellRendererAngularComp} from 'ag-grid-angular';
import {Tag} from 'primeng/tag';

type PrimeSeverity = 'success' | 'info' | 'warn' | 'danger' | 'secondary' | 'contrast';

@Component({
  selector: 'app-list-bons-statut',
  imports: [Tag],
  template: `<p-tag  [value]="label" [severity]="severity" [icon]="icon" [rounded]="true" />`,
  styles: [`:host { display:flex; align-items:center; height:100% }`],
})
export class ListBonsStatutComponent implements ICellRendererAngularComp {
  protected label = '';
  protected severity: PrimeSeverity = 'secondary';
  protected icon = '';

  agInit(params: any): void {
    this.update(params);
  }

  refresh(params: any): boolean {
    this.update(params);
    return true;
  }

  private update(params: any): void {
    const status = params.data?.orderStatus ?? params.data?.statut;
    if (status === 'RECEIVED') {
      this.label = 'En attente de saisie';
      this.severity = 'warn';
      this.icon = 'pi pi-inbox';
    } else {
      this.label = 'Clôturé';
      this.severity = 'success';
      this.icon = 'pi pi-check-circle';
    }
  }
}
