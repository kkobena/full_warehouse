import { AfterViewInit, Component, inject, OnInit, viewChild } from '@angular/core';
import { Button } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { MultiSelectModule } from 'primeng/multiselect';
import { PaginatorModule } from 'primeng/paginator';
import { ToolbarModule } from 'primeng/toolbar';
import { TooltipModule } from 'primeng/tooltip';
import { TypeFinancialTransaction } from '../../cash-register/model/cash-register.model';
import { TranslateService } from '@ngx-translate/core';
import { HttpResponse } from '@angular/common/http';
import { TaxeReportService } from './taxe-report.service';
import { DATE_FORMAT_ISO_DATE, TYPE_AFFICHAGE } from '../../../shared/util/warehouse-util';
import { getTypeVentes, MvtCaisseParams } from '../mvt-caisse-util';
import { TaxeWrapper } from './taxe-report.model';
import { RadioButtonModule } from 'primeng/radiobutton';

import { SelectButtonModule } from 'primeng/selectbutton';
import { ChartModule } from 'primeng/chart';
import { DoughnutChart } from '../../../shared/model/doughnut-chart.model';
import { CardModule } from 'primeng/card';
import { MvtParamServiceService } from '../mvt-param-service.service';
import { FormsModule } from '@angular/forms';
import { PrimeNG } from 'primeng/config';
import { FloatLabel } from 'primeng/floatlabel';
import { DatePickerModule } from 'primeng/datepicker';
import { Select } from 'primeng/select';
import { ChartColorsUtilsService } from '../../../shared/util/chart-colors-utils.service';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { ToastAlertComponent } from '../../../shared/toast-alert/toast-alert.component';
import { finalize } from 'rxjs/operators';
import { TauriPrinterService } from '../../../shared/services/tauri-printer.service';
import { handleBlobForTauri } from '../../../shared/util/tauri-util';

@Component({
  selector: 'jhi-taxe-report',
  imports: [
    WarehouseCommonModule,
    Button,
    InputTextModule,
    MultiSelectModule,
    PaginatorModule,
    ToolbarModule,
    TooltipModule,
    RadioButtonModule,
    SelectButtonModule,
    ChartModule,
    CardModule,
    FormsModule,
    FloatLabel,
    DatePickerModule,
    Select,
    ToastAlertComponent,
  ],
  templateUrl: './taxe-report.component.html',
  styleUrls: ['./taxe-report.component.scss'],
})
export class TaxeReportComponent implements OnInit, AfterViewInit {
  protected fromDate: Date | undefined;
  protected toDate: Date | undefined;
  protected loading = false;
  protected types: TypeFinancialTransaction[] = [TypeFinancialTransaction.CASH_SALE, TypeFinancialTransaction.CREDIT_SALE];
  protected selectedVente: TypeFinancialTransaction | null = null;
  protected taxeReportWrapper: TaxeWrapper | null = null;
  protected groupBy = 'codeTva';
  protected affichage = 'table';
  protected readonly typeAffichafes = TYPE_AFFICHAGE;
  protected doughnutChart: DoughnutChart | null = null;
  private readonly primeNGConfig = inject(PrimeNG);
  private readonly translate = inject(TranslateService);
  private readonly taxeReportService = inject(TaxeReportService);
  private readonly mvtParamServiceService = inject(MvtParamServiceService);
  private readonly chartColorsUtilsService = inject(ChartColorsUtilsService);
  private readonly alert = viewChild.required<ToastAlertComponent>('alert');
  private readonly tauriPrinterService = inject(TauriPrinterService);
  ngOnInit(): void {
    const params = this.mvtParamServiceService.mvtCaisseParam();
    if (params) {
      this.fromDate = params.fromDate;
      this.toDate = params.toDate;
      this.selectedVente = params.selectedVente;
      this.groupBy = params.groupByTva || 'codeTva';
    }

    this.onSearch();
  }

  ngAfterViewInit(): void {
    this.translate.use('fr');
    this.translate.stream('primeng').subscribe(data => {
      this.primeNGConfig.setTranslation(data);
    });
  }

  onSearch(): void {
    this.loading = true;
    this.taxeReportService
      .query({
        ...this.buildParams(),
      })
      .subscribe({
        next: (res: HttpResponse<TaxeWrapper>) => this.onSuccess(res.body),
        error: () => this.onError(),
      });
    this.updateParam();
  }

  onAffichageChange(): void {
    this.onSearch();
  }

  onPrint(): void {
    this.taxeReportService
      .exportToPdf({
        ...this.buildParams(),
      })
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: blob => {
          if (this.tauriPrinterService.isRunningInTauri()) {
            handleBlobForTauri(blob, 'rapport_tva');
          } else {
            window.open(URL.createObjectURL(blob));
          }
        },
        error: () => {
          this.alert().showError("Une erreur est survenue lors de l'export PDF");
        },
      });
    this.updateParam();
  }

  private onSuccess(data: TaxeWrapper | null): void {
    this.taxeReportWrapper = data || null;
    this.loading = false;
    this.buildChartLine();
  }

  private onError(): void {
    this.alert().showError('Une erreur est survenue lors de la récupération des données');
    this.taxeReportWrapper = null;
    this.loading = false;
  }

  private buildChartLine(): void {
    this.doughnutChart = {
      data: {
        labels: this.taxeReportWrapper?.chart.labeles,
        datasets: [
          {
            data: this.taxeReportWrapper?.chart.data,
            backgroundColor: this.chartColorsUtilsService.colors().slice(0, this.taxeReportWrapper?.chart.labeles.length),
            hoverBackgroundColor: this.chartColorsUtilsService.hoverColors().slice(0, this.taxeReportWrapper?.chart.labeles.length),
          },
        ],
      },
      options: {
        maintainAspectRatio: false,
        cutout: '40%',
        plugins: {
          legend: {
            labels: {
              color: this.chartColorsUtilsService.textColor(),
            },
          },
        },
      },
    };
  }
  private buildParams(): any {
    return {
      fromDate: DATE_FORMAT_ISO_DATE(this.fromDate),
      toDate: DATE_FORMAT_ISO_DATE(this.toDate),
      typeVentes: getTypeVentes(this.selectedVente),
      groupBy: this.groupBy,
      statuts: ['CLOSED'],
    };
  }

  private setParam(): void {
    const param: MvtCaisseParams = {
      fromDate: this.fromDate,
      toDate: this.toDate,
      selectedVente: this.selectedVente,
      groupByTva: this.groupBy,
    };
    this.mvtParamServiceService.setMvtCaisseParam(param);
  }

  private updateParam(): void {
    const params = this.mvtParamServiceService.mvtCaisseParam();
    if (params) {
      params.fromDate = this.fromDate;
      params.toDate = this.toDate;
      params.selectedVente = this.selectedVente;
      params.groupByTva = this.groupBy;
      this.mvtParamServiceService.setMvtCaisseParam(params);
    } else {
      this.setParam();
    }
  }
}
