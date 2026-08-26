import {
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  inject,
  input,
  OnInit,
  signal
} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {
  NgbDateStruct,
  NgbDropdown,
  NgbDropdownItem,
  NgbDropdownMenu,
  NgbDropdownToggle,
  NgbTooltip
} from '@ng-bootstrap/ng-bootstrap';
import {ITEMS_PER_PAGE} from '../../../shared/constants/pagination.constants';
import {ISales, SaleId} from '../../../shared/model/sales.model';
import {IUser} from '../../../core/user/user.model';
import {SalesService} from '../../sales/sales.service';
import {UserService} from '../../../core/user/user.service';
import {debounceTime, Subject} from 'rxjs';
import {TauriPrinterService} from '../../../shared/services/tauri-printer.service';
import {HttpHeaders, HttpResponse} from '@angular/common/http';
import {handleBlobForTauri} from '../../../shared/util/tauri-util';
import {IMagasin} from '../../../shared/model';
import {FormsModule} from '@angular/forms';
import {MagasinService} from '../../magasin/magasin.service';
import {RouterLink} from '@angular/router';
import {StockDepotService} from '../stock-depot/stock-depot.service';
import {NGB_DATE_TO_ISO} from '../../../shared/util/warehouse-util';
import {saveAs} from 'file-saver';
import {extractFileName2} from '../../../shared/util/file-utils';
import {CommonModule} from '@angular/common';
import {BlobDownloadService} from '../../../shared/services/blob-download.service';
import {NotificationService} from '../../../shared/services/notification.service';
import {
  AppTableLazyLoadEvent,
  ButtonComponent,
  DataTableComponent,
  SelectComponent,
  SkeletonComponent,
  ToolbarComponent,
} from '../../../shared/ui';
import {PharmaDatePickerComponent} from '../../../shared/date-picker/pharma-date-picker.component';

@Component({
  selector: 'app-achat-depot',
  imports: [
    CommonModule,
    ButtonComponent,
    PharmaDatePickerComponent,
    SelectComponent,
    SkeletonComponent,
    DataTableComponent,
    ToolbarComponent,
    NgbTooltip,
    FormsModule,
    RouterLink,
    NgbDropdown,
    NgbDropdownToggle,
    NgbDropdownMenu,
    NgbDropdownItem,
  ],
  templateUrl: './achat-depot.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './achat-depot.component.scss',
})
export class AchatDepotComponent implements OnInit {

  readonly navCode = input<string>('');

  protected readonly selectedDepot = signal<IMagasin | null>(null);
  protected readonly totalItems = signal(0);
  protected readonly loading = signal<boolean | undefined>(undefined);
  protected readonly page = signal(0);
  protected readonly itemsPerPage = signal(ITEMS_PER_PAGE);
  protected readonly sales = signal<ISales[]>([]);
  protected selectedEl?: ISales;
  protected users: IUser[] = [];
  protected selectedUserId: number | null;
  protected search = '';
  protected fromDate: NgbDateStruct = this.dateToNgbStruct(new Date());
  protected toDate: NgbDateStruct = this.dateToNgbStruct(new Date());
  protected readonly isLargeScreen = signal(true);
  protected readonly depots = signal<IMagasin[]>([]);
  /**
   * Options du sélecteur de dépôt, avec l'adresse ajoutée au libellé.
   *
   */
  protected readonly depotOptions = computed<(IMagasin & { displayLabel: string })[]>(() =>
    this.depots().map(depot => ({
      ...depot,
      displayLabel: depot.address ? `${depot.name} — ${depot.address}` : depot.name,
    })),
  );
  private readonly salesService = inject(SalesService);
  private readonly stockDepotService = inject(StockDepotService);
  private readonly userService = inject(UserService);
  private searchSubject = new Subject<void>();
  private readonly tauriPrinterService = inject(TauriPrinterService);
  private readonly magasinService = inject(MagasinService);
  private readonly blobDownloadService = inject(BlobDownloadService);
  private readonly notificationService = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);

  populate(): void {
    this.magasinService.fetchAllDepots().subscribe((res: HttpResponse<IMagasin[]>) => {
      this.depots.set(res.body || []);
    });
  }

  ngOnInit(): void {
    const width = window.innerWidth;
    if (width < 1800) {
      this.isLargeScreen.set(false);
    }
    this.loadAllUsers();
    this.populate();
    this.loadPage();

    this.searchSubject.pipe(debounceTime(300)).subscribe(() => {
      this.loadPage();
    });
  }

  loadAllUsers(): void {
    this.userService.query().subscribe((res: HttpResponse<IUser[]>) => {
      if (res.body) {
        this.users = [{id: null, abbrName: 'TOUT'}];
        this.users = [...this.users, ...res.body];
      }
    });
  }

  printReceiptForTauri(saleId: SaleId, isEdition = false): void {
    this.salesService
      .getEscPosReceiptForTauri(saleId, isEdition)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: async (escposData: ArrayBuffer) => {
          try {
            await this.tauriPrinterService.printEscPosFromBuffer(escposData);
          } catch {
            this.notificationService.error("Le ticket n'a pas pu être envoyé à l'imprimante.");
          }
        },
        error: () => this.notificationService.error("Le ticket n'a pas pu être imprimé."),
      });
  }

  protected onSelectDepot(): void {
    this.searchSubject.next();
  }


  protected onRowToggle(sale: ISales, table: DataTableComponent<ISales>): void {
    table.toggleRow(sale);
    if (!sale.saleId || (sale as ISales & { _loaded?: boolean })._loaded) {
      return;
    }
    this.stockDepotService
      .findSaleLines(sale.saleId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: salesLines => {
          const chargee = {...sale, salesLines, _loaded: true};
          this.sales.update(lignes => lignes.map(l => (l === sale ? chargee : l)));
        },
        error: () => this.notificationService.error("Le détail de la vente n'a pas pu être chargé.")
      });
  }

  protected loadPage(page?: number): void {
    const pageToLoad: number = page || this.page();
    this.fetchSales(pageToLoad, this.itemsPerPage());
  }

  protected lazyLoading(event: AppTableLazyLoadEvent): void {
    if (event) {
      this.page.set(event.first / event.rows);
      this.itemsPerPage.set(event.rows);
      this.fetchSales(this.page(), this.itemsPerPage());
    }
  }

  protected onSearch(): void {
    this.searchSubject.next();
  }

  protected exportWithFormat(format: string, sale: ISales): void {
    this.onExport(format, sale.saleId);
  }

  protected printInvoice(sale: ISales): void {
    this.salesService
      .printInvoice(sale.saleId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: blob => this.blobDownloadService.downloadPdf(blob, 'facture-client'),
        error: () => this.notificationService.error("La facture n'a pas pu être générée."),
      });
  }

  protected printSale(sale: ISales): void {
    if (this.tauriPrinterService.isRunningInTauri()) {
      this.printReceiptForTauri(sale.saleId, true);
      return;
    }
    this.salesService
      .rePrintReceipt(sale.saleId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        error: () => this.notificationService.error("Le ticket n'a pas pu être imprimé."),
      });
  }

  protected onSuccess(data: ISales[] | null, headers: HttpHeaders, page: number): void {
    this.totalItems.set(Number(headers.get('X-Total-Count')));
    this.page.set(page);
    this.sales.set(data || []);
    this.loading.set(false);
  }

  protected onError(): void {
    this.loading.set(false);
    this.notificationService.error('Une erreur est survenue. Veuillez réessayer.');
  }

  private fetchSales(page: number, size: number): void {
    this.loading.set(true);
    this.stockDepotService
      .fetchSales({
        page,
        size,
        ...this.buildCriteria(),
      })
      .subscribe({
        next: (res: HttpResponse<ISales[]>) => this.onSuccess(res.body, res.headers, page),
        error: () => this.onError(),
      });
  }

  private dateToNgbStruct(date: Date): NgbDateStruct {
    return {year: date.getFullYear(), month: date.getMonth() + 1, day: date.getDate()};
  }

  private buildCriteria(): any {
    return {
      search: this.search,
      fromDate: this.fromDate ? NGB_DATE_TO_ISO(this.fromDate) : null,
      toDate: this.toDate ? NGB_DATE_TO_ISO(this.toDate) : null,
      magasinId: this.selectedDepot()?.id ?? null,
      userId: this.selectedUserId,
    };
  }

  private onExport(format: string, saleId: SaleId): void {
    const exportObservable = format === 'excel' ? this.stockDepotService.exportToExcel(saleId) : this.stockDepotService.exportToCsv(saleId);

    exportObservable.subscribe({
      next: resp => {
        const blob = resp.body;
        if (!blob) {
          return;
        }

        const fileName = extractFileName2(
          resp.headers.get('Content-disposition'),
          format,
          `vente_depot_stock_${saleId.id}_${saleId.saleDate}`,
        );

        if (this.tauriPrinterService.isRunningInTauri()) {
          try {
            handleBlobForTauri(blob, fileName, format);
          } catch {
            this.notificationService.error("Le fichier n'a pas pu être enregistré.");
          }
        } else {
          saveAs(blob, fileName);
        }
      },
      error: () => this.onError(),
    });
  }
}
