import { Component } from '@angular/core';
import { CaPeriodeFilter } from '../../../shared/model/enumerations/ca-periode-filter.model';
import { TableModule } from 'primeng/table';
import { FormsModule } from '@angular/forms';
import { CommonModule, DecimalPipe } from '@angular/common';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { HomeBaseComponent } from '../../home-base/home-base.component';
import { ChartModule } from 'primeng/chart';
import { ToggleButtonModule } from 'primeng/togglebutton';
import { SelectModule } from 'primeng/select';

@Component({
  selector: 'jhi-monthly-data',
  templateUrl: '../../home-base/home-base.component.html',
  styleUrls: ['../../home-base/home-base.component.scss'],
  imports: [CommonModule, FormsModule, DecimalPipe, SelectModule, TableModule, FaIconComponent, ChartModule, ToggleButtonModule],
})
export class MonthlyDataComponent extends HomeBaseComponent {
  constructor() {
    super();
    this.dashboardPeriode = CaPeriodeFilter.monthly;
    /*  this.showGraphs = this.toggleStateService.toggleState(); */
  }
}
