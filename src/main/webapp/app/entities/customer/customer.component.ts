import { Component, OnInit, OnDestroy } from '@angular/core';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { ActivatedRoute, ParamMap, Router, Data } from '@angular/router';
import { Subscription, combineLatest, Observable } from 'rxjs';
import { JhiEventManager } from 'ng-jhipster';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';

import { ICustomer } from 'app/shared/model/customer.model';

import { ITEMS_PER_PAGE } from 'app/shared/constants/pagination.constants';
import { CustomerService } from './customer.service';
import { ConfirmationService, LazyLoadEvent, MenuItem, MessageService, PrimeNGConfig } from 'primeng/api';
import { UninsuredCustomerFormComponent } from './uninsured-customer-form/uninsured-customer-form.component';
import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';
import { FormAssuredCustomerComponent } from './form-assured-customer/form-assured-customer.component';
import { TranslateService } from '@ngx-translate/core';
import { FormAyantDroitComponent } from './form-ayant-droit/form-ayant-droit.component';
import { NgxSpinnerService } from 'ngx-spinner';

@Component({
  selector: 'jhi-customer',
  templateUrl: './customer.component.html',
  providers: [ConfirmationService, DialogService, MessageService],
})
export class CustomerComponent implements OnInit, OnDestroy {
  customers?: ICustomer[];
  types: string[] = ['TOUT', 'ASSURE', 'STANDARD'];
  statuts: object[] = [
    { value: 'ENABLE', label: 'ACTIF' },
    { value: 'DISABLE', label: 'DESACTIVE' },
  ];
  typeSelected = '';
  statutSelected = 'ENABLE';
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
  primngtranslate: Subscription;
  displayTiersPayantAction = false;
  jsonDialog = false;
  responseDialog = false;

  constructor(
    protected customerService: CustomerService,
    protected activatedRoute: ActivatedRoute,
    protected router: Router,
    protected eventManager: JhiEventManager,
    protected modalService: NgbModal,
    private dialogService: DialogService,
    protected confirmationService: ConfirmationService,
    public translate: TranslateService,
    public primeNGConfig: PrimeNGConfig,
    private spinner: NgxSpinnerService,
    private messageService: MessageService
  ) {
    this.translate.use('fr');
    this.primngtranslate = this.translate.stream('primeng').subscribe(data => {
      this.primeNGConfig.setTranslation(data);
    });
    this.splitbuttons = [
      {
        label: 'Importer à partir json',
        icon: 'pi pi-file-pdf',
        command: () => (this.jsonDialog = true),
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
        status: this.statutSelected,
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
          status: this.statutSelected,
        })
        .subscribe(
          (res: HttpResponse<ICustomer[]>) => this.onSuccess(res.body, res.headers, this.page, false),
          () => this.onError()
        );
    }
  }

  ngOnInit(): void {
    this.typeSelected = 'TOUT';
    this.statutSelected = 'ENABLE';
    this.handleNavigation();
    this.loadPage();
    //   this.registerChangeInCustomers();
  }

  cancel(): void {
    this.jsonDialog = false;
    this.responseDialog = false;
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

  deleteAssuredCustomer(customer: ICustomer): void {
    this.customerService.deleteAssuredCustomer(customer.id!).subscribe(() => this.loadPage());
  }

  lock(customer: ICustomer): void {
    this.customerService.lock(customer.id!).subscribe(() => this.loadPage());
  }

  confirmRemove(customer: ICustomer): void {
    this.confirmationService.confirm({
      message: 'Voulez-vous vraiment supprimer ce client ?',
      header: 'SUPPRESSION DE CLIENT',
      icon: 'pi pi-info-circle',
      accept: () => {
        if (customer.categorie === 'ASSURE') {
          this.deleteAssuredCustomer(customer);
        } else {
          this.delete(customer);
        }
      },
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

  addAssureCustomer(): void {
    this.ref = this.dialogService.open(FormAssuredCustomerComponent, {
      data: { entity: null },
      header: 'FORMULAIRE DE CREATION DE CLIENT ',
      width: '85%',
      closeOnEscape: false,
    });
    this.ref.onClose.subscribe((resp: ICustomer) => {
      if (resp) {
        this.loadPage();
      }
    });
  }

  editAssureCustomer(customer: ICustomer): void {
    this.ref = this.dialogService.open(FormAssuredCustomerComponent, {
      data: { entity: customer },
      header: 'FORMULAIRE DE MODIFICATION DE CLIENT ',
      width: '85%',
      closeOnEscape: false,
    });
    this.ref.onClose.subscribe((resp: ICustomer) => {
      if (resp) {
        this.loadPage();
      }
    });
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

  editUninsuredCustomer(customer: ICustomer): void {
    this.ref = this.dialogService.open(UninsuredCustomerFormComponent, {
      data: { entity: customer },
      header: 'FORMULAIRE DE MODIFICATION DE CLIENT ',
      width: '50%',
    });
    this.ref.onClose.subscribe((resp: ICustomer) => {
      if (resp) {
        this.loadPage();
      }
    });
  }

  addAyantDroit(customer: ICustomer): void {
    this.ref = this.dialogService.open(FormAyantDroitComponent, {
      data: { entity: null, assure: customer },
      header: "FORMULAIRE D'AJOUT D'AYANT DROIT ",
      width: '50%',
      closeOnEscape: false,
    });
    this.ref.onClose.subscribe((resp: ICustomer) => {
      if (resp) {
        this.loadPage();
      }
    });
  }

  editAyantDroit(customer: ICustomer, ayantDroit: ICustomer): void {
    this.ref = this.dialogService.open(FormAyantDroitComponent, {
      data: { entity: ayantDroit, assure: customer },
      header: "FORMULAIRE DE MODIFICATION D'AYANT DROIT ",
      width: '50%',
      closeOnEscape: false,
    });
    this.ref.onClose.subscribe((resp: ICustomer) => {
      if (resp) {
        this.loadPage();
      }
    });
  }

  confirmRemoveAyantDroit(ayantDroit: ICustomer): void {
    this.confirmationService.confirm({
      message: 'Voulez-vous vraiment supprimer cet ayant droit ?',
      header: 'SUPPRESSION ',
      icon: 'pi pi-info-circle',
      accept: () => this.deleteAssuredCustomer(ayantDroit),
      key: 'deleteCustomer',
    });
  }

  onUploadJson(event: any): void {
    const formData: FormData = new FormData();
    const file = event.files[0];
    formData.append('importjson', file, file.name);
    this.spinner.show('importation');
    this.jsonDialog = false;
    this.uploadJsonDataResponse(this.customerService.uploadJsonData(formData));
  }

  protected uploadJsonDataResponse(result: Observable<HttpResponse<void>>): void {
    result.subscribe(
      () => this.onPocesJsonSuccess(),
      () => this.onImportError()
    );
  }

  protected onImportError(): void {
    this.spinner.hide('importation');
    this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Enregistrement a échoué' });
  }

  protected onPocesJsonSuccess(): void {
    this.jsonDialog = false;
    this.spinner.hide('importation');
    this.loadPage();
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
