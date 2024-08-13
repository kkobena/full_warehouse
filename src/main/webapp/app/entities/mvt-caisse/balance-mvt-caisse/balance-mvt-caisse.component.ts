import { AfterViewInit, Component, inject, OnInit } from '@angular/core';
import { MvtParamServiceService } from '../mvt-param-service.service';
import { BalanceCaisseWrapper } from './balance-caisse.model';
import { BalanceMvtCaisseService } from './balance-mvt-caisse.service';
import { ConfirmationService, MessageService, PrimeNGConfig } from 'primeng/api';
import { TranslateService } from '@ngx-translate/core';
import { DialogService } from 'primeng/dynamicdialog';
import { Button } from 'primeng/button';
import { CalendarModule } from 'primeng/calendar';
import { MultiSelectModule } from 'primeng/multiselect';
import { PaginatorModule } from 'primeng/paginator';
import { ToolbarModule } from 'primeng/toolbar';
import { TooltipModule } from 'primeng/tooltip';
import { DatePipe, DecimalPipe } from '@angular/common';
import { SelectButtonModule } from 'primeng/selectbutton';
import { CardModule } from 'primeng/card';
import { SplitButtonModule } from 'primeng/splitbutton';
import { RadioButtonModule } from 'primeng/radiobutton';
import { HttpResponse } from '@angular/common/http';
import { MvtCaisseParams } from '../mvt-caisse-util';
import { DATE_FORMAT_ISO_DATE } from '../../../shared/util/warehouse-util';
import { DividerModule } from 'primeng/divider';
import { ToastModule } from 'primeng/toast';

@Component({
  selector: 'jhi-balance-mvt-caisse',
  standalone: true,
  providers: [MessageService, DialogService, ConfirmationService],
  imports: [
    Button,
    CalendarModule,
    MultiSelectModule,
    PaginatorModule,
    ToolbarModule,
    TooltipModule,
    DecimalPipe,
    DatePipe,
    SelectButtonModule,
    CardModule,
    SplitButtonModule,
    RadioButtonModule,
    DividerModule,
    ToastModule,
  ],
  templateUrl: './balance-mvt-caisse.component.html',
})
export class BalanceMvtCaisseComponent implements OnInit, AfterViewInit {
  protected fromDate: Date | undefined;
  protected toDate: Date | undefined;
  protected loading = false;
  protected balanceMvtCaisseWrapper: BalanceCaisseWrapper | null = null;
  private mvtParamServiceService = inject(MvtParamServiceService);
  private messageService = inject(MessageService);
  private translate = inject(TranslateService);
  private balanceMvtCaisseService = inject(BalanceMvtCaisseService);
  private primeNGConfig = inject(PrimeNGConfig);

  ngAfterViewInit(): void {
    this.translate.use('fr');
    this.translate.stream('primeng').subscribe(data => {
      this.primeNGConfig.setTranslation(data);
    });
  }

  ngOnInit(): void {
    const params = this.mvtParamServiceService.mvtCaisseParam();
    if (params) {
      this.fromDate = params.fromDate;
      this.toDate = params.toDate;
    }
    this.onSearch();
  }

  onSearch(): void {
    this.loading = true;
    this.balanceMvtCaisseService
      .query({
        ...this.buildParams(),
      })
      .subscribe({
        next: (res: HttpResponse<BalanceCaisseWrapper>) => this.onSuccess(res.body),
        error: () => this.onError(),
      });
    this.updateParam();
  }

  onPrint(): void {
    this.balanceMvtCaisseService
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

  private setParam(): void {
    const param: MvtCaisseParams = {
      fromDate: this.fromDate,
      toDate: this.toDate,
    };
    this.mvtParamServiceService.setMvtCaisseParam(param);
  }

  private updateParam(): void {
    const params = this.mvtParamServiceService.mvtCaisseParam();
    if (params) {
      params.fromDate = this.fromDate;
      params.toDate = this.toDate;
      this.mvtParamServiceService.setMvtCaisseParam(params);
    } else {
      this.setParam();
    }
  }

  private buildParams(): any {
    return {
      fromDate: DATE_FORMAT_ISO_DATE(this.fromDate),
      toDate: DATE_FORMAT_ISO_DATE(this.toDate),
      statuts: ['CLOSED'],
    };
  }

  private onSuccess(data: BalanceCaisseWrapper | null): void {
    this.balanceMvtCaisseWrapper = data || null;
    this.loading = false;
  }

  private onError(): void {
    this.messageService.add({
      severity: 'error',
      summary: 'Error',
      detail: 'Une erreur est survenue',
    });
    this.balanceMvtCaisseWrapper = null;
    this.loading = false;
  }
}
