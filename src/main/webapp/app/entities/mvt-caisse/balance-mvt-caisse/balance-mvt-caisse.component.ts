import { AfterViewInit, Component, inject, OnInit, viewChild } from '@angular/core';
import { MvtParamServiceService } from '../mvt-param-service.service';
import { BalanceCaisseWrapper } from './balance-caisse.model';
import { BalanceMvtCaisseService } from './balance-mvt-caisse.service';
import { TranslateService } from '@ngx-translate/core';
import { Button } from 'primeng/button';
import { MultiSelectModule } from 'primeng/multiselect';
import { PaginatorModule } from 'primeng/paginator';
import { ToolbarModule } from 'primeng/toolbar';
import { TooltipModule } from 'primeng/tooltip';
import { DecimalPipe } from '@angular/common';
import { SelectButtonModule } from 'primeng/selectbutton';
import { CardModule } from 'primeng/card';
import { SplitButtonModule } from 'primeng/splitbutton';
import { RadioButtonModule } from 'primeng/radiobutton';
import { HttpResponse } from '@angular/common/http';
import { MvtCaisseParams } from '../mvt-caisse-util';
import { DATE_FORMAT_ISO_DATE } from '../../../shared/util/warehouse-util';
import { DividerModule } from 'primeng/divider';
import { FormsModule } from '@angular/forms';
import { PrimeNG } from 'primeng/config';
import { DatePicker } from 'primeng/datepicker';
import { FloatLabel } from 'primeng/floatlabel';
import { ToastAlertComponent } from '../../../shared/toast-alert/toast-alert.component';
import { finalize } from 'rxjs/operators';
import { TauriPrinterService } from '../../../shared/services/tauri-printer.service';
import { handleBlobForTauri } from '../../../shared/util/tauri-util';

@Component({
  selector: 'jhi-balance-mvt-caisse',
  imports: [
    Button,
    MultiSelectModule,
    PaginatorModule,
    ToolbarModule,
    TooltipModule,
    DecimalPipe,
    SelectButtonModule,
    CardModule,
    SplitButtonModule,
    RadioButtonModule,
    DividerModule,
    FormsModule,
    DatePicker,
    FloatLabel,
    ToastAlertComponent,
  ],
  templateUrl: './balance-mvt-caisse.component.html',
  styleUrls: ['./balance-mvt-caisse.component.scss'],
})
export class BalanceMvtCaisseComponent implements OnInit, AfterViewInit {
  protected fromDate: Date | undefined;
  protected toDate: Date | undefined;
  protected loading = false;
  protected balanceMvtCaisseWrapper: BalanceCaisseWrapper | null = null;
  private mvtParamServiceService = inject(MvtParamServiceService);
  private translate = inject(TranslateService);
  private balanceMvtCaisseService = inject(BalanceMvtCaisseService);
  private readonly tauriPrinterService = inject(TauriPrinterService);
  private primeNGConfig = inject(PrimeNG);
  private readonly alert = viewChild.required<ToastAlertComponent>('alert');
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
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: blob => {
          if (this.tauriPrinterService.isRunningInTauri()) {
            handleBlobForTauri(blob, 'balance_vente_caisse');
          } else {
            window.open(URL.createObjectURL(blob));
          }
        },
        error: () => {
          this.alert().showError('Erreur', "Une erreur est survenue lors de l'export PDF");
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
    this.alert().showError('Une erreur est survenue lors de la récupération des données');
    this.balanceMvtCaisseWrapper = null;
    this.loading = false;
  }
}
