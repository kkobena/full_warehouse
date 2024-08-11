import { AfterViewInit, Component, ElementRef, inject, OnInit, viewChild } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { ISales } from 'app/shared/model/sales.model';
import { SalesService } from './sales.service';
import { ConfirmationService, LazyLoadEvent, MenuItem, PrimeNGConfig } from 'primeng/api';
import { TranslateService } from '@ngx-translate/core';
import { Subscription } from 'rxjs';
import moment from 'moment';
import { IUser, User } from '../../core/user/user.model';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { UserService } from '../../core/user/user.service';
import { HOURS, ITEMS_PER_PAGE } from '../../shared/constants/pagination.constants';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { FormsModule } from '@angular/forms';
import { TooltipModule } from 'primeng/tooltip';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { RippleModule } from 'primeng/ripple';
import { TableModule } from 'primeng/table';
import { DropdownModule } from 'primeng/dropdown';
import { ToolbarModule } from 'primeng/toolbar';
import { DividerModule } from 'primeng/divider';
import { CalendarModule } from 'primeng/calendar';
import { CheckboxModule } from 'primeng/checkbox';
import { SplitButtonModule } from 'primeng/splitbutton';
import { VoSalesService } from './service/vo-sales.service';
import { HasAuthorityService } from './service/has-authority.service';
import { SaleToolBarService } from './service/sale-tool-bar.service';

@Component({
  selector: 'jhi-sales',
  styles: [
    `
      .table tr:hover {
        cursor: pointer;
      }

      .p-datatable td {
        font-size: 0.6rem;
      }

      .table tr th {
        font-size: 0.9rem;
      }

      .secondColumn {
        color: blue;
        text-align: right;
      }

      .invoice-table tr {
        border-bottom: 1px solid #dee2e6;
      }

      .invoice-table td:first-child {
        text-align: left;
      }

      .invoice-table td {
        padding: 0.1rem;
      }

      .invoice-table {
        color: #2d2d2d !important;
      }
    `,
  ],
  templateUrl: './sales.component.html',
  providers: [ConfirmationService],
  standalone: true,
  imports: [
    WarehouseCommonModule,
    RouterModule,
    ConfirmDialogModule,
    FormsModule,
    TooltipModule,
    ButtonModule,
    InputTextModule,
    RippleModule,
    TableModule,
    DropdownModule,
    ToolbarModule,
    DividerModule,
    CalendarModule,
    CheckboxModule,
    SplitButtonModule,
  ],
})
export class SalesComponent implements OnInit, AfterViewInit {
  typeVentes: string[] = ['TOUT', 'VNO', 'VO'];
  typeVenteSelected = '';
  totalItems = 0;
  loading!: boolean;
  canEdit = false;
  page = 0;
  itemsPerPage = ITEMS_PER_PAGE;
  sales: ISales[] = [];
  selectedEl?: ISales;
  users: IUser[] = [];
  selectedUserId: number | null;
  search = '';
  global = true;
  showBtnDele: boolean;
  fromDate: Date = new Date();
  toDate: Date = new Date();
  isLargeScreen = true;
  fromHour = '01:00';
  toHour = '23:59';
  hous = HOURS;
  splitbuttons: MenuItem[];
  primngtranslate: Subscription;
  hasAuthorityService = inject(HasAuthorityService);
  saleToolBarService = inject(SaleToolBarService);
  userControl = viewChild<ElementRef>('userControl');

  constructor(
    protected assuranceSalesService: VoSalesService,
    protected salesService: SalesService,
    protected activatedRoute: ActivatedRoute,
    protected router: Router,
    protected confirmationService: ConfirmationService,
    protected userService: UserService,
    public translate: TranslateService,
    public primeNGConfig: PrimeNGConfig,
  ) {
    this.translate.use('fr');
    this.primngtranslate = this.translate.stream('primeng').subscribe(data => {
      this.primeNGConfig.setTranslation(data);
    });
    this.showBtnDele = false;
    this.splitbuttons = [
      {
        label: 'Fiche à partir csv',
        icon: 'pi pi-file-pdf',
        command: () => console.error('print all record'),
      },
    ];
  }

  ngOnInit(): void {
    const width = window.innerWidth;
    if (width < 1800) {
      this.isLargeScreen = false;
    }
    // this.typeVenteSelected = 'TOUT';
    this.canEdit = this.hasAuthorityService.hasAuthorities('EDIT_SALES');

    this.loadAllUsers();
    const lastPram = this.saleToolBarService.toolBarParam();
    if (lastPram) {
      this.typeVenteSelected = lastPram.typeVente || 'TOUT';
      this.search = lastPram.search;
      this.global = lastPram.global;
      this.fromDate = lastPram.fromDate || new Date();
      this.toDate = lastPram.toDate || new Date();
      this.fromHour = lastPram.fromHour || '01:00';
      this.toHour = lastPram.toHour || '23:59';
      this.selectedUserId = lastPram.selectedUserId || null;
    }
    this.loadPage();
  }

  loadAllUsers(): void {
    this.userService.query().subscribe((res: HttpResponse<User[]>) => {
      if (res.body) {
        this.users = res.body;
      }
    });
  }

  onSelectUser(evt: any): void {
    this.selectedUserId = evt.value;
    this.loadPage();
  }

  onTypeVenteChange(): void {
    this.loadPage();
  }

  loadPage(page?: number): void {
    const pageToLoad: number = page || this.page;
    this.loading = true;
    this.salesService
      .query({
        page: pageToLoad,
        size: this.itemsPerPage,
        search: this.search,
        type: this.typeVenteSelected,
        fromDate: this.fromDate ? moment(this.fromDate).format('yyyy-MM-DD') : null,
        toDate: this.toDate ? moment(this.toDate).format('yyyy-MM-DD') : null,
        fromHour: this.fromHour,
        toHour: this.toHour,
        global: this.global,
        userId: this.selectedUserId,
      })
      .subscribe({
        next: (res: HttpResponse<ISales[]>) => this.onSuccess(res.body, res.headers, pageToLoad),
        error: () => this.onError(),
      });
    this.updateParam();
  }

  lazyLoading(event: LazyLoadEvent): void {
    if (event) {
      this.page = event.first / event.rows;
      this.loading = true;
      this.salesService
        .query({
          page: this.page,
          size: event.rows,
          search: this.search,
          type: this.typeVenteSelected,
          fromDate: this.fromDate ? moment(this.fromDate).format('yyyy-MM-DD') : null,
          toDate: this.toDate ? moment(this.toDate).format('yyyy-MM-DD') : null,
          fromHour: this.fromHour,
          toHour: this.toHour,
          global: this.global,
          userId: this.selectedUserId,
        })
        .subscribe({
          next: (res: HttpResponse<ISales[]>) => this.onSuccess(res.body, res.headers, this.page),
          error: () => this.onError(),
        });
    }
  }

  onSearch(): void {
    this.loadPage();
  }

  delete(sale: ISales): void {
    if (sale) {
      if (sale.categorie === 'VNO') {
        this.salesService.cancelComptant(sale.id).subscribe(() => this.loadPage());
      } else {
        this.salesService.cancelAssurance(sale.id).subscribe(() => this.loadPage());
      }
    }
  }

  confirmRemove(sale: ISales): void {
    this.confirmationService.confirm({
      message: 'Voulez-vous vraiment annuler cette vente ?',
      header: 'ANNULATION DE VENTE',
      icon: 'pi pi-info-circle',
      accept: () => this.delete(sale),
      key: 'deleteVente',
    });
  }

  print(sales: ISales): void {
    this.salesService.printInvoice(sales.id).subscribe(blod => {
      const blobUrl = URL.createObjectURL(blod);
      window.open(blobUrl);
    });
  }

  printSale(sale: ISales): void {
    if (sale.categorie === 'VNO') {
      this.salesService.printReceipt(sale.id).subscribe();
    } else {
      this.assuranceSalesService.printReceipt(sale.id).subscribe();
    }
  }

  suggerer(sales: ISales): void {
    console.log(sales);
  }

  ngAfterViewInit(): void {
    this.userControl().nativeElement.value = this.selectedUserId;
  }

  protected onSuccess(data: ISales[] | null, headers: HttpHeaders, page: number): void {
    this.totalItems = Number(headers.get('X-Total-Count'));
    this.page = page;
    this.router.navigate(['/sales'], {
      queryParams: {
        page: this.page,
        size: this.itemsPerPage,
        search: this.search,
        type: this.typeVenteSelected,
        fromDate: this.fromDate ? moment(this.fromDate).format('yyyy-MM-DD') : null,
        toDate: this.toDate ? moment(this.toDate).format('yyyy-MM-DD') : null,
        fromHour: this.fromHour,
        toHour: this.toHour,
        global: this.global,
        userId: this.selectedUserId,
      },
    });
    this.sales = data || [];
    this.loading = false;
  }

  protected onError(): void {
    this.loading = false;
  }

  private updateParam(): void {
    this.saleToolBarService.updateToolBarParam({
      typeVente: this.typeVenteSelected,
      search: this.search,
      global: this.global,
      fromDate: this.fromDate,
      toDate: this.toDate,
      fromHour: this.fromHour,
      toHour: this.toHour,
      selectedUserId: this.selectedUserId,
    });
  }
}
