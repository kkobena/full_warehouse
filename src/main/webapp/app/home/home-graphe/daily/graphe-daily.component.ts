import { Component, OnInit, inject } from '@angular/core';
import { TOPS } from '../../../shared/constants/pagination.constants';
import { faChartArea } from '@fortawesome/free-solid-svg-icons';
import { StatGroupBy } from '../../../shared/model/enumerations/type-vente.model';
import { VentePeriodeRecord } from '../../../shared/model/vente-record.model';
import { Observable } from 'rxjs';
import { CaPeriodeFilter } from '../../../shared/model/enumerations/ca-periode-filter.model';
import { CharBuilderService } from '../../../shared/util/char-builder.service';
import { LineChart } from '../../../shared/model/line-chart.model';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { DropdownModule } from 'primeng/dropdown';
import { TableModule } from 'primeng/table';
import { FormsModule } from '@angular/forms';
import { ChartModule } from 'primeng/chart';

@Component({
    selector: 'jhi-graphe-daily',
    templateUrl: './graphe-daily.component.html',
    styleUrls: ['./graphe-daily.component.scss'],
    imports: [WarehouseCommonModule, DropdownModule, TableModule, FormsModule, ChartModule]
})
export class GrapheDailyComponent implements OnInit {
  private charBuilderService = inject(CharBuilderService);

  protected readonly TOPS = TOPS;
  protected readonly faChartArea = faChartArea;
  protected sales: VentePeriodeRecord[] = [];
  protected dashboardPeriode: CaPeriodeFilter = CaPeriodeFilter.yearly;
  protected venteStatGroupBy: StatGroupBy = StatGroupBy.HOUR;
  protected data: any;
  protected options: any;
  protected lineChart: LineChart;

  /** Inserted by Angular inject() migration for backwards compatibility */
  constructor(...args: unknown[]);

  constructor() {}

  ngOnInit(): void {
    this.onFetchLineChartSalesData();
  }

  onFetchLineChartSalesData(): void {
    this.subscribeLineChartResponse(
      this.charBuilderService.buildLineTimeSerie({
        dashboardPeriode: this.dashboardPeriode,
        venteStatGroupBy: this.venteStatGroupBy,
      }),
    );
  }

  protected subscribeLineChartResponse(result: Observable<LineChart>): void {
    result.subscribe((lineChart: LineChart) => this.onLineChartSuccess(lineChart));
  }

  protected onLineChartSuccess(lineChart: LineChart): void {
    this.lineChart = lineChart;
  }
}
