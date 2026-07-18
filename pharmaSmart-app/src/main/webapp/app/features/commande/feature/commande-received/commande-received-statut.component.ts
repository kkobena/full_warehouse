import {Component, ChangeDetectionStrategy} from '@angular/core';
import {ICellRendererAngularComp} from 'ag-grid-angular';
import {Tag} from 'primeng/tag';
import {IOrderLine} from '../../../../shared/model/order-line.model';

type PrimeSeverity = 'success' | 'info' | 'warn' | 'danger' | 'secondary' | 'contrast';

const SEVERITY_MAP: Record<string, PrimeSeverity> = {
  success: 'success',
  danger:  'danger',
  info:    'info',
  warn:    'warn',
};

@Component({
  selector: 'app-commande-received-statut',
  imports: [Tag],
  template: `<p-tag [value]="label" [severity]="severity" [rounded]="true" />`,
  changeDetection: ChangeDetectionStrategy.Eager,
  styles: [`:host { display:flex; align-items:center; justify-content:center; height:100% }`],
})
export class CommandeReceivedStatutComponent implements ICellRendererAngularComp {
  protected label = '';
  protected severity: PrimeSeverity = 'secondary';

  agInit(params: any): void {
    this.update(params);
  }

  refresh(params: any): boolean {
    this.update(params);
    return true;
  }

  private update(params: any): void {
    const line: IOrderLine = params.data;
    if (!line) return;
    const s: {label: string; severity: string} = params.context.componentParent.lineStatut(line);
    this.label = s.label;
    this.severity = SEVERITY_MAP[s.severity] ?? 'secondary';
  }
}
