import { Component, inject, OnInit, viewChild } from '@angular/core';
import { Button } from 'primeng/button';
import { ConfirmDialogComponent } from '../../../shared/dialog/confirm-dialog/confirm-dialog.component';
import { DatePicker } from 'primeng/datepicker';
import { DecimalPipe } from '@angular/common';
import { FloatLabel } from 'primeng/floatlabel';
import { Select } from 'primeng/select';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { Toolbar } from 'primeng/toolbar';
import { Tooltip } from 'primeng/tooltip';
import { ITEMS_PER_PAGE } from '../../../shared/constants/pagination.constants';
import { ISales, SaleId } from '../../../shared/model/sales.model';
import { IUser } from '../../../core/user/user.model';
import { MenuItem } from 'primeng/api';
import { TranslateService } from '@ngx-translate/core';
import { PrimeNG } from 'primeng/config';
import { SalesService } from '../../sales/sales.service';
import { UserService } from '../../../core/user/user.service';
import { debounceTime, Subject } from 'rxjs';
import { TauriPrinterService } from '../../../shared/services/tauri-printer.service';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { handleBlobForTauri } from '../../../shared/util/tauri-util';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { IMagasin } from '../../../shared/model/magasin.model';
import { FormsModule } from '@angular/forms';
import { MagasinService } from '../../magasin/magasin.service';
import { RouterLink } from '@angular/router';
import { StockDepotService } from '../stock-depot/stock-depot.service';
import { DATE_FORMAT_ISO_DATE } from '../../../shared/util/warehouse-util';
import { saveAs } from 'file-saver';
import { extractFileName2 } from '../../../shared/util/file-utils';
import { Menu } from 'primeng/menu';

@Component({
  selector: 'jhi-achat-depot',
  imports: [
    Button,
    WarehouseCommonModule,
    ConfirmDialogComponent,
    DatePicker,
    DecimalPipe,
    FloatLabel,
    Select,
    TableModule,
    Toolbar,
    Tooltip,
    FormsModule,
    RouterLink,
    Menu
  ],
  templateUrl: './achat-depot.component.html',
  styleUrl: './achat-depot.component.scss'
})
export class AchatDepotComponent implements OnInit {
  protected selectedDepot: IMagasin | null = null;
  protected totalItems = 0;
  protected loading!: boolean;
  protected page = 0;
  protected itemsPerPage = ITEMS_PER_PAGE;
  protected sales: ISales[] = [];
  protected selectedEl?: ISales;
  protected users: IUser[] = [];
  protected selectedUserId: number | null;
  protected search = '';
  protected fromDate: Date = new Date();
  protected toDate: Date = new Date();
  protected isLargeScreen = true;
  protected splitbuttons: MenuItem[];
  protected depots: IMagasin[] = [];
  protected actions: MenuItem[] | undefined;
  protected exportMenuItems: MenuItem[] = [];
  protected currentSaleForExport?: ISales;

  private readonly translate = inject(TranslateService);
  private readonly primeNGConfig = inject(PrimeNG);
  private readonly salesService = inject(SalesService);
  private readonly stockDepotService = inject(StockDepotService);
  private readonly userService = inject(UserService);

  private searchSubject = new Subject<void>();

  private readonly exportMenu = viewChild.required<Menu>('exportMenu');
  private readonly tauriPrinterService = inject(TauriPrinterService);
  private readonly magasinService = inject(MagasinService);

  constructor() {
    this.translate.use('fr');
    this.translate.stream('primeng').subscribe(data => {
      this.primeNGConfig.setTranslation(data);
    });
    this.splitbuttons = [
      {
        label: 'Fiche à partir csv',
        icon: 'pi pi-file-pdf',
        command: () => console.error('print all record')
      }
    ];

    this.exportMenuItems = [
      {
        label: 'Exporter en CSV',
        icon: 'pi pi-file',
        command: () => this.exportWithFormat('CSV')
      },
      {
        label: 'Exporter en Excel',
        icon: 'pi pi-file-excel',
        command: () => this.exportWithFormat('EXCEL')
      },
      {
        label: 'Exporter en PDF',
        icon: 'pi pi-file-pdf',
        command: () => this.print()
      }
    ];
  }


  protected onSelectDepot(): void {
    this.searchSubject.next();
  }

  populate(): void {
    this.magasinService.fetchAllDepots().subscribe((res: HttpResponse<IMagasin[]>) => {
      this.depots = res.body || [];

    });
  }

  ngOnInit(): void {
    const width = window.innerWidth;
    if (width < 1800) {
      this.isLargeScreen = false;
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
        this.users = [{ id: null, abbrName: 'TOUT' }];
        this.users = [...this.users, ...res.body];
      }
    });
  }

  onSelectUser(evt: { value: number | null }): void {
    this.selectedUserId = evt.value;
    this.searchSubject.next();
  }


  printReceiptForTauri(saleId: SaleId, isEdition: boolean = false): void {
    this.salesService.getEscPosReceiptForTauri(saleId, isEdition).subscribe({
      next: async (escposData: ArrayBuffer) => {
        try {
          await this.tauriPrinterService.printEscPosFromBuffer(escposData);
        } catch (error) {
        }
      },
      error: () => {
      }
    });
  }


  protected loadPage(page?: number): void {
    const pageToLoad: number = page || this.page;
    this.fetchSales(pageToLoad, this.itemsPerPage);

  }

  protected lazyLoading(event: TableLazyLoadEvent): void {
    if (event) {
      this.page = event.first / event.rows;
      this.itemsPerPage = event.rows;
      this.fetchSales(this.page, this.itemsPerPage);
    }
  }

  protected onSearch(): void {
    this.searchSubject.next();
  }


  protected onExportMenu(event: Event, sale: ISales): void {
    this.currentSaleForExport = sale;
    this.exportMenu().toggle(event);
  }

  protected exportWithFormat(format: string): void {
    if (this.currentSaleForExport) {
      this.onExport(format, this.currentSaleForExport.saleId);
    }
  }

  protected print(): void {
    this.salesService.printInvoice(this.currentSaleForExport.saleId).subscribe(blob => {
      if (this.tauriPrinterService.isRunningInTauri()) {
        handleBlobForTauri(blob, 'facture-client');
      } else {
        window.open(URL.createObjectURL(blob));
      }
    });
  }

  protected printSale(sale: ISales): void {
    if (this.tauriPrinterService.isRunningInTauri()) {
      this.printReceiptForTauri(sale.saleId, true);
    } else {
      this.salesService.rePrintReceipt(sale.saleId).subscribe();
    }
  }


  protected onSuccess(data: ISales[] | null, headers: HttpHeaders, page: number): void {
    this.totalItems = Number(headers.get('X-Total-Count'));
    this.page = page;
    this.sales = data || [];
    this.loading = false;
  }

  protected onError(): void {
    this.loading = false;
  }

  private fetchSales(page: number, size: number): void {
    this.loading = true;
    this.stockDepotService
      .fetchSales({
        page,
        size,
        ...this.buildCriteria()
      })
      .subscribe({
        next: (res: HttpResponse<ISales[]>) => this.onSuccess(res.body, res.headers, page),
        error: () => this.onError()
      });
  }

  private buildCriteria(): any {
    return {
      search: this.search,
      fromDate: this.fromDate ? DATE_FORMAT_ISO_DATE(this.fromDate) : null,
      toDate: this.toDate ? DATE_FORMAT_ISO_DATE(this.toDate) : null,
      magasinId: this.selectedDepot ? this.selectedDepot.id : null,
      userId: this.selectedUserId
    };
  }

  private async onExport(format: string, saleId: SaleId): Promise<void> {
    this.stockDepotService.export(format, saleId).subscribe({
      next: async resp => {
        const blob = resp.body;
        if (!blob) {

          return;
        }

        const fileName = extractFileName2(
          resp.headers.get('Content-disposition'),
          format,
          `vente_depot_stock_${saleId.id}_${saleId.saleDate}`
        );

        if (this.tauriPrinterService.isRunningInTauri()) {
          // Tauri version - save file using dialog
          try {
            handleBlobForTauri(blob, fileName);
          } catch (error) {
            console.error('Erreur lors de la sauvegarde du fichier dans Tauri:', error);
          }
        } else {
          // Web version - download file directly
          saveAs(blob, fileName);
        }
      },
      error: (err) => {
        console.error('Erreur lors de l\'exportation:', err);
      }
    });
  }
}
