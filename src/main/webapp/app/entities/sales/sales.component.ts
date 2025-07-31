import { AfterViewInit, Component, inject, OnInit, viewChild } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { ISales } from 'app/shared/model/sales.model';
import { SalesService } from './sales.service';
import { ConfirmationService, LazyLoadEvent, MenuItem } from 'primeng/api';
import { TranslateService } from '@ngx-translate/core';
import moment from 'moment';
import { IUser } from '../../core/user/user.model';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { UserService } from '../../core/user/user.service';
import { ITEMS_PER_PAGE } from '../../shared/constants/pagination.constants';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { FormsModule } from '@angular/forms';
import { TooltipModule } from 'primeng/tooltip';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { RippleModule } from 'primeng/ripple';
import { TableModule } from 'primeng/table';
import { ToolbarModule } from 'primeng/toolbar';
import { DividerModule } from 'primeng/divider';
import { CalendarModule } from 'primeng/calendar';
import { CheckboxModule } from 'primeng/checkbox';
import { SplitButtonModule } from 'primeng/splitbutton';
import { VoSalesService } from './service/vo-sales.service';
import { HasAuthorityService } from './service/has-authority.service';
import { SaleToolBarService } from './service/sale-tool-bar.service';
import { Authority } from '../../shared/constants/authority.constants';
import { PrimeNG } from 'primeng/config';
import { acceptButtonProps, rejectButtonProps } from '../../shared/util/modal-button-props';
import { Select } from 'primeng/select';
import { InputGroupModule } from 'primeng/inputgroup';
import { InputGroupAddonModule } from 'primeng/inputgroupaddon';
import { DatePickerModule } from 'primeng/datepicker';
import { FloatLabel } from 'primeng/floatlabel';
import { TIMES } from '../../shared/util/times';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { showCommonModal } from './selling-home/sale-helper';
import { SaleUpdateDateModalComponent } from './sale-update-date-modal/sale-update-date-modal.component';

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
    ToolbarModule,
    DividerModule,
    CalendarModule,
    CheckboxModule,
    SplitButtonModule,
    Select,
    InputGroupModule,
    InputGroupAddonModule,
    DatePickerModule,
    FloatLabel,
  ],
})
export class SalesComponent implements OnInit, AfterViewInit {
  protected typeVentes: string[] = ['TOUT', 'VNO', 'VO'];
  protected typeVenteSelected = '';
  protected totalItems = 0;
  protected loading!: boolean;
  protected canEdit = false;
  protected canCancel = false;
  protected page = 0;
  protected itemsPerPage = ITEMS_PER_PAGE;
  protected sales: ISales[] = [];
  protected selectedEl?: ISales;
  protected users: IUser[] = [];
  protected selectedUserId: number | null;
  protected search = '';
  protected global = true;
  protected showBtnDele: boolean;
  protected fromDate: Date = new Date();
  protected toDate: Date = new Date();
  protected isLargeScreen = true;
  protected fromHour = '01:00';
  protected toHour = '23:59';
  protected hous = TIMES;
  protected splitbuttons: MenuItem[];
  protected hasAuthorityService = inject(HasAuthorityService);
  protected saleToolBarService = inject(SaleToolBarService);
  protected userControl = viewChild<Select>('userControl');
  private readonly translate = inject(TranslateService);
  private readonly primeNGConfig = inject(PrimeNG);
  private readonly assuranceSalesService = inject(VoSalesService);
  private readonly salesService = inject(SalesService);
  private readonly router = inject(Router);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly userService = inject(UserService);

  private readonly modalService = inject(NgbModal);

  constructor() {
    this.translate.use('fr');
    this.translate.stream('primeng').subscribe(data => {
      this.primeNGConfig.setTranslation(data);
    });
    this.showBtnDele = false;
    this.splitbuttons = [
      {
        label: 'Fiche à partir csv',
        icon: 'pi pi-file-pdf',
        command: () => console.error('print all record'),
      } /*,
      {
        label: 'Modifier la date',
        icon: 'pi pi-calendar-plus',
        command: () => this.editSaleUpdatedDate(),
      },*/,
    ];
  }

  editSaleUpdatedDate(sale: ISales): void {
    if (sale) {
      showCommonModal(this.modalService, SaleUpdateDateModalComponent, { sale }, (updatedSale: ISales) => {
        if (updatedSale) {
          // const index = this.sales.findIndex(s => s.id === updatedSale.id);
          /* if (index !== -1) {
             this.sales[index] = updatedSale;
           }*/
        }
      });
    }
  }

  ngOnInit(): void {
    const width = window.innerWidth;
    if (width < 1800) {
      this.isLargeScreen = false;
    }

    this.canEdit = !this.hasAuthorityService.hasAuthorities(Authority.PR_MODIFICATION_VENTE);
    this.canCancel = !this.hasAuthorityService.hasAuthorities(Authority.PR_ANNULATION_VENTE);

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
    this.userService.query().subscribe((res: HttpResponse<IUser[]>) => {
      if (res.body) {
        this.users = [{ id: null, abbrName: 'TOUT' }];
        this.users = [...this.users, ...res.body];
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
    this.fetchSales(pageToLoad, this.itemsPerPage);
    this.updateParam();
  }

  lazyLoading(event: LazyLoadEvent): void {
    if (event) {
      this.page = event.first / event.rows;
      this.itemsPerPage = event.rows;
      this.fetchSales(this.page, this.itemsPerPage);
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
      rejectButtonProps: rejectButtonProps(),
      acceptButtonProps: acceptButtonProps(),
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
      this.salesService.rePrintReceipt(sale.id).subscribe();
    } else {
      this.assuranceSalesService.rePrintReceipt(sale.id).subscribe();
    }
  }

  suggerer(sales: ISales): void {
    console.log(sales);
  }

  ngAfterViewInit(): void {
    this.userControl().value = this.selectedUserId;
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

  private fetchSales(page: number, size: number): void {
    this.loading = true;
    this.salesService
      .query({
        page,
        size,
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
        next: (res: HttpResponse<ISales[]>) => this.onSuccess(res.body, res.headers, page),
        error: () => this.onError(),
      });
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
