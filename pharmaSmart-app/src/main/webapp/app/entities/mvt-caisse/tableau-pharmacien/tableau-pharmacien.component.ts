import { Component, inject, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { TableauPharmacienService } from './tableau-pharmacien.service';
import { HttpResponse } from '@angular/common/http';
import { FORMAT_ISO_DATE_TO_STRING_FR, NGB_DATE_TO_ISO } from '../../../shared/util/warehouse-util';
import { TableauPharmacien, TableauPharmacienWrapper } from './tableau-pharmacien.model';
import { ChartComponent } from 'app/shared/chart/chart.component';
import { IGroupeFournisseur } from '../../../shared/model/groupe-fournisseur.model';
import { MvtParamServiceService } from '../mvt-param-service.service';
import { MvtCaisseParams } from '../mvt-caisse-util';
import { VerticalBarChart } from '../../../shared/model/vertical-bar-chart.model';
import { FormsModule } from '@angular/forms';
import { ChartColorsUtilsService } from '../../../shared/util/chart-colors-utils.service';
import { saveAs } from 'file-saver';
import { extractFileName } from '../../../shared/util/file-utils';
import { NotificationService } from '../../../shared/services/notification.service';
import { finalize } from 'rxjs/operators';
import { handleBlobForTauri } from '../../../shared/util/tauri-util';
import { TauriPrinterService } from '../../../shared/services/tauri-printer.service';
import { CommonModule } from "@angular/common";
import { NgbDateStruct, NgbTooltip } from '@ng-bootstrap/ng-bootstrap';
import { AppSplitButtonItem, ButtonComponent, SplitButtonComponent, ToolbarComponent } from '../../../shared/ui';
import { PharmaDatePickerComponent } from '../../../shared/date-picker/pharma-date-picker.component';

@Component({
  selector: 'jhi-tableau-pharmacien',
  imports: [
    CommonModule,
    FormsModule,
    ButtonComponent,
    ToolbarComponent,
    SplitButtonComponent,
    ChartComponent,
    PharmaDatePickerComponent,
    NgbTooltip,
  ],
  templateUrl: './tableau-pharmacien.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrls: ['./tableau-pharmacien.component.scss'],
})
export class TableauPharmacienComponent implements OnInit {
  protected exportMenus: AppSplitButtonItem[];
  protected fromDate: NgbDateStruct | null = null;
  protected groupBy = 'daily';
  protected toDate: NgbDateStruct | null = null;
  protected loading = false;
  protected affichage = 'table';
  protected typeAffichafes = [
    { icon: 'pi pi-align-justify', value: 'table' },
    { icon: 'pi pi-chart-bar', value: 'graphe' },
  ];
  protected tableauPharmacienWrapper: TableauPharmacienWrapper | null = null;
  protected groupeFournisseurs: IGroupeFournisseur[] = [];
  protected colspan = 9;
  protected verticalBarChart: VerticalBarChart | null = null;
  protected grossiste: VerticalBarChart | null = null;

  protected showGrossisteChart = false;
  private readonly tauriPrinterService = inject(TauriPrinterService);
  private tableauPharmacienService = inject(TableauPharmacienService);
  private mvtParamServiceService = inject(MvtParamServiceService);
  private chartColorsUtilsService = inject(ChartColorsUtilsService);
  private readonly notificationService = inject(NotificationService);

  ngOnInit(): void {
    this.exportMenus = [
      {
        label: 'PDF',
        icon: 'pi pi-file-pdf',
        command: () => this.onPrint(),
      },
      {
        label: 'Excel',
        icon: 'pi pi-file-excel',
        command: () => this.onExcel(),
      },
    ];
    const params = this.mvtParamServiceService.mvtCaisseParam();
    if (params) {
      this.fromDate = params.fromDate;
      this.toDate = params.toDate;
      this.groupBy = params.groupBy || 'daily';
    }
    this.fetchGroupGrossisteToDisplay();
    this.onSearch();
  }

  onAffichageChange(): void {
    this.onSearch();
  }

  onSearch(): void {
    this.loading = true;
    this.tableauPharmacienService
      .query({
        ...this.buildParams(),
      })
      .subscribe({
        next: (res: HttpResponse<TableauPharmacienWrapper>) => this.onSuccess(res.body),
        error: () => this.onError(),
      });
    this.updateParam();
  }

  protected fetchGroupGrossisteToDisplay(): void {
    this.tableauPharmacienService.fetchGroupGrossisteToDisplay().subscribe({
      next: (res: HttpResponse<IGroupeFournisseur[]>) => {
        this.groupeFournisseurs = res.body || [];

        this.colspan = 2 + this.groupeFournisseurs.length;
      },
    });
  }

  protected getGroupFournisseurAmount(group: IGroupeFournisseur | null, tableauPharmacien: TableauPharmacien): number {
    const groupAchats = tableauPharmacien.groupAchats;
    let montant = 0;
    groupAchats.forEach(groupAchat => {
      if (groupAchat.id === group.id) {
        montant += groupAchat.achat.montantNet || 0;
      }
    });
    return montant;
  }

  protected getTotalFournisseurAmount(group: IGroupeFournisseur | null, tableauPharmaciens: TableauPharmacien[]): number {
    return tableauPharmaciens.reduce((acc, tableauPharmacien) => acc + this.getGroupFournisseurAmount(group, tableauPharmacien), 0);
  }

  protected onPrint(): void {
    this.loading = true;
    this.updateParam();
    this.tableauPharmacienService
      .exportToPdf(this.buildParams())
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: blob => {
          if (this.tauriPrinterService.isRunningInTauri()) {
            handleBlobForTauri(blob, 'tableau-pharmacien', 'excel');
          } else {
            window.open(URL.createObjectURL(blob));
          }
        },
        error: () => {
          this.notificationService.error("Une erreur est survenue lors de l'export PDF");
        },
      });
  }

  protected onExcel(): void {
    this.loading = true;
    this.updateParam();
    this.tableauPharmacienService
      .exportToExcel(this.buildParams())
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: resp => {
          const blob = resp.body;
          if (this.tauriPrinterService.isRunningInTauri()) {
            handleBlobForTauri(blob, 'tableau-pharmacien', 'excel');
          } else {
            saveAs(blob, extractFileName(resp.headers.get('content-disposition') || ''));
          }
        },
        error: () => {
          this.notificationService.error("Une erreur est survenue lors de l'export Excel");
        },
      });
  }

  private onSuccess(data: TableauPharmacienWrapper | null): void {
    this.tableauPharmacienWrapper = data || null;
    this.loading = false;
    this.buildChartData(this.tableauPharmacienWrapper.tableauPharmaciens);
  }

  private onError(): void {
    this.notificationService.error('Une erreur est survenue lors de la récupération des données');
    this.tableauPharmacienWrapper = null;
    this.loading = false;
  }

  private buildParams(): any {
    return {
      fromDate: this.fromDate ? NGB_DATE_TO_ISO(this.fromDate) : null,
      toDate: this.toDate ? NGB_DATE_TO_ISO(this.toDate) : null,
      groupBy: this.groupBy,
      statuts: ['CLOSED'],
    };
  }

  private setParam(): void {
    const param: MvtCaisseParams = {
      fromDate: this.fromDate,
      toDate: this.toDate,
      groupBy: this.groupBy,
    };
    this.mvtParamServiceService.setMvtCaisseParam(param);
  }

  private updateParam(): void {
    const params = this.mvtParamServiceService.mvtCaisseParam();
    if (params) {
      params.fromDate = this.fromDate;
      params.toDate = this.toDate;
      params.groupBy = this.groupBy;
      this.mvtParamServiceService.setMvtCaisseParam(params);
    } else {
      this.setParam();
    }
  }

  private buildChartData(tableauPharmaciens: TableauPharmacien[]): void {
    const labels = tableauPharmaciens.map(tableauPharmacien => FORMAT_ISO_DATE_TO_STRING_FR(tableauPharmacien.mvtDate));
    const comptants = tableauPharmaciens.map(tableauPharmacien => tableauPharmacien.montantComptant);
    const credits = tableauPharmaciens.map(tableauPharmacien => tableauPharmacien.montantCredit);
    const montantNets = tableauPharmaciens.map(tableauPharmacien => tableauPharmacien.montantNet);
    const montantRemises = tableauPharmaciens.map(tableauPharmacien => tableauPharmacien.montantRemise);
    this.buildBarChart(labels, comptants, credits, montantNets, montantRemises);
    const achatsGrossiste: any = [];
    const cls = this.chartColorsUtilsService.colors();
    const hColors = this.chartColorsUtilsService.hoverColors();
    const labelsBarGrossiste: string[] = [];
    const dataSet: any[] = [];

    tableauPharmaciens.forEach(t => {
      const achats = t.groupAchats;
      if (achats.length > 0) {
        const validData: any = {};
        const grossisteDuJour: any[] = [];
        labelsBarGrossiste.push(FORMAT_ISO_DATE_TO_STRING_FR(t.mvtDate));
        achats.forEach(achat => {
          grossisteDuJour.push({
            id: achat.id,
            montantAchatNet: achat.achat.montantNet,
          });
        });

        validData['date'] = t.mvtDate;
        validData['grossistes'] = grossisteDuJour;
        dataSet.push(validData);
      }
    });

    const finalData = new Map<string, any>();
    let i = 0;
    this.groupeFournisseurs.forEach(groupe => {
      const montantAchats: number[] = [];
      const chartDataSet = {
        data: montantAchats,
        label: groupe.libelle,
        id: groupe.id,
        backgroundColor: cls.slice(i, i + 1),
        hoverBackgroundColor: hColors.slice(i, i + 1),
      };
      achatsGrossiste.push(chartDataSet);
      i++;
    });

    dataSet.forEach(data => {
      const gr: any[] = [];
      this.groupeFournisseurs.forEach((groupeGrossiste: any) => {
        const grossiste = data.grossistes.find((g: any) => g.id === groupeGrossiste.id);
        if (grossiste) {
          gr.push({ id: grossiste.id, montantAchatNet: grossiste.montantAchatNet });
        }
      });

      finalData.set(data.date, gr);
    });

    achatsGrossiste.forEach((chartDataSet: any) => {
      finalData.forEach(value => {
        let achatDuJour: any;
        value.forEach((v: any) => {
          if (chartDataSet.id === v.id) {
            achatDuJour = v;
            return;
          }
        });
        if (achatDuJour) {
          chartDataSet.data.push(achatDuJour.montantAchatNet);
        } else {
          chartDataSet.data.push(0);
        }
      });
    });

    if (finalData.size > 0) {
      this.showGrossisteChart = true;
      this.buildGrossisteBarChat(labelsBarGrossiste, achatsGrossiste);
    } else {
      this.showGrossisteChart = false;
    }
  }

  private buildBarChart(labels: string[], comptants: number[], credits: number[], montantNets: number[], montantRemises: number[]): void {
    this.verticalBarChart = {
      data: {
        labels,
        datasets: [
          {
            label: 'Comptant',
            data: comptants,
            backgroundColor: this.chartColorsUtilsService.colors().slice(0, 1),
            hoverBackgroundColor: this.chartColorsUtilsService.hoverColors().slice(0, 1),
          },
          {
            label: 'Crédit',
            data: credits,
            backgroundColor: this.chartColorsUtilsService.colors().slice(1, 2),
            hoverBackgroundColor: this.chartColorsUtilsService.hoverColors().slice(1, 2),
          },
          {
            label: 'Montant net',
            data: montantNets,
            backgroundColor: this.chartColorsUtilsService.colors().slice(2, 3),
            hoverBackgroundColor: this.chartColorsUtilsService.hoverColors().slice(2, 3),
          },
          {
            label: 'Montant remise',
            data: montantRemises,
            backgroundColor: this.chartColorsUtilsService.colors().slice(3, 4),
            hoverBackgroundColor: this.chartColorsUtilsService.hoverColors().slice(3, 4),
          },
        ],
      },
      options: {
        maintainAspectRatio: false,
        aspectRatio: 0.8,
        // cutout: '40%',
        plugins: {
          legend: {
            labels: {
              color: this.chartColorsUtilsService.textColor(),
            },
          },
        },
        scales: {
          x: {
            ticks: {
              color: this.chartColorsUtilsService.textColorSecondary(),
              font: {
                weight: 700,
              },
            },
            grid: {
              color: this.chartColorsUtilsService.surfaceBorder(),
              drawBorder: false,
            },
          },
          y: {
            ticks: {
              color: this.chartColorsUtilsService.textColorSecondary(),
            },
            grid: {
              color: this.chartColorsUtilsService.surfaceBorder(),
              drawBorder: false,
            },
          },
        },
      },
    };
  }

  private buildGrossisteBarChat(labels: string[], achatsGrossiste: any = []): void {
    this.grossiste = {
      data: {
        labels,
        datasets: achatsGrossiste,
      },
      options: {
        maintainAspectRatio: false,
        aspectRatio: 0.8,

        plugins: {
          legend: {
            labels: {
              color: this.chartColorsUtilsService.textColor(),
            },
          },
        },
        scales: {
          x: {
            ticks: {
              color: this.chartColorsUtilsService.textColorSecondary(),
              font: {
                weight: 700,
              },
            },
            grid: {
              color: this.chartColorsUtilsService.surfaceBorder(),
              drawBorder: false,
            },
          },
          y: {
            ticks: {
              color: this.chartColorsUtilsService.textColorSecondary(),
            },
            grid: {
              color: this.chartColorsUtilsService.surfaceBorder(),
              drawBorder: false,
            },
          },
        },
      },
    };
  }
}
