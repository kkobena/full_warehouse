import {Component, ChangeDetectionStrategy, signal } from '@angular/core';
import {ICellRendererAngularComp} from 'ag-grid-angular';
import {AppBadgeSeverity, BadgeComponent} from '../../../../shared/ui';
import {IOrderLine} from '../../../../shared/model/order-line.model';

const SEVERITY_MAP: Record<string, AppBadgeSeverity> = {
  success: 'success',
  danger:  'danger',
  info:    'info',
  warn:    'warn',
};

@Component({
  selector: 'app-commande-received-statut',
  imports: [BadgeComponent],
  template: `<app-badge [label]="label()" [severity]="severity()" [rounded]="true" />`,
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: [`:host { display:flex; align-items:center; justify-content:center; height:100% }`],
})
export class CommandeReceivedStatutComponent implements ICellRendererAngularComp {
  protected readonly label = signal('');
  protected readonly severity = signal<AppBadgeSeverity>('secondary');

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
    this.label.set(s.label);
    this.severity.set(SEVERITY_MAP[s.severity] ?? 'secondary');
  }
}
