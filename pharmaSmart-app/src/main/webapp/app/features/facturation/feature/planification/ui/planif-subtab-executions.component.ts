import { Component, inject, input, ChangeDetectionStrategy } from '@angular/core';
import { DatePipe } from '@angular/common';
import { TableModule } from 'primeng/table';

import { PlanificationStateService } from '../planification-state.service';

@Component({
  selector: 'app-planif-subtab-executions',
  imports: [DatePipe, TableModule],
  styles: [
    `
      .plan-message-cell {
        max-width: 300px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
    `,
  ],
  changeDetection: ChangeDetectionStrategy.Eager,
  template: `
    @if (state.isLoadingHistorique(planId())) {
      <div class="d-flex justify-content-center p-4">
        <i class="pi pi-spin pi-spinner fs-2 text-muted"></i>
      </div>
    } @else if (state.getHistoriqueForPlan(planId()).length === 0) {
      <div class="text-center text-muted py-5">
        <i class="pi pi-inbox d-block fs-1 mb-2"></i>
        Aucune exécution enregistrée
      </div>
    } @else {
      <p-table
        [value]="state.getHistoriqueForPlan(planId())"
        [scrollable]="true"
        scrollHeight="calc(100vh - 430px)"
        class="p-datatable-sm"
        [stripedRows]="true"
      >
        <ng-template #header>
          <tr class="pharma-table-head">
            <th>Début</th>
            <th>Fin</th>
            <th>Statut</th>
            <th class="text-end">Factures</th>
            <th>Message</th>
          </tr>
        </ng-template>
        <ng-template #body let-h>
          <tr>
            <td class="small">{{ h.executionDebut | date: 'dd/MM/yy HH:mm' }}</td>
            <td class="small">{{ h.executionFin ? (h.executionFin | date: 'dd/MM/yy HH:mm') : '—' }}</td>
            <td><span [class]="state.getStatutBadgeClass(h.statut)">{{ h.statut }}</span></td>
            <td class="text-end">{{ h.nombreFactures ?? 0 }}</td>
            <td class="plan-message-cell small text-muted">{{ h.message || '—' }}</td>
          </tr>
        </ng-template>
      </p-table>
    }
  `,
})
export class PlanifSubtabExecutionsComponent {
  readonly planId = input.required<number>();
  protected readonly state = inject(PlanificationStateService);
}

