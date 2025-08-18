import { AfterViewInit, Component, inject, OnInit, viewChild } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { ISales } from 'app/shared/model/sales.model';
import { SalesService } from './sales.service';
import { LazyLoadEvent, MenuItem } from 'primeng/api';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
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
import { Select } from 'primeng/select';
import { InputGroupModule } from 'primeng/inputgroup';
import { InputGroupAddonModule } from 'primeng/inputgroupaddon';
import { DatePickerModule } from 'primeng/datepicker';
import { FloatLabel } from 'primeng/floatlabel';
import { TIMES } from '../../shared/util/times';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { showCommonModal } from './selling-home/sale-helper';
import { SaleUpdateDateModalComponent } from './sale-update-date-modal/sale-update-date-modal.component';
import { debounceTime, Subject } from 'rxjs';
import { ConfirmDialogComponent } from '../../shared/dialog/confirm-dialog/confirm-dialog.component';
import { CustomerEditModalComponent } from './customer-edit-modal/customer-edit-modal.component';
import { Card } from 'primeng/card';

@Component({
  selector: 'jhi-sales',
  styleUrls: ['./sales.component.scss'],
  templateUrl: './sales.component.html',
  providers: [DatePipe],
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
    ConfirmDialogComponent,
    Card
  ]
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
  protected actions: MenuItem[] | undefined;
  private readonly translate = inject(TranslateService);
  private readonly primeNGConfig = inject(PrimeNG);
  private readonly assuranceSalesService = inject(VoSalesService);
  private readonly salesService = inject(SalesService);
  private readonly router = inject(Router);
  private readonly userService = inject(UserService);
  private readonly modalService = inject(NgbModal);
  private readonly datePipe = inject(DatePipe);
  private searchSubject = new Subject<void>();
  private readonly confimDialog = viewChild.required<ConfirmDialogComponent>('confirmDialog');

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

  editSaleUpdatedDate(sale: ISales): void {
    if (sale) {
      showCommonModal(
        this.modalService,
        SaleUpdateDateModalComponent,
        { sale },
        (updatedSale: ISales) => {
          if (updatedSale) {
            this.loadPage();
          }
        },
        '45%'
      );
    }
  }

  ngOnInit(): void {
    const width = window.innerWidth;
    if (width < 1800) {
      this.isLargeScreen = false;
    }

    this.canEdit = this.hasAuthorityService.hasAuthorities(Authority.PR_MODIFICATION_VENTE);
    this.canCancel = this.hasAuthorityService.hasAuthorities(Authority.PR_ANNULATION_VENTE);

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

    this.searchSubject.pipe(debounceTime(300)).subscribe(() => {
      this.loadPage();
    });
    this.actions = [
      {
        label: 'Options',
        items: [
          {
            label: 'Modifier la vente',
            icon: 'pi pi-pencil'
          },
          {
            label: 'Modifier la date de vente',
            icon: 'pi pi-calendar-plus'
          },
          {
            label: 'Modifier les informations du client',
            icon: 'pi pi-user-edit'
          }
        ]
      }
    ];
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
    this.userControl().value = this.selectedUserId;
  }

  protected onTypeVenteChange(): void {
    this.searchSubject.next();
  }

  protected onEditCustomer(currSale: ISales): void {
    showCommonModal(
      this.modalService,
      CustomerEditModalComponent,
      {
        sale: currSale
      },
      () => {
        this.loadPage();
      },
      'xl'
    );
  }

  protected loadPage(page?: number): void {
    const pageToLoad: number = page || this.page;
    this.fetchSales(pageToLoad, this.itemsPerPage);
    this.updateParam();
  }

  protected lazyLoading(event: LazyLoadEvent): void {
    if (event) {
      this.page = event.first / event.rows;
      this.itemsPerPage = event.rows;
      this.fetchSales(this.page, this.itemsPerPage);
    }
  }

  protected onSearch(): void {
    this.searchSubject.next();
  }

  protected delete(sale: ISales): void {
    if (sale) {
      if (sale.categorie === 'VNO') {
        this.salesService.cancelComptant(sale.id).subscribe(() => this.loadPage());
      } else {
        this.salesService.cancelAssurance(sale.id).subscribe(() => this.loadPage());
      }
    }
  }

  protected confirmRemove(sale: ISales): void {
    this.confimDialog().onConfirm(() => this.delete(sale), 'ANNULATION DE VENTE', 'Voulez-vous vraiment annuler cette vente ?');
  }

  protected print(sales: ISales): void {
    this.salesService.printInvoice(sales.id).subscribe(blod => {
      const blobUrl = URL.createObjectURL(blod);
      window.open(blobUrl);
    });
  }

  protected printSale(sale: ISales): void {
    if (sale.categorie === 'VNO') {
      this.salesService.rePrintReceipt(sale.id).subscribe();
    } else {
      this.assuranceSalesService.rePrintReceipt(sale.id).subscribe();
    }
  }

  protected suggerer(sales: ISales): void {
    console.log(sales);
  }

  protected onSuccess(data: ISales[] | null, headers: HttpHeaders, page: number): void {
    this.totalItems = Number(headers.get('X-Total-Count'));
    this.page = page;
    const queryParams = {
      page: this.page,
      size: this.itemsPerPage,
      search: this.search,
      type: this.typeVenteSelected,
      fromDate: this.fromDate ? this.datePipe.transform(this.fromDate, 'yyyy-MM-dd') : null,
      toDate: this.toDate ? this.datePipe.transform(this.toDate, 'yyyy-MM-dd') : null,
      fromHour: this.fromHour,
      toHour: this.toHour,
      global: this.global,
      userId: this.selectedUserId
    };

    this.router.navigate(['/sales'], { queryParams });
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
        fromDate: this.fromDate ? this.datePipe.transform(this.fromDate, 'yyyy-MM-dd') : null,
        toDate: this.toDate ? this.datePipe.transform(this.toDate, 'yyyy-MM-dd') : null,
        fromHour: this.fromHour,
        toHour: this.toHour,
        global: this.global,
        userId: this.selectedUserId
      })
      .subscribe({
        next: (res: HttpResponse<ISales[]>) => this.onSuccess(res.body, res.headers, page),
        error: () => this.onError()
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
      selectedUserId: this.selectedUserId
    });
  }
}
