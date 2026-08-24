import { Component, DestroyRef, inject, OnInit, signal, ChangeDetectionStrategy, input} from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { CommonModule } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { HttpHeaders } from "@angular/common/http";
import { RouterLink } from "@angular/router";
import {
  NgbDateParserFormatter,
  NgbDateStruct,
  NgbDropdown,
  NgbDropdownItem,
  NgbDropdownMenu,
  NgbDropdownToggle,
  NgbTooltip
} from "@ng-bootstrap/ng-bootstrap";

import { FrenchDateParserFormatter } from "../../../../config/french-date-parser-formatter";
import { PharmaDatePickerComponent } from "../../../../shared/date-picker/pharma-date-picker.component";
import {
  AppTableLazyLoadEvent,
  ButtonComponent,
  DataTableComponent,
  IconFieldComponent,
  SkeletonComponent,
  ToolbarComponent
} from "../../../../shared/ui";
import { ITEMS_PER_PAGE } from "../../../../shared/constants/pagination.constants";
import { ISales } from "../../../../shared/model";
import { SaleId } from "../../../../shared/model/sales.model";
import { VenteDepotApiService } from "../../data-access/services/vente-depot-api.service";
import { SalesApiService } from "../../data-access/services/sales-api.service";
import { TauriPrinterService } from "../../../../shared/services/tauri-printer.service";
import { NotificationService } from "../../../../shared/services/notification.service";
import { BlobDownloadService } from "../../../../shared/services/blob-download.service";

import { DeviseDirective } from 'app/shared/utils/devise';
@Component({
  selector: "app-vente-depot-list",
  templateUrl: "./vente-depot-list.component.html",
  styleUrl: "./vente-depot-list.component.scss",
  providers: [{ provide: NgbDateParserFormatter, useClass: FrenchDateParserFormatter }],
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DeviseDirective,
    CommonModule,
    FormsModule,
    ButtonComponent,
    DataTableComponent,
    ToolbarComponent,
    PharmaDatePickerComponent,
    IconFieldComponent,
    SkeletonComponent,
    NgbTooltip,
    NgbDropdown,
    NgbDropdownToggle,
    NgbDropdownMenu,
    NgbDropdownItem,
    RouterLink
  ]
})
export class VenteDepotListComponent implements OnInit {

  readonly navCode = input<string>('');

  private readonly api = inject(VenteDepotApiService);
  private readonly blobDownload = inject(BlobDownloadService);
  private readonly salesApi = inject(SalesApiService);
  private readonly tauriPrinterService = inject(TauriPrinterService);
  private readonly notificationService = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);

  protected loading = signal(false);
  protected readonly sales = signal<ISales[]>([]);
  protected readonly totalItems = signal(0);
  protected readonly page = signal(0);
  protected readonly itemsPerPage = signal(ITEMS_PER_PAGE);

  protected search = "";
  protected fromDate: NgbDateStruct = this.todayNgb();
  protected toDate: NgbDateStruct = this.todayNgb();

  get totalAmount(): number {
    return this.sales().reduce((sum, s) => sum + (s.salesAmount ?? 0), 0);
  }

  ngOnInit(): void {
    this.loadPage();
  }

  protected loadPage(page?: number): void {
    const pageToLoad = page ?? this.page();
    this.loading.set(true);
    this.api.query({
      page: pageToLoad,
      size: this.itemsPerPage(),
      search: this.search || null,
      fromDate: this.ngbDateToIso(this.fromDate),
      toDate: this.ngbDateToIso(this.toDate)
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
    this.totalItems.set(Number(headers.get("X-Total-Count")));
    this.page.set(page);
    this.sales.set(data || []);
  }

  protected lazyLoading(event: AppTableLazyLoadEvent): void {
    if (event.first != null && event.rows != null) {
      this.page.set(event.first / event.rows);
      this.itemsPerPage.set(event.rows);
      this.loadPage(this.page());
    }
  }

  protected onSearch(): void {
    this.loadPage(0);
  }

  /**
   * Déplie ou replie la ligne, et charge son détail au premier dépliage.
   *
   * <p>`/api/stock-depots/sales` ne renvoie plus les lignes — la colonne « Articles » lit
   * `itemCount`. La vente est remplacée dans le signal plutôt que mutée : la table est `OnPush`,
   * et le dépliage est mémorisé par la clé `dataKey="id"`, que la copie conserve.
   */
  protected onRowToggle(sale: ISales, table: DataTableComponent<ISales>): void {
    table.toggleRow(sale);
    if (!sale.saleId || (sale as ISales & { _loaded?: boolean })._loaded) {
      return;
    }
    this.api
      .findSaleLines(sale.saleId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(salesLines => {
        const chargee = { ...sale, salesLines, _loaded: true };
        this.sales.update(lignes => lignes.map(l => (l === sale ? chargee : l)));
      });
  }

  /**
   * Export tableur de la vente. Le PDF passe par {@link printInvoice} : c'est la même facture.
   *
   * <p>`downloadFromObservable` choisit seul entre téléchargement navigateur et boîte
   * « Enregistrer sous » de Tauri.
   */
  protected exportWithFormat(format: 'excel' | 'csv', sale: ISales): void {
    if (!sale.saleId) {
      return;
    }
    const source$ = format === 'excel' ? this.api.exportToExcel(sale.saleId) : this.api.exportToCsv(sale.saleId);
    this.blobDownload.downloadFromObservable(source$, `vente-depot-${sale.numberTransaction}`, format);
  }

  /**
   * Réimprime le ticket de caisse, comme sur l'écran « Achats dépôts ».
   *
   * <p>Sous Tauri l'impression est locale : le serveur renvoie la trame ESC/POS brute, que le
   * côté Rust pousse vers l'imprimante thermique. Dans un navigateur, c'est le serveur qui
   * imprime, sur l'imprimante configurée pour le poste.
   */
  protected printSale(sale: ISales): void {
    if (!sale.saleId) {
      return;
    }
    if (this.tauriPrinterService.isRunningInTauri()) {
      this.printReceiptForTauri(sale.saleId);
      return;
    }
    this.salesApi
      .reprintReceiptComptant(sale.saleId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({ error: () => this.notificationService.error("Le ticket n'a pas pu être imprimé.") });
  }

  private printReceiptForTauri(saleId: SaleId): void {
    this.salesApi
      .getEscPosReceiptForTauri(saleId, true)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: async escposData => {
          try {
            await this.tauriPrinterService.printEscPosFromBuffer(escposData);
          } catch {
            this.notificationService.error("Le ticket n'a pas pu être envoyé à l'imprimante.");
          }
        },
        error: () => this.notificationService.error("Le ticket n'a pas pu être imprimé.")
      });
  }

  protected printInvoice(sale: ISales): void {
    if (!sale.saleId) return;
    this.api.printInvoice(sale.saleId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(blob => {
        this.blobDownload.downloadPdf(blob, `facture-${sale.numberTransaction}`);

      });
  }

  private todayNgb(): NgbDateStruct {
    const d = new Date();
    return { year: d.getFullYear(), month: d.getMonth() + 1, day: d.getDate() };
  }

  private ngbDateToIso(date: NgbDateStruct | null): string | null {
    if (!date) return null;
    return `${date.year}-${String(date.month).padStart(2, "0")}-${String(date.day).padStart(2, "0")}`;
  }
}
