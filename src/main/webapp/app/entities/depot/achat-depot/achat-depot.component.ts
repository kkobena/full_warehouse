import { AfterViewInit, Component, inject, OnInit, viewChild } from '@angular/core';
import { Button } from 'primeng/button';
import { ConfirmDialogComponent } from '../../../shared/dialog/confirm-dialog/confirm-dialog.component';
import { DatePicker } from 'primeng/datepicker';
import { DecimalPipe } from '@angular/common';
import { FloatLabel } from 'primeng/floatlabel';
import { InputGroup } from 'primeng/inputgroup';
import { InputText } from 'primeng/inputtext';
import { Select } from 'primeng/select';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { Toolbar } from 'primeng/toolbar';
import { Tooltip } from 'primeng/tooltip';
import { ITEMS_PER_PAGE } from '../../../shared/constants/pagination.constants';
import { ISales, SaleId } from '../../../shared/model/sales.model';
import { IUser } from '../../../core/user/user.model';
import { MenuItem } from 'primeng/api';
import { HasAuthorityService } from '../../sales/service/has-authority.service';
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
import { Router, RouterLink } from '@angular/router';
import { StockDepotService } from '../stock-depot/stock-depot.service';
import { DATE_FORMAT_ISO_DATE } from '../../../shared/util/warehouse-util';

@Component({
  selector: 'jhi-achat-depot',
  imports: [
    Button, WarehouseCommonModule,
    ConfirmDialogComponent,
    DatePicker,
    DecimalPipe,
    FloatLabel,
    InputGroup,
    InputText,
    Select,
    TableModule,
    Toolbar,
    Tooltip,
    FormsModule,
    RouterLink
  ],
  templateUrl: './achat-depot.component.html',
  styleUrl: './achat-depot.component.scss'
})
export class AchatDepotComponent implements OnInit, AfterViewInit {
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
  protected hasAuthorityService = inject(HasAuthorityService);

  protected actions: MenuItem[] | undefined;
  private readonly translate = inject(TranslateService);
  private readonly primeNGConfig = inject(PrimeNG);
  private readonly salesService = inject(SalesService);
  private readonly stockDepotService = inject(StockDepotService);
  private readonly userService = inject(UserService);

  private searchSubject = new Subject<void>();
  private readonly confimDialog = viewChild.required<ConfirmDialogComponent>('confirmDialog');
  private readonly tauriPrinterService = inject(TauriPrinterService);
  private readonly magasinService = inject(MagasinService);
  private router = inject(Router);

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
  }

  onNewVente(): void {
    this.router.navigate(['/depot', 'new-vente']);
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

  ngAfterViewInit(): void {

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


  protected exportToCsv(sale: ISales): void {

  }

  protected print(sales: ISales): void {
    this.salesService.printInvoice(sales.saleId).subscribe(blob => {
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
}
