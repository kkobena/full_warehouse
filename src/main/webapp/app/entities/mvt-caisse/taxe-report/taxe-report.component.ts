import { AfterViewInit, Component, inject, OnInit } from '@angular/core';
import { Button } from 'primeng/button';
import { CalendarModule } from 'primeng/calendar';
import { DropdownModule } from 'primeng/dropdown';
import { InputTextModule } from 'primeng/inputtext';
import { MultiSelectModule } from 'primeng/multiselect';
import { PaginatorModule } from 'primeng/paginator';
import { ToolbarModule } from 'primeng/toolbar';
import { TooltipModule } from 'primeng/tooltip';
import { TypeFinancialTransaction } from '../../cash-register/model/cash-register.model';
import { ConfirmationService, MessageService, PrimeNGConfig } from 'primeng/api';
import { TranslateService } from '@ngx-translate/core';
import { HttpResponse } from '@angular/common/http';
import { TaxeReportService } from './taxe-report.service';
import { DATE_FORMAT_ISO_DATE } from '../../../shared/util/warehouse-util';
import { getTypeVentes, MvtCaisseParams } from '../mvt-caisse-util';
import { TaxeWrapper } from './taxe-report.model';
import { RadioButtonModule } from 'primeng/radiobutton';
import { DatePipe, DecimalPipe } from '@angular/common';
import { SelectButtonModule } from 'primeng/selectbutton';
import { ChartModule } from 'primeng/chart';
import { DoughnutChart } from '../../../shared/model/doughnut-chart.model';
import { CardModule } from 'primeng/card';
import { MvtParamServiceService } from '../mvt-param-service.service';
import { ToastModule } from 'primeng/toast';

@Component({
  selector: 'jhi-taxe-report',
  standalone: true,
  providers: [MessageService, ConfirmationService],
  imports: [
    Button,
    CalendarModule,
    DropdownModule,
    InputTextModule,
    MultiSelectModule,
    PaginatorModule,
    ToolbarModule,
    TooltipModule,
    RadioButtonModule,
    DecimalPipe,
    DatePipe,
    SelectButtonModule,
    ChartModule,
    CardModule,
    ToastModule,
  ],
  templateUrl: './taxe-report.component.html',
})
export class TaxeReportComponent implements OnInit, AfterViewInit {
  protected documentStyle: CSSStyleDeclaration;
  protected fromDate: Date | undefined;
  protected toDate: Date | undefined;
  protected loading = false;
  protected types: TypeFinancialTransaction[] = [TypeFinancialTransaction.CASH_SALE, TypeFinancialTransaction.CREDIT_SALE];
  protected selectedVente: TypeFinancialTransaction | null = null;
  protected taxeReportWrapper: TaxeWrapper | null = null;
  protected groupBy = 'codeTva';
  protected affichage = 'table';
  protected typeAffichafes = [
    { icon: 'pi pi-align-justify', value: 'table' },
    { icon: 'pi pi-chart-bar', value: 'graphe' },
  ];
  protected doughnutChart: DoughnutChart | null = null;
  protected textColor: string;
  protected textColorSecondary: string;
  protected surfaceBorder: string;
  private primeNGConfig = inject(PrimeNGConfig);
  private translate = inject(TranslateService);
  private messageService = inject(MessageService);
  private taxeReportService = inject(TaxeReportService);
  private colors: string[] = [];
  private hoverColors: string[] = [];
  private mvtParamServiceService = inject(MvtParamServiceService);

  constructor() {
    this.documentStyle = getComputedStyle(document.documentElement);
    this.textColor = this.documentStyle.getPropertyValue('--text-color');
    this.textColorSecondary = this.documentStyle.getPropertyValue('--text-color-secondary');
    this.surfaceBorder = this.documentStyle.getPropertyValue('--surface-border');
  }

  ngOnInit(): void {
    const params = this.mvtParamServiceService.mvtCaisseParam();
    if (params) {
      this.fromDate = params.fromDate;
      this.toDate = params.toDate;
      this.selectedVente = params.selectedVente;
      this.groupBy = params.groupByTva || 'codeTva';
    }

    this.onSearch();
    this.colors = [
      this.documentStyle.getPropertyValue('--blue-300'),
      this.documentStyle.getPropertyValue('--yellow-300'),
      this.documentStyle.getPropertyValue('--green-300'),
      this.documentStyle.getPropertyValue('--pink-300'),
      this.documentStyle.getPropertyValue('--orange-300'),
      this.documentStyle.getPropertyValue('--red-300'),
    ];
    this.hoverColors = [
      this.documentStyle.getPropertyValue('--blue-200'),
      this.documentStyle.getPropertyValue('--yellow-200'),
      this.documentStyle.getPropertyValue('--green-200'),
      this.documentStyle.getPropertyValue('--pink-200'),
      this.documentStyle.getPropertyValue('--orange-200'),
      this.documentStyle.getPropertyValue('--red-200'),
    ];
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
      .subscribe({
        next(blod) {
          const blobUrl = URL.createObjectURL(blod);
          window.open(blobUrl);
        },
        error: () => {
          this.loading = false;
          this.messageService.add({
            severity: 'error',
            summary: 'Error',
            detail: 'Une erreur est survenue',
          });
        },
        complete: () => {
          this.loading = false;
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
    this.messageService.add({
      severity: 'error',
      summary: 'Error',
      detail: 'Une erreur est survenue',
    });
    this.taxeReportWrapper = null;
    this.loading = false;
  }

  private buildChartLine(): void {
    this.doughnutChart = {
      data: {
        labels: this.taxeReportWrapper?.chart?.labeles,
        datasets: [
          {
            data: this.taxeReportWrapper?.chart?.data,
            backgroundColor: this.colors.slice(0, this.taxeReportWrapper?.chart?.labeles.length),
            hoverBackgroundColor: this.hoverColors.slice(0, this.taxeReportWrapper?.chart?.labeles.length),
          },
        ],
      },
      options: {
        maintainAspectRatio: false,
        cutout: '40%',
        plugins: {
          legend: {
            labels: {
              color: this.textColor,
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
