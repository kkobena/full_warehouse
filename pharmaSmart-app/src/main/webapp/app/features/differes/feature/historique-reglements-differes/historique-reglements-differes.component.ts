import {ChangeDetectionStrategy, Component, DestroyRef, inject, OnInit, input, signal } from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {finalize} from 'rxjs/operators';
import {HttpHeaders} from '@angular/common/http';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {NgbDateStruct, NgbTooltip} from '@ng-bootstrap/ng-bootstrap';
import {
  AppTableLazyLoadEvent,
  ButtonComponent,
  DataTableComponent,
  RowTogglerDirective,
  SelectComponent,
  ToolbarComponent
} from '../../../../shared/ui';
import {
  PharmaDatePickerComponent
} from '../../../../shared/date-picker/pharma-date-picker.component';

import {NotificationService} from '../../../../shared/services/notification.service';
import {ErrorService} from '../../../../shared/error.service';
import {TauriPrinterService} from '../../../../shared/services/tauri-printer.service';
import {ITEMS_PER_PAGE} from '../../../../shared/constants/pagination.constants';
import {
  DATE_FORMAT_ISO_DATE,
  NGB_DATE_TO_ISO,
  TODAY_NGB_DATE
} from '../../../../shared/util/warehouse-util';

import {DiffereApiService} from '../../data-access/services/differe-api.service';
import {DiffereStore} from '../../data-access/store/differe.store';
import {
  IDiffereSearchParams,
  IPaymentIdDiffere,
  IReglementDiffere,
  IReglementDiffereItem,
} from '../../data-access/models';
import {BlobDownloadService} from "../../../../shared/services/blob-download.service";

import { DeviseDirective } from 'app/shared/utils/devise';
@Component({
  selector: 'app-historique-reglements-differes',
  imports: [DeviseDirective, 
    CommonModule,
    FormsModule,
    ButtonComponent,
    DataTableComponent,
    PharmaDatePickerComponent,
    SelectComponent,
    ToolbarComponent,
    RowTogglerDirective,
    NgbTooltip
  ],
  templateUrl: './historique-reglements-differes.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './historique-reglements-differes.component.scss',
})
export class HistoriqueReglementsDifferesComponent implements OnInit {
  /**
   * Code de l'entrée de navigation dont cet écran est le contenu.
   *
   * <p>Fourni par le layout : le titre de la barre suit le libellé du menu — ou son `titre_long`
   * quand la barre nomme plus longuement. Un écran atteint depuis deux menus affiche donc le nom
   * de celui par lequel on est entré.
   */
  readonly navCode = input<string>('');

  protected readonly loading = signal(false);
  protected readonly loadingBtn = signal(false);
  protected readonly loadingPdf = signal(false);
  protected readonly page = signal(0);
  protected readonly itemsPerPage = ITEMS_PER_PAGE;
  protected readonly modelStartDate = signal<NgbDateStruct | undefined>(undefined);
  protected modelEndDate: NgbDateStruct = TODAY_NGB_DATE();
  protected customerId: number | null = null;

  protected readonly store = inject(DiffereStore);
  private readonly differeApiService = inject(DiffereApiService);
  private readonly notificationService = inject(NotificationService);
  private readonly errorService = inject(ErrorService);
  private readonly tauriPrinterService = inject(TauriPrinterService);
  private readonly blobDownload = inject(BlobDownloadService);
  private readonly destroyRef = inject(DestroyRef);

  constructor() {
    const d = new Date();
    d.setMonth(d.getMonth() - 1);
    this.modelStartDate.set({year: d.getFullYear(), month: d.getMonth() + 1, day: d.getDate()});
  }

  ngOnInit(): void {
    if (!this.store.clientsLoaded()) {
      this.loadClients();
    }
    this.onSearch();
  }

  onSearch(): void {
    this.page.set(0);
    this.loadPage();
    this.loadSummary();
  }

  lazyLoading(event: AppTableLazyLoadEvent): void {
    if (event) {
      this.page.set(Math.floor(event.first / event.rows));
      this.loadPage(event.rows);
    }
  }

  exportPdf(): void {
    this.loadingPdf.set(true);
    this.differeApiService
      .exportReglementsToPdf(this.buildParams())
      .pipe(
        finalize(() => (this.loadingPdf.set(false))),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: blob => {
          this.blobDownload.downloadPdf(blob, "reglements-differes");

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
            } catch {
            }
          },
        });
    } else {
      this.differeApiService
        .printReceipt(paymentId)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe();
    }
  }

  private loadPage(rows = this.itemsPerPage): void {
    this.loading.set(true);
    this.loadingBtn.set(true);
    this.store.setLoadingReglements(true);
    this.differeApiService
      .getReglementsDifferes({...this.buildParams(), page: this.page(), size: rows})
      .pipe(
        finalize(() => {
          this.loading.set(false);
          this.loadingBtn.set(false);
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
      fromDate: NGB_DATE_TO_ISO(this.modelStartDate()),
      toDate: NGB_DATE_TO_ISO(this.modelEndDate),
    };
    if (this.customerId) {
      params.customerId = this.customerId;
    }
    return params;
  }
}
