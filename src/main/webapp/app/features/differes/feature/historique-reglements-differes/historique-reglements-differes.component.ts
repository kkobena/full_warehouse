import { Component, DestroyRef, inject, OnInit } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs/operators';
import { HttpHeaders } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { SelectModule } from 'primeng/select';
import { TooltipModule } from 'primeng/tooltip';
import { FloatLabel } from 'primeng/floatlabel';
import { DatePickerModule } from 'primeng/datepicker';
import { Toolbar } from 'primeng/toolbar';

import { NotificationService } from '../../../../shared/services/notification.service';
import { ErrorService } from '../../../../shared/error.service';
import { TauriPrinterService } from '../../../../shared/services/tauri-printer.service';
import { handleBlobForTauri } from '../../../../shared/util/tauri-util';
import { ITEMS_PER_PAGE } from '../../../../shared/constants/pagination.constants';
import { DATE_FORMAT_ISO_DATE } from '../../../../shared/util/warehouse-util';

import { DiffereApiService } from '../../data-access/services/differe-api.service';
import { DiffereStore } from '../../data-access/store/differe.store';
import {
  IDiffereSearchParams,
  IPaymentIdDiffere,
  IReglementDiffere,
  IReglementDiffereItem,
} from '../../data-access/models';

@Component({
  selector: 'app-historique-reglements-differes',
  imports: [
    CommonModule,
    FormsModule,
    TableModule,
    ButtonModule,
    SelectModule,
    TooltipModule,
    FloatLabel,
    DatePickerModule,
    Toolbar,
  ],
  templateUrl: './historique-reglements-differes.component.html',
  styleUrl: './historique-reglements-differes.component.scss',
})
export class HistoriqueReglementsDifferesComponent implements OnInit {
  protected loading = false;
  protected loadingBtn = false;
  protected loadingPdf = false;
  protected page = 0;
  protected readonly itemsPerPage = ITEMS_PER_PAGE;
  protected expandedRows: Record<string, boolean> = {};
  protected modelStartDate: Date;
  protected modelEndDate: Date = new Date();
  protected customerId: number | null = null;

  protected readonly store = inject(DiffereStore);
  private readonly differeApiService = inject(DiffereApiService);
  private readonly notificationService = inject(NotificationService);
  private readonly errorService = inject(ErrorService);
  private readonly tauriPrinterService = inject(TauriPrinterService);
  private readonly destroyRef = inject(DestroyRef);

  constructor() {
    const d = new Date();
    d.setMonth(d.getMonth() - 1);
    this.modelStartDate = d;
  }

  ngOnInit(): void {
    if (!this.store.clientsLoaded()) {
      this.loadClients();
    }
    this.onSearch();
  }

  onSearch(): void {
    this.page = 0;
    this.loadPage();
    this.loadSummary();
  }

  lazyLoading(event: TableLazyLoadEvent): void {
    if (event) {
      this.page = Math.floor(event.first / event.rows);
      this.loadPage(event.rows);
    }
  }

  exportPdf(): void {
    this.loadingPdf = true;
    this.differeApiService
      .exportReglementsToPdf(this.buildParams())
      .pipe(
        finalize(() => (this.loadingPdf = false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: blob => {
          if (this.tauriPrinterService.isRunningInTauri()) {
            handleBlobForTauri(blob, 'reglements-differes');
          } else {
            window.open(URL.createObjectURL(blob));
          }
        },
        error: err =>
          this.notificationService.error(this.errorService.getErrorMessage(err), 'Export PDF'),
      });
  }

  printItem(item: IReglementDiffereItem): void {
    const paymentId: IPaymentIdDiffere = {
      id: item.id,
      transactionDate: DATE_FORMAT_ISO_DATE(new Date(item.mvtDate as any)),
    };
    if (this.tauriPrinterService.isRunningInTauri()) {
      this.differeApiService
        .getEscPosReceiptForTauri(paymentId)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: async (data: ArrayBuffer) => {
            try {
              await this.tauriPrinterService.printEscPosFromBuffer(data);
            } catch {}
          },
        });
    } else {
      this.differeApiService
        .printReceipt(paymentId)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe();
    }
  }

  toggleRow(reglement: IReglementDiffere): void {
    const key = String(reglement.id);
    if (this.expandedRows[key]) {
      delete this.expandedRows[key];
    } else {
      this.expandedRows[key] = true;
    }
    this.expandedRows = { ...this.expandedRows };
  }

  isExpanded(reglement: IReglementDiffere): boolean {
    return !!this.expandedRows[String(reglement.id)];
  }

  private loadPage(rows = this.itemsPerPage): void {
    this.loading = true;
    this.loadingBtn = true;
    this.store.setLoadingReglements(true);
    this.differeApiService
      .getReglementsDifferes({ ...this.buildParams(), page: this.page, size: rows })
      .pipe(
        finalize(() => {
          this.loading = false;
          this.loadingBtn = false;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: res => this.onSuccess(res.body, res.headers),
        error: err => {
          this.store.setLoadingReglements(false);
          this.notificationService.error(this.errorService.getErrorMessage(err), 'Chargement historique');
        },
      });
  }

  private onSuccess(data: IReglementDiffere[] | null, headers: HttpHeaders): void {
    const total = Number(headers.get('X-Total-Count'));
    this.store.setReglements(data ?? [], total);
  }

  private loadSummary(): void {
    this.differeApiService
      .getReglementDiffereSummary(this.buildParams())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => this.store.setReglementSummary(res.body),
        error: () => this.store.setReglementSummary(null),
      });
  }

  private loadClients(): void {
    this.differeApiService
      .findClients()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => this.store.setClients(res.body ?? []),
        error: () => this.store.setClients([]),
      });
  }

  private buildParams(): IDiffereSearchParams {
    const params: IDiffereSearchParams = {
      fromDate: DATE_FORMAT_ISO_DATE(this.modelStartDate),
      toDate: DATE_FORMAT_ISO_DATE(this.modelEndDate),
    };
    if (this.customerId) {
      params.customerId = this.customerId;
    }
    return params;
  }
}
