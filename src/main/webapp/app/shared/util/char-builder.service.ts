import { Injectable } from '@angular/core';
import { DashboardService } from '../../home/dashboard.service';
import { Observable } from 'rxjs';
import { VenteRecordParam } from '../model/vente-record-param.model';
import { VentePeriodeRecord } from '../model/vente-record.model';
import { LineChart, LineChartWrapper } from '../model/line-chart.model';
import { map } from 'rxjs/operators';
import { HttpResponse } from '@angular/common/http';

@Injectable({
  providedIn: 'root',
})
export class CharBuilderService {
  protected documentStyle: CSSStyleDeclaration;
  protected textColor: string;
  protected textColorSecondary: string;
  protected surfaceBorder: string;

  constructor(private dashboardService: DashboardService) {
    this.documentStyle = getComputedStyle(document.documentElement);
    this.textColor = this.documentStyle.getPropertyValue('--text-color');
    this.textColorSecondary = this.documentStyle.getPropertyValue('--text-color-secondary');
    this.surfaceBorder = this.documentStyle.getPropertyValue('--surface-border');
  }

  public subscribeSalesResponse(venteRecordParam: VenteRecordParam): Observable<LineChartWrapper> {
    return this.dashboardService
      .getCaGroupingByPeriode(venteRecordParam)
      .pipe(map((res: HttpResponse<VentePeriodeRecord[]>) => this.buildSaleTimeBasedChartLineData(res.body)));
  }

  public buildLineTimeSerie(venteRecordParam: VenteRecordParam): Observable<LineChart> {
    return this.subscribeSalesResponse(venteRecordParam).pipe(map((e: LineChartWrapper) => this.buildChartLine(e)));
  }

  protected buildChartLine(lineChartWrapper: LineChartWrapper): LineChart {
    return {
      data: {
        labels: lineChartWrapper.labeles,
        datasets: [
          {
            label: 'Montant HT',
            data: lineChartWrapper.saleAmount.data,
            fill: false,
            borderColor: this.documentStyle.getPropertyValue('--pink-500'),
            tension: 0.4,
          },

          {
            label: 'Montant tva',
            data: lineChartWrapper.tva.data,
            fill: false,
            borderColor: this.documentStyle.getPropertyValue('--blue-500'),
            tension: 0.4,
          },
          {
            label: 'Valeur Achat',
            data: lineChartWrapper.saleCostAmount.data,
            fill: false,
            borderColor: this.documentStyle.getPropertyValue('--green-500'),
            tension: 0.4,
          },
          {
            label: 'Marge',
            data: lineChartWrapper.marge.data,
            fill: false,
            borderColor: this.documentStyle.getPropertyValue('--teal-500'),
            tension: 0.4,
          },
        ],
      },
      options: {
        maintainAspectRatio: false,
        responsive: false,
        aspectRatio: 0.4,
        plugins: {
          legend: {
            labels: {
              color: this.textColor,
            },
          },
        },
        scales: {
          x: {
            ticks: {
              color: this.textColorSecondary,
            },
            grid: {
              color: this.surfaceBorder,
              drawBorder: false,
            },
          },
          y: {
            ticks: {
              color: this.textColorSecondary,
            },
            grid: {
              color: this.surfaceBorder,
              drawBorder: false,
            },
          },
        },
      },
    };
  }

  private buildSaleTimeBasedChartLineData(periodeRecords: VentePeriodeRecord[]): LineChartWrapper {
    const labeles: string[] = [];
    const data: number[] = [];
    const saleCostAmount: number[] = [];
    const marge: number[] = [];
    const tva: number[] = [];
    for (const periodeRecord of periodeRecords) {
      labeles.push(periodeRecord.dateMvt);
      data.push(periodeRecord.venteRecord.htAmount);
      saleCostAmount.push(periodeRecord.venteRecord.costAmount);
      tva.push(periodeRecord.venteRecord.taxAmount);
      marge.push(periodeRecord.venteRecord.marge);
    }
    return {
      labeles,
      saleAmount: { data },
      saleCostAmount: {
        data: saleCostAmount,
      },
      marge: { data: marge },
      tva: { data: tva },
    };
  }
}
