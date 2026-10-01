import { ChangeDetectionStrategy, Component, DestroyRef, effect, inject, input, signal, untracked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { NgbDateStruct, NgbPagination, NgbTooltip } from '@ng-bootstrap/ng-bootstrap';
import { ButtonComponent, DataTableComponent, DetailGridComponent, DetailSectionComponent } from 'app/shared/ui';
import { PharmaDatePickerComponent } from 'app/shared/date-picker/pharma-date-picker.component';
import { ErrorService } from 'app/shared/error.service';
import { NotificationService } from 'app/shared/services/notification.service';
import { BlobDownloadService } from 'app/shared/services/blob-download.service';
import { NGB_DATE_OFFSET_MONTHS, NGB_DATE_TO_ISO } from 'app/shared/util/warehouse-util';
import { ISales } from 'app/shared/model/sales.model';
import { ISalesLine } from 'app/shared/model/sales-line.model';
import { CustomerService } from '../customer.service';
import { SalesService } from '../../sales/sales.service';

/** Onglet « Achats » de la fiche client : historique des ventes clôturées et détail de la facture sélectionnée. */
@Component({
  selector: 'app-customer-achats-tab',
  templateUrl: './customer-achats-tab.component.html',
  styleUrls: ['./customer-achats-tab.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, FormsModule, NgbPagination, NgbTooltip, ButtonComponent, DataTableComponent, DetailGridComponent, DetailSectionComponent, PharmaDatePickerComponent],
})
export class CustomerAchatsTabComponent {
  readonly customerId = input.required<number>();
  readonly customerCode = input<string | undefined>(undefined);

  protected readonly pageSize = 10;
  protected readonly sales = signal<ISales[]>([]);
  protected readonly selectedRowIndex = signal<number | undefined>(undefined);
  protected readonly selectedRowSaleLines = signal<ISalesLine[]>([]);
  protected readonly saleSelected = signal<ISales | undefined>(undefined);
  // Douze derniers mois par défaut : charger tout l'historique d'un client fidèle était lent.
  protected readonly fromDate = signal<NgbDateStruct>(NGB_DATE_OFFSET_MONTHS(-12));
  protected readonly toDate = signal<NgbDateStruct>(NGB_DATE_OFFSET_MONTHS(0));
  protected readonly page = signal(1);
  protected readonly totalSales = signal(0);

  private readonly customerService = inject(CustomerService);
  private readonly salesService = inject(SalesService);
  private readonly notificationService = inject(NotificationService);
  private readonly blobDownloadService = inject(BlobDownloadService);
  private readonly errorService = inject(ErrorService);
  private readonly destroyRef = inject(DestroyRef);

  constructor() {
    effect(() => {
      const id = this.customerId();
      untracked(() => this.loadSales(id));
    });
  }

  protected searchSales(): void {
    this.page.set(1);
    this.saleSelected.set(undefined);
    this.loadSales(this.customerId());
  }

  protected changePage(page: number): void {
    this.page.set(page);
    this.loadSales(this.customerId());
  }

  protected clickRow(item: ISales): void {
    this.selectedRowIndex.set(item.id);
    this.selectedRowSaleLines.set(item.salesLines);
    this.saleSelected.set(item);
  }

  protected print(): void {
    const sale = this.saleSelected();
    if (sale) {
      this.salesService
        .print(sale.saleId)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: blob => this.blobDownloadService.downloadPdf(blob, `facture-${sale.numberTransaction}`),
          error: this.signaler("La facture n'a pas pu être imprimée"),
        });
    }
  }

  /** Attestation de dépenses sur la période des achats. */
  protected imprimerAttestation(): void {
    this.customerService
      .attestationDepensesPdf(this.customerId(), NGB_DATE_TO_ISO(this.fromDate()), NGB_DATE_TO_ISO(this.toDate()))
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: blob => this.blobDownloadService.downloadPdf(blob, `attestation-depenses-${this.customerCode() ?? this.customerId()}`),
        error: this.signaler("L'attestation n'a pas pu être éditée"),
      });
  }

  private loadSales(customerId: number): void {
    this.customerService
      .purchases({
        customerId,
        fromDate: NGB_DATE_TO_ISO(this.fromDate()),
        toDate: NGB_DATE_TO_ISO(this.toDate()),
        page: this.page() - 1,
        size: this.pageSize,
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res: HttpResponse<ISales[]>) => {
          this.totalSales.set(Number(res.headers.get('X-Total-Count') ?? 0));
          this.sales.set(res.body ?? []);
        },
        error: this.signaler("Les achats n'ont pas pu être chargés"),
      });
  }

  private signaler(action: string): (err: HttpErrorResponse) => void {
    return err => this.notificationService.error(this.errorService.getErrorMessage(err, action));
  }
}

