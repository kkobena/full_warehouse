import {ChangeDetectionStrategy, Component, inject, OnInit} from '@angular/core';
import {MvtParamServiceService} from '../mvt-param-service.service';
import {BalanceCaisseWrapper} from './balance-caisse.model';
import {BalanceMvtCaisseService} from './balance-mvt-caisse.service';
import {HttpResponse} from '@angular/common/http';
import {MvtCaisseParams} from '../mvt-caisse-util';
import {NGB_DATE_TO_ISO} from '../../../shared/util/warehouse-util';
import {FormsModule} from '@angular/forms';
import {NotificationService} from '../../../shared/services/notification.service';
import {finalize} from 'rxjs/operators';
import {TauriPrinterService} from '../../../shared/services/tauri-printer.service';
import {handleBlobForTauri} from '../../../shared/util/tauri-util';
import {CommonModule} from "@angular/common";
import {NgbDateStruct, NgbTooltip} from '@ng-bootstrap/ng-bootstrap';
import {ButtonComponent, ToolbarComponent} from '../../../shared/ui';
import {PharmaDatePickerComponent} from '../../../shared/date-picker/pharma-date-picker.component';

@Component({
  selector: 'app-balance-mvt-caisse',
  imports: [
    CommonModule,
    FormsModule,
    ButtonComponent,
    ToolbarComponent,
    PharmaDatePickerComponent,
    NgbTooltip,
  ],
  templateUrl: './balance-mvt-caisse.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrls: ['./balance-mvt-caisse.component.scss'],
})
export class BalanceMvtCaisseComponent implements OnInit {
  protected fromDate: NgbDateStruct | null = null;
  protected toDate: NgbDateStruct | null = null;
  protected loading = false;
  protected balanceMvtCaisseWrapper: BalanceCaisseWrapper | null = null;
  private mvtParamServiceService = inject(MvtParamServiceService);
  private balanceMvtCaisseService = inject(BalanceMvtCaisseService);
  private readonly tauriPrinterService = inject(TauriPrinterService);
  private readonly notificationService = inject(NotificationService);

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
          this.notificationService.error('Erreur', "Une erreur est survenue lors de l'export PDF");
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
      fromDate: this.fromDate ? NGB_DATE_TO_ISO(this.fromDate) : null,
      toDate: this.toDate ? NGB_DATE_TO_ISO(this.toDate) : null,
      statuts: ['CLOSED'],
    };
  }

  private onSuccess(data: BalanceCaisseWrapper | null): void {
    this.balanceMvtCaisseWrapper = data || null;
    this.loading = false;
  }

  private onError(): void {
    this.notificationService.error('Une erreur est survenue lors de la récupération des données');
    this.balanceMvtCaisseWrapper = null;
    this.loading = false;
  }
}
