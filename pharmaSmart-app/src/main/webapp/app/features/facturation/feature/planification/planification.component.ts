import { Component, inject, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { NgbNavModule } from '@ng-bootstrap/ng-bootstrap';
import { ButtonModule } from 'primeng/button';
import { Toolbar } from 'primeng/toolbar';
import { Toast } from 'primeng/toast';

import { PlanificationStateService } from './planification-state.service';
import { PlanifTabFacturesComponent, PlanifTabFneComponent } from './ui';

@Component({
  selector: 'app-planification',
  providers: [PlanificationStateService],
  imports: [Toolbar, ButtonModule, Toast, NgbNavModule, PlanifTabFacturesComponent, PlanifTabFneComponent],
  templateUrl: './planification.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './planification.component.scss',
})
export class PlanificationComponent implements OnInit {
  protected readonly state = inject(PlanificationStateService);
  protected activeMainTab = 'def';

  ngOnInit(): void {
    this.state.load();
  }

  protected onMainTabChange(tab: string): void {
    this.activeMainTab = tab;
    this.state.onMainTabChange(tab);
  }
}
