import { Component, DestroyRef, inject, OnInit, signal, ChangeDetectionStrategy } from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { CommonModule, DatePipe } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { HttpHeaders } from "@angular/common/http";
import { RouterLink } from "@angular/router";
import { Button } from "primeng/button";
import { TableLazyLoadEvent, TableModule } from "primeng/table";
import { Toolbar } from "primeng/toolbar";
import { DatePicker } from "primeng/datepicker";
import { TooltipModule } from "primeng/tooltip";
import { FloatLabel } from "primeng/floatlabel";
import { IconField } from "primeng/iconfield";
import { InputIcon } from "primeng/inputicon";
import { InputText } from "primeng/inputtext";
import { ButtonGroup } from "primeng/buttongroup";
import { ITEMS_PER_PAGE } from "../../../../shared/constants/pagination.constants";
import { ISales } from "../../../../shared/model";
import { VenteDepotApiService } from "../../data-access/services/vente-depot-api.service";
import { BlobDownloadService } from "../../../../shared/services/blob-download.service";
import { NotificationService } from "../../../../shared/services/notification.service";

@Component({
  selector: "app-vente-depot-list",
  templateUrl: "./vente-depot-list.component.html",
  styleUrl: "./vente-depot-list.component.scss",
  providers: [DatePipe],
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [
    CommonModule,
    FormsModule,
    Button,
    ButtonGroup,
    TableModule,
    Toolbar,
    DatePicker,
    InputText,
    TooltipModule,
    FloatLabel,
    IconField,
    InputIcon,
    RouterLink
  ]
})
export class VenteDepotListComponent implements OnInit {
  private readonly api = inject(VenteDepotApiService);
  private readonly blobDownload = inject(BlobDownloadService);
  private readonly notificationService = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly datePipe = inject(DatePipe);

  protected loading = signal(false);
  protected sales: ISales[] = [];
  protected totalItems = 0;
  protected page = 0;
  protected itemsPerPage = ITEMS_PER_PAGE;

  protected search = "";
  protected fromDate: Date = new Date();
  protected toDate: Date = new Date();

  get totalAmount(): number {
    return this.sales.reduce((sum, s) => sum + (s.salesAmount ?? 0), 0);
  }

  ngOnInit(): void {
    this.loadPage();
  }

  protected loadPage(page?: number): void {
    const pageToLoad = page ?? this.page;
    this.loading.set(true);
    this.api.query({
      page: pageToLoad,
      size: this.itemsPerPage,
      search: this.search || null,
      fromDate: this.datePipe.transform(this.fromDate, "yyyy-MM-dd"),
      toDate: this.datePipe.transform(this.toDate, "yyyy-MM-dd")
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          this.loading.set(false);
          this.onSuccess(res.body, res.headers, pageToLoad);
        },
        error: () => this.loading.set(false)
      });
  }

  private onSuccess(data: ISales[] | null, headers: HttpHeaders, page: number): void {
    this.totalItems = Number(headers.get("X-Total-Count"));
    this.page = page;
    this.sales = data || [];
  }

  protected lazyLoading(event: TableLazyLoadEvent): void {
    if (event.first != null && event.rows != null) {
      this.page = event.first / event.rows;
      this.itemsPerPage = event.rows;
      this.loadPage(this.page);
    }
  }

  protected onSearch(): void {
    this.loadPage(0);
  }

  protected printInvoice(sale: ISales): void {
    if (!sale.saleId) return;
    this.api.printInvoice(sale.saleId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(blob => {
        this.blobDownload.downloadPdf(blob, `facture-${sale.numberTransaction}`);

      });
  }
}
