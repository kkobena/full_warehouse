import { Component, inject, OnInit, ChangeDetectionStrategy } from "@angular/core";
import { Button } from "primeng/button";
import { InputTextModule } from "primeng/inputtext";
import { MultiSelectModule } from "primeng/multiselect";
import { PaginatorModule } from "primeng/paginator";
import { ToolbarModule } from "primeng/toolbar";
import { TooltipModule } from "primeng/tooltip";
import { TypeFinancialTransaction } from "../../cash-register/model/cash-register.model";
import { HttpResponse } from "@angular/common/http";
import { TaxeReportService } from "./taxe-report.service";
import { DATE_FORMAT_ISO_DATE, TYPE_AFFICHAGE } from "../../../shared/util/warehouse-util";
import { getTypeVentes, MvtCaisseParams } from "../mvt-caisse-util";
import { TaxeWrapper } from "./taxe-report.model";
import { RadioButtonModule } from "primeng/radiobutton";

import { SelectButtonModule } from "primeng/selectbutton";
import { ChartComponent } from "app/shared/chart/chart.component";
import { DoughnutChart } from "../../../shared/model/doughnut-chart.model";
import { CardModule } from "primeng/card";
import { MvtParamServiceService } from "../mvt-param-service.service";
import { FormsModule } from "@angular/forms";
import { FloatLabel } from "primeng/floatlabel";
import { DatePickerModule } from "primeng/datepicker";
import { Select } from "primeng/select";
import { ChartColorsUtilsService } from "../../../shared/util/chart-colors-utils.service";
import { finalize } from "rxjs/operators";
import { CommonModule } from "@angular/common";
import { NotificationService } from "../../../shared/services/notification.service";
import { BlobDownloadService } from "../../../shared/services/blob-download.service";
import { Toast } from "primeng/toast";

@Component({
  selector: "app-taxe-report",
  imports: [
    Button,
    CommonModule,
    InputTextModule,
    MultiSelectModule,
    PaginatorModule,
    ToolbarModule,
    TooltipModule,
    RadioButtonModule,
    SelectButtonModule,
    ChartComponent,
    CardModule,
    FormsModule,
    FloatLabel,
    DatePickerModule,
    Select,
    Toast
  ],
  templateUrl: "./taxe-report.component.html",
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrls: ["./taxe-report.component.scss"]
})
export class TaxeReportComponent implements OnInit {
  protected fromDate: Date | undefined;
  protected toDate: Date | undefined;
  protected loading = false;
  protected types: TypeFinancialTransaction[] = [TypeFinancialTransaction.CASH_SALE, TypeFinancialTransaction.CREDIT_SALE];
  protected selectedVente: TypeFinancialTransaction | null = null;
  protected taxeReportWrapper: TaxeWrapper | null = null;
  protected groupBy = "codeTva";
  protected affichage = "table";
  protected readonly typeAffichafes = TYPE_AFFICHAGE;
  protected doughnutChart: DoughnutChart | null = null;
  private readonly taxeReportService = inject(TaxeReportService);
  private readonly mvtParamServiceService = inject(MvtParamServiceService);
  private readonly chartColorsUtilsService = inject(ChartColorsUtilsService);
  private readonly notificationService = inject(NotificationService);
  private readonly blobDownloadService = inject(BlobDownloadService);

  ngOnInit(): void {
    const params = this.mvtParamServiceService.mvtCaisseParam();
    if (params) {
      this.fromDate = params.fromDate;
      this.toDate = params.toDate;
      this.selectedVente = params.selectedVente;
      this.groupBy = params.groupByTva || "codeTva";
    }

    this.onSearch();
  }


  onSearch(): void {
    this.loading = true;
    this.taxeReportService
      .query({
        ...this.buildParams()
      })
      .subscribe({
        next: (res: HttpResponse<TaxeWrapper>) => this.onSuccess(res.body),
        error: () => this.onError()
      });
    this.updateParam();
  }

  onAffichageChange(): void {
    this.onSearch();
  }

  onPrint(): void {
    this.taxeReportService
      .exportToPdf({
        ...this.buildParams()
      })
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: blob => {
          this.blobDownloadService.downloadPdf(blob, "rapport_tva");

        },
        error: () => this.notificationService.error("Une erreur est survenue lors de l'export PDF")
      });
    this.updateParam();
  }

  private onSuccess(data: TaxeWrapper | null): void {
    this.taxeReportWrapper = data || null;
    this.loading = false;
    this.buildChartLine();
  }

  private onError(): void {
    this.notificationService.error("Une erreur est survenue lors de la récupération des données");
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
            hoverBackgroundColor: this.chartColorsUtilsService.hoverColors().slice(0, this.taxeReportWrapper?.chart.labeles.length)
          }
        ]
      },
      options: {
        maintainAspectRatio: false,
        cutout: "40%",
        plugins: {
          legend: {
            labels: {
              color: this.chartColorsUtilsService.textColor()
            }
          }
        }
      }
    };
  }

  private buildParams(): any {
    return {
      fromDate: DATE_FORMAT_ISO_DATE(this.fromDate),
      toDate: DATE_FORMAT_ISO_DATE(this.toDate),
      typeVentes: getTypeVentes(this.selectedVente),
      groupBy: this.groupBy,
      statuts: ["CLOSED"]
    };
  }

  private setParam(): void {
    const param: MvtCaisseParams = {
      fromDate: this.fromDate,
      toDate: this.toDate,
      selectedVente: this.selectedVente,
      groupByTva: this.groupBy
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
