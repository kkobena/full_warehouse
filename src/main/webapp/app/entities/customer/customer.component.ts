import { Component, OnInit, OnDestroy } from '@angular/core';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { ActivatedRoute, ParamMap, Router, Data } from '@angular/router';
import { Subscription, combineLatest } from 'rxjs';
import { JhiEventManager } from 'ng-jhipster';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';

import { ICustomer } from 'app/shared/model/customer.model';

import { ITEMS_PER_PAGE } from 'app/shared/constants/pagination.constants';
import { CustomerService } from './customer.service';
import { CustomerDeleteDialogComponent } from './customer-delete-dialog.component';
import { ConfirmationService, LazyLoadEvent, MenuItem } from 'primeng/api';
import { UninsuredCustomerFormComponent } from './uninsured-customer-form/uninsured-customer-form.component';
import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';
import { ISales } from '../../shared/model/sales.model';

@Component({
  selector: 'jhi-customer',
  templateUrl: './customer.component.html',
  providers: [ConfirmationService, DialogService],
})
export class CustomerComponent implements OnInit, OnDestroy {
  customers?: ICustomer[];
  type: string[] = ['TOUT', 'ASSURE', 'STANDARD'];
  typeSelected = '';
  search = '';
  totalItems = 0;
  loading!: boolean;
  eventSubscriber?: Subscription;
  itemsPerPage = ITEMS_PER_PAGE;
  page!: number;
  predicate!: string;
  ascending!: boolean;
  ngbPaginationPage = 1;
  splitbuttons: MenuItem[];
  ref!: DynamicDialogRef;
  constructor(
    protected customerService: CustomerService,
    protected activatedRoute: ActivatedRoute,
    protected router: Router,
    protected eventManager: JhiEventManager,
    protected modalService: NgbModal,
    private dialogService: DialogService,
    protected confirmationService: ConfirmationService
  ) {
    this.splitbuttons = [
      {
        label: 'Importer à partir csv',
        icon: 'pi pi-file-pdf',
        command: () => console.error('print all record'),
      },
    ];
  }
  onSearch(): void {
    this.loadPage();
  }
  onTypeChange(): void {
    this.loadPage();
  }

  loadPage(page?: number, dontNavigate?: boolean): void {
    const pageToLoad: number = page || this.page || 1;

    this.customerService
      .query({
        page: pageToLoad - 1,
        size: this.itemsPerPage,
        sort: this.sort(),
        type: this.typeSelected,
        search: this.search,
      })
      .subscribe(
        (res: HttpResponse<ICustomer[]>) => this.onSuccess(res.body, res.headers, pageToLoad, !dontNavigate),
        () => this.onError()
      );
  }

  lazyLoading(event: LazyLoadEvent): void {
    if (event) {
      this.page = event.first! / event.rows!;
      this.loading = true;
      this.customerService
        .query({
          page: this.page,
          size: event.rows,
          sort: this.sort(),
          type: this.typeSelected,
          search: this.search,
        })
        .subscribe(
          (res: HttpResponse<ICustomer[]>) => this.onSuccess(res.body, res.headers, this.page, false),
          () => this.onError()
        );
    }
  }

  ngOnInit(): void {
    this.typeSelected = 'TOUT';
    this.handleNavigation();
    this.loadPage();
    //   this.registerChangeInCustomers();
  }

  protected handleNavigation(): void {
    combineLatest(this.activatedRoute.data, this.activatedRoute.queryParamMap, (data: Data, params: ParamMap) => {
      const page = params.get('page');
      const pageNumber = page !== null ? +page : 1;
      const sort = (params.get('sort') ?? data['defaultSort']).split(',');
      const predicate = sort[0];
      const ascending = sort[1] === 'asc';
      if (pageNumber !== this.page || predicate !== this.predicate || ascending !== this.ascending) {
        this.predicate = predicate;
        this.ascending = ascending;
        this.loadPage(pageNumber, true);
      }
    }).subscribe();
  }

  ngOnDestroy(): void {
    if (this.eventSubscriber) {
      this.eventManager.destroy(this.eventSubscriber);
    }
  }

  trackId(index: number, item: ICustomer): number {
    return item.id!;
  }

  registerChangeInCustomers(): void {
    this.eventSubscriber = this.eventManager.subscribe('customerListModification', () => this.loadPage());
  }

  delete(customer: ICustomer): void {
    this.customerService.delete(customer.id!).subscribe(() => this.loadPage());
  }
  lock(customer: ICustomer): void {
    this.customerService.lock(customer.id!).subscribe(() => this.loadPage());
  }
  confirmRemove(customer: ICustomer): void {
    this.confirmationService.confirm({
      message: 'Voulez-vous vraiment supprimer ce client ?',
      header: 'SUPPRESSION DE CLIENT',
      icon: 'pi pi-info-circle',
      accept: () => this.delete(customer),
      key: 'deleteCustomer',
    });
  }
  confirmDesactivation(customer: ICustomer): void {
    this.confirmationService.confirm({
      message: 'Voulez-vous vraiment désativer ce client ?',
      header: 'DESACTIVATION DE CLIENT',
      icon: 'pi pi-info-circle',
      accept: () => this.lock(customer),
      key: 'desactiverCustomer',
    });
  }
  sort(): string[] {
    const result = [this.predicate + ',' + (this.ascending ? 'asc' : 'desc')];
    if (this.predicate !== 'id') {
      result.push('id');
    }
    return result;
  }
  addUninsuredCustomer(): void {
    this.ref = this.dialogService.open(UninsuredCustomerFormComponent, {
      data: { entity: null },
      header: 'FORMULAIRE DE CREATION DE CLIENT ',
      width: '50%',
    });
    this.ref.onClose.subscribe((resp: ICustomer) => {
      if (resp) {
        this.loadPage();
      }
    });
  }

  protected onSuccess(data: ICustomer[] | null, headers: HttpHeaders, page: number, navigate: boolean): void {
    this.totalItems = Number(headers.get('X-Total-Count'));
    this.page = page;
    if (navigate) {
      this.router.navigate(['/customer'], {
        queryParams: {
          page: this.page,
          size: this.itemsPerPage,
          sort: this.predicate + ',' + (this.ascending ? 'asc' : 'desc'),
        },
      });
    }
    this.customers = data || [];
    this.ngbPaginationPage = this.page;
    this.loading = false;
  }

  protected onError(): void {
    this.loading = false;
    this.ngbPaginationPage = this.page ?? 1;
  }
}
