import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { ActivatedRoute, Data, ParamMap, Router, RouterLink } from '@angular/router';
import { combineLatest, Observable, Subject } from 'rxjs';
import { finalize, takeUntil } from 'rxjs/operators';

import { NgbModal } from '@ng-bootstrap/ng-bootstrap';

import { ICustomer } from 'app/shared/model/customer.model';

import { ITEMS_PER_PAGE } from 'app/shared/constants/pagination.constants';
import { CustomerService } from './customer.service';
import { ConfirmationService, LazyLoadEvent, MenuItem, MessageService } from 'primeng/api';
import {
  UninsuredCustomerFormComponent
} from './uninsured-customer-form/uninsured-customer-form.component';
import { DialogService } from 'primeng/dynamicdialog';
import { TranslateService } from '@ngx-translate/core';
import { FormAyantDroitComponent } from './form-ayant-droit/form-ayant-droit.component';
import { NgxSpinnerModule, NgxSpinnerService } from 'ngx-spinner';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { InputTextModule } from 'primeng/inputtext';
import { TableModule } from 'primeng/table';
import { DialogModule } from 'primeng/dialog';
import { FileUploadModule } from 'primeng/fileupload';
import { ToolbarModule } from 'primeng/toolbar';
import { DropdownModule } from 'primeng/dropdown';
import { SplitButtonModule } from 'primeng/splitbutton';
import { TooltipModule } from 'primeng/tooltip';
import {
  CustomerTiersPayantComponent
} from './customer-tiers-payant/customer-tiers-payant.component';
import { IClientTiersPayant } from '../../shared/model/client-tiers-payant.model';
import { AssureFormStepComponent } from './assure-form-step/assure-form-step.component';
import { PrimeNG } from 'primeng/config';
import { acceptButtonProps, rejectButtonProps } from '../../shared/util/modal-button-props';
import { Select } from 'primeng/select';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { Panel } from 'primeng/panel';
import { DividerModule } from 'primeng/divider';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { CustomerCarnetComponent } from './carnet/customer-carnet.component';

@Component({
  selector: 'jhi-customer',
  templateUrl: './customer.component.html',
  providers: [ConfirmationService, DialogService, MessageService],
  imports: [
    WarehouseCommonModule,
    DropdownModule,
    ToolbarModule,
    FormsModule,
    ButtonModule,
    RippleModule,
    ConfirmDialogModule,
    InputTextModule,
    TableModule,
    DialogModule,
    FileUploadModule,
    DividerModule,
    SplitButtonModule,
    NgxSpinnerModule,
    TooltipModule,
    RouterLink,
    Select,
    IconField,
    InputIcon,
    Panel,
  ],
})
export class CustomerComponent implements OnInit, OnDestroy {
  translate = inject(TranslateService);
  primeNGConfig = inject(PrimeNG);
  customers?: ICustomer[];
  types: string[] = ['TOUT', 'ASSURE', 'STANDARD'];
  statuts: object[] = [
    { value: 'ENABLE', label: 'Actifs' },
    { value: 'DISABLE', label: 'Désactivés' },
  ];
  typeSelected = '';
  statutSelected = 'ENABLE';
  search = '';
  totalItems = 0;
  itemsPerPage = ITEMS_PER_PAGE;
  page!: number;
  predicate!: string;
  ascending!: boolean;
  loading!: boolean;
  ngbPaginationPage = 1;
  newCustomerbuttons: MenuItem[];
  displayTiersPayantAction = true;
  jsonDialog = false;
  responseDialog = false;
  protected customerService = inject(CustomerService);
  protected activatedRoute = inject(ActivatedRoute);
  protected router = inject(Router);
  protected modalService = inject(NgbModal);
  protected confirmationService = inject(ConfirmationService);
  private destroy$ = new Subject<void>();
  private dialogService = inject(DialogService);
  private spinner = inject(NgxSpinnerService);
  private messageService = inject(MessageService);

  constructor() {
    this.translate.use('fr');
    this.translate.stream('primeng').subscribe(data => {
      this.primeNGConfig.setTranslation(data);
    });

    this.newCustomerbuttons = [
      {
        label: 'Assuré',
        icon: 'pi pi-user-plus',
        command: () => this.addAssureCustomer('ASSURANCE'),
      },
      {
        label: 'Carnet',
        icon: 'pi pi-user-plus',
        command: () => this.addCarnet('CARNET'),
      },
      {
        label: 'Dépôt',
        icon: 'pi pi-user-plus',
        command: () => this.addCarnet('DEPOT'),
      },
      {
        label: 'Standard',
        icon: 'pi pi-user-plus',
        command: () => this.addUninsuredCustomer(),
      },
    ];
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
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
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (res: HttpResponse<ICustomer[]>) => this.onSuccess(res.body, res.headers, pageToLoad, dontNavigate),
        error: () => this.onError(),
      });
  }

  lazyLoading(event: LazyLoadEvent): void {
    if (event) {
      this.page = event.first / event.rows;
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
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (res: HttpResponse<ICustomer[]>) => this.onSuccess(res.body, res.headers, this.page, false),
          error: () => this.onError(),
        });
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

  delete(customer: ICustomer): void {
    this.handleServiceCall(this.customerService.delete(customer.id), () => this.loadPage(), 'delete-customer-spinner');
  }

  deleteAssuredCustomer(customer: ICustomer): void {
    this.handleServiceCall(
      this.customerService.deleteAssuredCustomer(customer.id),
      () => this.loadPage(),
      'delete-assured-customer-spinner',
    );
  }

  lock(customer: ICustomer): void {
    this.handleServiceCall(this.customerService.lock(customer.id), () => this.loadPage(), 'lock-customer-spinner');
  }

  confirmRemove(customer: ICustomer): void {
    this.confirmationService.confirm({
      message: 'Voulez-vous vraiment supprimer ce client ?',
      header: 'SUPPRESSION DE CLIENT',
      icon: 'pi pi-info-circle',
      rejectButtonProps: rejectButtonProps(),
      acceptButtonProps: acceptButtonProps(),
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
      rejectButtonProps: rejectButtonProps(),
      acceptButtonProps: acceptButtonProps(),
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

  addCarnet(categorie: string): void {
    this.openCustomerModal(CustomerCarnetComponent, {
      entity: null,
      categorie: categorie,
      header: `FORMULAIRE DE CREATION DE CLIENT [ ${categorie} ]`,
    });
  }

  addAssureCustomer(typeAssure: string): void {
    this.openCustomerModal(AssureFormStepComponent, {
      entity: null,
      typeAssure,
      header: 'FORMULAIRE DE CREATION DE CLIENT ',
    });
  }

  editAssureCustomer(customer: ICustomer): void {
    this.openCustomerModal(AssureFormStepComponent, {
      entity: customer,
      header: `FORMULAIRE DE MODIFICATION DE CLIENT  [ ${customer.fullName}  ]`,
    });
  }

  addUninsuredCustomer(): void {
    this.openCustomerModal(
      UninsuredCustomerFormComponent,
      {
        entity: null,
        header: 'FORMULAIRE DE CREATION DE CLIENT ',
      },
      '50%',
    );
  }

  editUninsuredCustomer(customer: ICustomer): void {
    this.openCustomerModal(
      UninsuredCustomerFormComponent,
      {
        entity: customer,
        header: `FORMULAIRE DE MODIFICATION DE CLIENT  [ ${customer.fullName}  ]`,
      },
      '50%',
    );
  }

  addAyantDroit(customer: ICustomer): void {
    this.openCustomerModal(
      FormAyantDroitComponent,
      {
        entity: null,
        assure: customer,
      },
      '50%',
      "FORMULAIRE D'AJOUT D'AYANT DROIT ",
    );
  }

  editAyantDroit(customer: ICustomer, ayantDroit: ICustomer): void {
    this.openCustomerModal(
      FormAyantDroitComponent,
      {
        entity: ayantDroit,
        assure: customer,
      },
      '50%',
      `FORMULAIRE DE MODIFICATION D'AYANT DROIT [ ${ayantDroit.fullName}  ]`,
    );
  }

  confirmRemoveAyantDroit(ayantDroit: ICustomer): void {
    this.confirmationService.confirm({
      message: 'Voulez-vous vraiment supprimer cet ayant droit ?',
      rejectButtonProps: rejectButtonProps(),
      acceptButtonProps: acceptButtonProps(),
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
    this.jsonDialog = false;
    this.handleServiceCall(this.customerService.uploadJsonData(formData), () => this.onPocesJsonSuccess(), 'importation');
  }

  onAddNewTiersPayant(customer: ICustomer): void {
    this.openCustomerModal(CustomerTiersPayantComponent, { customer }, '50%', "FORMULAIRE D'AJOUT DE TIERS PAYANT ");
  }

  onEditTiersPayant(customer: ICustomer, clientTiersPayant: IClientTiersPayant): void {
    this.openCustomerModal(
      CustomerTiersPayantComponent,
      {
        entity: clientTiersPayant,
        customer,
      },
      '50%',
      'FORMULAIRE DE MODIFICATION DE TIERS PAYANT [ ' + clientTiersPayant.tiersPayantName + ' ]',
    );
  }

  onRemoveTiersPayant(clientTiersPayant: IClientTiersPayant): void {
    this.confirmationService.confirm({
      message: 'Voulez-vous vraiment supprimer ce tiers payant ?',
      header: 'SUPPRESSION DE TIERS PAYANT',
      rejectButtonProps: rejectButtonProps(),
      acceptButtonProps: acceptButtonProps(),
      icon: 'pi pi-info-circle',
      accept: () => {
        this.handleServiceCall(
          this.customerService.deleteTiersPayant(clientTiersPayant.id),
          () => this.loadPage(),
          'delete-tiers-payant-spinner',
        );
      },
      key: 'deleteTiersPayant',
    });
  }

  protected uploadJsonDataResponse(result: Observable<HttpResponse<void>>): void {
    result.pipe(finalize(() => this.spinner.hide('importation'))).subscribe({
      next: () => this.onPocesJsonSuccess(),
      error: () => this.onImportError(),
    });
  }

  protected onImportError(): void {
    this.spinner.hide('importation');
    this.messageService.add({
      severity: 'error',
      summary: 'Erreur',
      detail: 'Enregistrement a échoué',
    });
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
    })
      .pipe(takeUntil(this.destroy$))
      .subscribe();
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

  protected displayTiersPayantAddBtn(customer: ICustomer): boolean {
    return customer.typeTiersPayant === 'ASSURANCE' && customer.tiersPayants && customer.tiersPayants.length < 4;
  }

  private handleServiceCall(observable: Observable<any>, successCallback: () => void, spinnerName: string): void {
    this.spinner.show(spinnerName);
    observable.pipe(takeUntil(this.destroy$)).subscribe({
      next: () => {
        this.spinner.hide(spinnerName);
        successCallback();
      },
      error: error => {
        this.spinner.hide(spinnerName);
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: error.error?.errorKey ? this.translate.instant(error.error.errorKey) : error.error?.title || 'Erreur interne du serveur.',
        });
      },
    });
  }

  private openCustomerModal(component: any, data: any, width?: string, header?: string): void {
    this.dialogService
      .open(component, {
        data,
        header,
        width: width || '85%',
        closeOnEscape: false,
        maximizable: true,
      })
      .onClose.pipe(takeUntil(this.destroy$))
      .subscribe((resp: ICustomer) => {
        if (resp) {
          this.loadPage();
        }
      });
  }
}
