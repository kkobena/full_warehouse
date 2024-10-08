import { AfterViewInit, Component, inject, OnInit } from '@angular/core';
import { ConfirmationService, MenuItem, MessageService, PrimeNGConfig } from 'primeng/api';
import { TranslateService } from '@ngx-translate/core';
import { TableauPharmacienService } from './tableau-pharmacien.service';
import { HttpResponse } from '@angular/common/http';
import { DATE_FORMAT_ISO_DATE, FORMAT_ISO_DATE_TO_STRING_FR } from '../../../shared/util/warehouse-util';
import { TableauPharmacien, TableauPharmacienWrapper } from './tableau-pharmacien.model';
import { Button } from 'primeng/button';
import { CalendarModule } from 'primeng/calendar';
import { DropdownModule } from 'primeng/dropdown';
import { RadioButtonModule } from 'primeng/radiobutton';
import { SelectButtonModule } from 'primeng/selectbutton';
import { ToolbarModule } from 'primeng/toolbar';
import { TooltipModule } from 'primeng/tooltip';
import { DialogService } from 'primeng/dynamicdialog';
import { InputTextModule } from 'primeng/inputtext';
import { MultiSelectModule } from 'primeng/multiselect';
import { PaginatorModule } from 'primeng/paginator';
import { DatePipe, DecimalPipe } from '@angular/common';
import { ChartModule } from 'primeng/chart';
import { CardModule } from 'primeng/card';
import { SplitButtonModule } from 'primeng/splitbutton';
import { IGroupeFournisseur } from '../../../shared/model/groupe-fournisseur.model';
import { MvtParamServiceService } from '../mvt-param-service.service';
import { MvtCaisseParams } from '../mvt-caisse-util';
import { VerticalBarChart } from '../../../shared/model/vertical-bar-chart.model';

@Component({
  selector: 'jhi-tableau-pharmacien',
  standalone: true,
  providers: [MessageService, DialogService, ConfirmationService],
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
    SplitButtonModule,
  ],
  templateUrl: './tableau-pharmacien.component.html',
})
export class TableauPharmacienComponent implements OnInit, AfterViewInit {
  protected exportMenus: MenuItem[];
  protected fromDate: Date | undefined;
  protected groupBy = 'daily';
  protected toDate: Date | undefined;
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
  protected textColor: string;
  protected textColorSecondary: string;
  protected surfaceBorder: string;
  protected documentStyle: CSSStyleDeclaration;
  protected showGrossisteChart = false;
  private primeNGConfig = inject(PrimeNGConfig);
  private translate = inject(TranslateService);
  private messageService = inject(MessageService);
  private tableauPharmacienService = inject(TableauPharmacienService);
  private mvtParamServiceService = inject(MvtParamServiceService);
  private colors: string[] = [];
  private hoverColors: string[] = [];

  constructor() {
    this.documentStyle = getComputedStyle(document.documentElement);
    this.textColor = this.documentStyle.getPropertyValue('--text-color');
    this.textColorSecondary = this.documentStyle.getPropertyValue('--text-color-secondary');
    this.surfaceBorder = this.documentStyle.getPropertyValue('--surface-border');
  }

  ngOnInit(): void {
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
    this.exportMenus = [
      {
        label: 'PDF',
        icon: 'pi pi-file-pdf',
        command: () => this.onPrint(),
      },
      {
        label: 'Excel',
        icon: 'pi pi-file-excel',
        command: () => {},
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

  ngAfterViewInit(): void {
    this.translate.use('fr');
    this.translate.stream('primeng').subscribe(data => {
      this.primeNGConfig.setTranslation(data);
    });
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
    this.tableauPharmacienService.exportToPdf(this.buildParams()).subscribe({
      next: blod => {
        this.loading = false;
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
  }

  private onSuccess(data: TableauPharmacienWrapper | null): void {
    this.tableauPharmacienWrapper = data || null;
    this.loading = false;
    this.buildChartData(this.tableauPharmacienWrapper.tableauPharmaciens);
  }

  private onError(): void {
    this.messageService.add({
      severity: 'error',
      summary: 'Error',
      detail: 'Une erreur est survenue',
    });
    this.tableauPharmacienWrapper = null;
    this.loading = false;
  }

  private buildParams(): any {
    return {
      fromDate: DATE_FORMAT_ISO_DATE(this.fromDate),
      toDate: DATE_FORMAT_ISO_DATE(this.toDate),
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
    const cls = this.colors;
    const hColors = this.hoverColors;
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

    const finalData: Map<string, any> = new Map();
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
            backgroundColor: this.colors.slice(0, 1),
            hoverBackgroundColor: this.hoverColors.slice(0, 1),
          },
          {
            label: 'Crédit',
            data: credits,
            backgroundColor: this.colors.slice(1, 2),
            hoverBackgroundColor: this.hoverColors.slice(1, 2),
          },
          {
            label: 'Montant net',
            data: montantNets,
            backgroundColor: this.colors.slice(2, 3),
            hoverBackgroundColor: this.hoverColors.slice(2, 3),
          },
          {
            label: 'Montant remise',
            data: montantRemises,
            backgroundColor: this.colors.slice(3, 4),
            hoverBackgroundColor: this.hoverColors.slice(3, 4),
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
              color: this.textColor,
            },
          },
        },
        scales: {
          x: {
            ticks: {
              color: this.textColorSecondary,
              font: {
                weight: 700,
              },
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
              color: this.textColor,
            },
          },
        },
        scales: {
          x: {
            ticks: {
              color: this.textColorSecondary,
              font: {
                weight: 700,
              },
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
}
