import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  input,
  OnInit,
  signal
} from '@angular/core';
import {TypeFinancialTransaction} from "../../cash-register/model/cash-register.model";
import {HttpResponse} from "@angular/common/http";
import {TaxeReportService} from "./taxe-report.service";
import {NGB_DATE_TO_ISO, TYPE_AFFICHAGE} from "../../../shared/util/warehouse-util";
import {getTypeVentes, MvtCaisseParams} from "../mvt-caisse-util";
import {TaxeWrapper} from "./taxe-report.model";
import {ChartComponent} from "app/shared/chart/chart.component";
import {DoughnutChart} from "../../../shared/model/doughnut-chart.model";
import {MvtParamServiceService} from "../mvt-param-service.service";
import {FormsModule} from "@angular/forms";
import {ChartColorsUtilsService} from "../../../shared/util/chart-colors-utils.service";
import {finalize} from "rxjs/operators";
import {CommonModule} from "@angular/common";
import {NotificationService} from "../../../shared/services/notification.service";
import {BlobDownloadService} from "../../../shared/services/blob-download.service";
import {NgbDateStruct, NgbTooltip} from "@ng-bootstrap/ng-bootstrap";
import {ButtonComponent, SelectComponent, ToolbarComponent} from "../../../shared/ui";
import {PharmaDatePickerComponent} from "../../../shared/date-picker/pharma-date-picker.component";
import {ModeCa, ModeCaService} from '../mode-ca';

@Component({
  selector: "app-taxe-report",
  imports: [
    CommonModule,
    FormsModule,
    ButtonComponent,
    ToolbarComponent,
    SelectComponent,
    ChartComponent,
    PharmaDatePickerComponent,
    NgbTooltip
  ],
  templateUrl: "./taxe-report.component.html",
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrls: ["./taxe-report.component.scss"]
})
export class TaxeReportComponent implements OnInit {
  /**
   * Mode imposé par l'écran appelant. Laissé vide, il est déduit de la licence : le chiffre déclaré
   * n'a de sens que si l'officine a souscrit au moins un module de retraitement.
   */
  readonly mode = input<ModeCa | null>(null);

  /**
   * Code de l'entrée de navigation dont cet écran est le contenu, ex. `comptabilite.balance`.
   *
   * <p>Fourni par l'appelant et non écrit en dur : cet écran est ouvert depuis deux menus — la
   * Comptabilité et le Retraitement du CA — qui le nomment différemment. Le titre suit donc le
   * menu par lequel on y est entré.
   */
  readonly navCode = input<string>('');
  protected readonly fromDate = signal<NgbDateStruct | null>(null);
  protected readonly toDate = signal<NgbDateStruct | null>(null);
  protected readonly loading = signal(false);
  protected types: TypeFinancialTransaction[] = [TypeFinancialTransaction.CASH_SALE, TypeFinancialTransaction.CREDIT_SALE];
  protected readonly selectedVente = signal<TypeFinancialTransaction | null>(null);
  protected readonly taxeReportWrapper = signal<TaxeWrapper | null>(null);
  protected readonly groupBy = signal("codeTva");
  protected readonly affichage = signal("table");
  protected readonly typeAffichafes = TYPE_AFFICHAGE;
  protected readonly doughnutChart = signal<DoughnutChart | null>(null);
  private readonly modeCaService = inject(ModeCaService);
  protected readonly modeEffectif = computed<ModeCa>(() => this.mode() ?? this.modeCaService.modeComptabilite());
  private readonly taxeReportService = inject(TaxeReportService);
  private readonly mvtParamServiceService = inject(MvtParamServiceService);
  private readonly chartColorsUtilsService = inject(ChartColorsUtilsService);
  private readonly notificationService = inject(NotificationService);
  private readonly blobDownloadService = inject(BlobDownloadService);

  ngOnInit(): void {
    const params = this.mvtParamServiceService.mvtCaisseParam();
    if (params) {
      this.fromDate.set(params.fromDate);
      this.toDate.set(params.toDate);
      this.selectedVente.set(params.selectedVente ?? null);
      this.groupBy.set(params.groupByTva || "codeTva");
    }

    this.onSearch();
  }


  onSearch(): void {
    this.loading.set(true);
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
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: blob => {
          this.blobDownloadService.downloadPdf(blob, "rapport_tva");

        },
        error: () => this.notificationService.error("Une erreur est survenue lors de l'export PDF")
      });
    this.updateParam();
  }

  private onSuccess(data: TaxeWrapper | null): void {
    this.taxeReportWrapper.set(data ?? null);
    this.loading.set(false);
    this.buildChartLine();
  }

  private onError(): void {
    this.notificationService.error("Une erreur est survenue lors de la récupération des données");
    this.taxeReportWrapper.set(null);
    this.loading.set(false);
  }

  private buildChartLine(): void {
    const chart = this.taxeReportWrapper();
    this.doughnutChart.set({
      data: {
        labels: chart?.chart.labeles,
        datasets: [
          {
            data: chart?.chart.data,
            backgroundColor: this.chartColorsUtilsService.colors().slice(0, chart?.chart.labeles.length),
            hoverBackgroundColor: this.chartColorsUtilsService.hoverColors().slice(0, chart?.chart.labeles.length)
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
    });
  }

  private buildParams(): any {
    return {
      mode: this.modeEffectif(),
      fromDate: this.fromDate() ? NGB_DATE_TO_ISO(this.fromDate()!) : null,
      toDate: this.toDate() ? NGB_DATE_TO_ISO(this.toDate()!) : null,
      typeVentes: getTypeVentes(this.selectedVente()),
      groupBy: this.groupBy(),
      statuts: ["CLOSED"]
    };
  }

  private setParam(): void {
    const param: MvtCaisseParams = {
      fromDate: this.fromDate(),
      toDate: this.toDate(),
      selectedVente: this.selectedVente(),
      groupByTva: this.groupBy()
    };
    this.mvtParamServiceService.setMvtCaisseParam(param);
  }

  private updateParam(): void {
    const params = this.mvtParamServiceService.mvtCaisseParam();
    if (params) {
      params.fromDate = this.fromDate();
      params.toDate = this.toDate();
      params.selectedVente = this.selectedVente();
      params.groupByTva = this.groupBy();
      this.mvtParamServiceService.setMvtCaisseParam(params);
    } else {
      this.setParam();
    }
  }
}
