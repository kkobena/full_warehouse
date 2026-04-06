import { Component, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { ToggleSwitch } from 'primeng/toggleswitch';
import { Tooltip } from 'primeng/tooltip';

import { PlanificationStateService } from '../planification-state.service';

@Component({
  selector: 'app-planif-tab-fne',
  imports: [DatePipe, FormsModule, TableModule, ButtonModule, ToggleSwitch, Tooltip],
  templateUrl: './planif-tab-fne.component.html',
  styleUrl: './planif-tab-fne.component.scss',
})
export class PlanifTabFneComponent {
  protected readonly state = inject(PlanificationStateService);
}

