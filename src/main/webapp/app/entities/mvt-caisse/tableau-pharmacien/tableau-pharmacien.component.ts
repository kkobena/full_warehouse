import { AfterViewInit, Component, inject, OnInit } from '@angular/core';
import { ConfirmationService, MenuItem, MessageService, PrimeNGConfig } from 'primeng/api';
import { TranslateService } from '@ngx-translate/core';
import { TableauPharmacienService } from './tableau-pharmacien.service';
import { HttpResponse } from '@angular/common/http';
import { DATE_FORMAT_ISO_DATE } from '../../../shared/util/warehouse-util';
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
  private primeNGConfig = inject(PrimeNGConfig);
  private translate = inject(TranslateService);
  private messageService = inject(MessageService);
  private tableauPharmacienService = inject(TableauPharmacienService);
  private mvtParamServiceService = inject(MvtParamServiceService);

  ngOnInit(): void {
    this.exportMenus = [
      {
        label: 'PDF',
        icon: 'pi pi-file-pdf',
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
    return tableauPharmacien.groupAchats?.get(group?.id || 0)?.reduce((acc, achat) => acc + achat.montantNet, 0) || 0;
  }

  private onSuccess(data: TableauPharmacienWrapper | null): void {
    this.tableauPharmacienWrapper = data || null;
    this.loading = false;
    //  this.buildChartLine();
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
}
