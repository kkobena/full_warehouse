import { Component } from '@angular/core';
import { CaPeriodeFilter } from '../../../shared/model/enumerations/ca-periode-filter.model';
import { DropdownModule } from 'primeng/dropdown';
import { TableModule } from 'primeng/table';
import { FormsModule } from '@angular/forms';
import { HomeBaseComponent } from '../../home-base/home-base.component';
import { CommonModule, DecimalPipe } from '@angular/common';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { ChartModule } from 'primeng/chart';
import { ToggleButtonModule } from 'primeng/togglebutton';

@Component({
  selector: 'jhi-daily-data',
  templateUrl: '../../home-base/home-base.component.html',
  styleUrls: ['../../home-base/home-base.component.scss'],
  imports: [CommonModule, FormsModule, DecimalPipe, DropdownModule, TableModule, FaIconComponent, ChartModule, ToggleButtonModule],
})
export class DailyDataComponent extends HomeBaseComponent {
  protected dashboardPeriode: CaPeriodeFilter | null = null;

  constructor() {
    super();
    this.dashboardPeriode = CaPeriodeFilter.daily;
  }
}
