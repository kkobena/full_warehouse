import { Component, computed, inject, input } from '@angular/core';
import { NgbNavModule } from '@ng-bootstrap/ng-bootstrap';
import { ButtonModule } from 'primeng/button';
import { Tooltip } from 'primeng/tooltip';

import { IPlanification } from '../../../data-access/models';
import { PlanificationStateService } from '../planification-state.service';
import { PlanifSubtabExecutionsComponent } from './planif-subtab-executions.component';
import { PlanifSubtabGroupesComponent } from './planif-subtab-groupes.component';
import { PlanifSubtabTpsComponent } from './planif-subtab-tps.component';

@Component({
  selector: 'app-planif-detail-panel',
  imports: [
    NgbNavModule,
    ButtonModule,
    Tooltip,
    PlanifSubtabExecutionsComponent,
    PlanifSubtabGroupesComponent,
    PlanifSubtabTpsComponent,
  ],
  templateUrl: './planif-detail-panel.component.html',
  styleUrl: './planif-detail-panel.component.scss',
})
export class PlanifDetailPanelComponent {
  readonly plan = input.required<IPlanification>();
  readonly mode = input.required<'def' | 'prov'>();

  protected readonly state = inject(PlanificationStateService);

  protected readonly activeSubTab = computed(() =>
    this.mode() === 'def' ? this.state.activeSubTabDef() : this.state.activeSubTabProv(),
  );

  protected readonly badgeClass = computed(() =>
    this.mode() === 'def' ? 'badge bg-info text-dark' : 'badge bg-warning text-dark',
  );

  protected readonly iconClass = computed(() =>
    this.mode() === 'def' ? 'pi pi-calendar text-primary' : 'pi pi-calendar text-warning',
  );

  protected onSubTabChange(tab: string): void {
    if (this.mode() === 'def') this.state.onSubTabChangeDef(tab);
    else this.state.onSubTabChangeProv(tab);
  }

  protected onClose(): void {
    if (this.mode() === 'def') this.state.onSelectPlanDef(null);
    else this.state.onSelectPlanProv(null);
  }
}

