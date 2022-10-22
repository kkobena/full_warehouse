import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ISales } from 'app/shared/model/sales.model';
import { SalesService } from './sales.service';
import { SalesLineService } from '../sales-line/sales-line.service';
import { faPrint } from '@fortawesome/free-solid-svg-icons';
import { ConfirmationService, LazyLoadEvent, MenuItem, PrimeNGConfig } from 'primeng/api';
import { TranslateService } from '@ngx-translate/core';
import { Subscription } from 'rxjs';
import * as moment from 'moment';
import { IUser, User } from '../../core/user/user.model';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { UserService } from '../../core/user/user.service';
import { ITEMS_PER_PAGE } from '../../shared/constants/pagination.constants';

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
})
export class SalesComponent implements OnInit {
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
  user?: IUser | null;
  search = '';
  global = true;
  showBtnDele: boolean;
  saleSelected?: ISales;
  faPrint = faPrint;
  fromDate: Date = new Date();
  toDate: Date = new Date();
  isLargeScreen = true;
  fromHour = '01:00';
  toHour = '23:59';
  hous = [
    '01:00',
    '01:30',
    '02:00',
    '02:30',
    '03:00',
    '04:00',
    '04:30',
    '05:00',
    '05:30',
    '06:00',
    '06:30',
    '07:30',
    '07:30',
    '08:00',
    '08:30',
    '09:00',
    '09:30',
    '10:00',
    '10:30',
    '11:00',
    '11:30',
    '12:00',
    '12:30',
    '13:00',
    '13:30',
    '14:00',
    '14:30',
    '15:00',
    '15:30',
    '16:00',
    '16:30',
    '17:00',
    '17:30',
    '18:00',
    '18:30',
    '19:00',
    '19:30',
    '20:00',
    '20:30',
    '21:00',
    '21:30',
    '22:00',
    '22:30',
    '23:00',
    '23:59',
    '00:00',
    '00:30',
  ];
  splitbuttons: MenuItem[];
  primngtranslate: Subscription;

  constructor(
    protected salesService: SalesService,
    protected activatedRoute: ActivatedRoute,
    protected router: Router,
    protected confirmationService: ConfirmationService,
    protected saleLineService: SalesLineService,
    protected userService: UserService,
    public translate: TranslateService,
    public primeNGConfig: PrimeNGConfig
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
    this.typeVenteSelected = 'TOUT';
    this.loadAllUsers();
    this.loadPage();
  }

  searchUser(event: any): void {
    const key = event.key;
    if (
      key !== 'ArrowDown' &&
      key !== 'ArrowUp' &&
      key !== 'ArrowRight' &&
      key !== 'ArrowLeft' &&
      key !== 'NumLock' &&
      key !== 'CapsLock' &&
      key !== 'Control' &&
      key !== 'PageUp' &&
      key !== 'PageDown'
    ) {
      this.loadAllUsers();
    }
  }

  loadAllUsers(): void {
    this.userService.query().subscribe((res: HttpResponse<User[]>) => {
      this.users.push({ id: null, fullName: 'TOUT' });
      if (res.body) {
        this.users.push(...res.body);
      }
      this.user = { id: null, fullName: 'TOUT' };
    });
  }

  onSelectUser(): void {
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
        userId: this.user?.id,
      })
      .subscribe(
        (res: HttpResponse<ISales[]>) => this.onSuccess(res.body, res.headers, pageToLoad),
        () => this.onError()
      );
  }

  lazyLoading(event: LazyLoadEvent): void {
    if (event) {
      this.page = event.first! / event.rows!;
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
          userId: this.user ? this.user.id : null,
        })
        .subscribe(
          (res: HttpResponse<ISales[]>) => this.onSuccess(res.body, res.headers, this.page),
          () => this.onError()
        );
    }
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
        userId: this.user ? this.user.id : null,
      },
    });
    this.sales = data || [];
    this.loading = false;
  }

  protected onError(): void {
    this.loading = false;
  }

  onSearch(): void {
    this.loadPage();
  }

  delete(sale: ISales): void {
    if (sale) {
      if (sale.categorie === 'VNO') {
        this.salesService.cancelComptant(sale.id!).subscribe(() => this.loadPage());
      } else {
        this.salesService.cancelAssurance(sale.id!).subscribe(() => this.loadPage());
      }
    }
  }

  confirmRemove(sale: ISales): void {
    this.confirmationService.confirm({
      message: 'Voulez-vous vraiment annuler cette vente ?',
      header: 'ANNULATION DE PRE-VENTE',
      icon: 'pi pi-info-circle',
      accept: () => this.delete(sale),
      key: 'deleteVente',
    });
  }

  print(sales: ISales): void {
    this.salesService.printInvoice(sales.id!).subscribe(blod => {
      const blobUrl = URL.createObjectURL(blod);
      window.open(blobUrl);
    });
  }

  printSale(sale: ISales): void {
    this.salesService.printReceipt(sale.id!, sale.categorie!).subscribe();
  }
}
