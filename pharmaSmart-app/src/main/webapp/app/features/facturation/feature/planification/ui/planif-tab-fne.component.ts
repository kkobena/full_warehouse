import { Component, inject, ChangeDetectionStrategy } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NgbTooltip } from '@ng-bootstrap/ng-bootstrap';
import { ButtonComponent, DataTableComponent, SwitchComponent } from '../../../../../shared/ui';

import { PlanificationStateService } from '../planification-state.service';

@Component({
  selector: 'app-planif-tab-fne',
  imports: [DatePipe, FormsModule, NgbTooltip, ButtonComponent, DataTableComponent, SwitchComponent],
  templateUrl: './planif-tab-fne.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './planif-tab-fne.component.scss',
})
export class PlanifTabFneComponent {
  protected readonly state = inject(PlanificationStateService);
}

