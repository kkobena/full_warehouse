import { Component, inject, OnDestroy, OnInit, viewChild } from '@angular/core';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { ActivatedRoute, Data, ParamMap, Router, RouterLink } from '@angular/router';
import { combineLatest, Observable, Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { ICustomer } from 'app/shared/model/customer.model';
import { ITEMS_PER_PAGE } from 'app/shared/constants/pagination.constants';
import { CustomerService } from './customer.service';
import { MenuItem, MessageService } from 'primeng/api';
import { UninsuredCustomerFormComponent } from './uninsured-customer-form/uninsured-customer-form.component';
import { DialogService } from 'primeng/dynamicdialog';
import { TranslateService } from '@ngx-translate/core';
import { FormAyantDroitComponent } from './form-ayant-droit/form-ayant-droit.component';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { InputTextModule } from 'primeng/inputtext';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { DialogModule } from 'primeng/dialog';
import { FileUploadModule } from 'primeng/fileupload';
import { ToolbarModule } from 'primeng/toolbar';
import { SplitButtonModule } from 'primeng/splitbutton';
import { TooltipModule } from 'primeng/tooltip';
import { CustomerTiersPayantComponent } from './customer-tiers-payant/customer-tiers-payant.component';
import { IClientTiersPayant } from '../../shared/model/client-tiers-payant.model';
import { AssureFormStepComponent } from './assure-form-step/assure-form-step.component';
import { PrimeNG } from 'primeng/config';
import { Select, SelectModule } from 'primeng/select';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { DividerModule } from 'primeng/divider';
import { CustomerCarnetComponent } from './carnet/customer-carnet.component';
import { ConfirmDialogComponent } from '../../shared/dialog/confirm-dialog/confirm-dialog.component';
import { showCommonModal } from '../sales/selling-home/sale-helper';
import { SpinnerComponent } from '../../shared/spinner/spinner.component';
import { Card } from 'primeng/card';
import { FloatLabel } from 'primeng/floatlabel';

@Component({
  selector: 'jhi-customer',
  templateUrl: './customer.component.html',
  styleUrl: './customer.component.scss',
  providers: [DialogService, MessageService],
  imports: [
    WarehouseCommonModule,
    SelectModule,
    ToolbarModule,
    FormsModule,
    ButtonModule,
    RippleModule,
    InputTextModule,
    TableModule,
    DialogModule,
    FileUploadModule,
    DividerModule,
    SplitButtonModule,
    TooltipModule,
    RouterLink,
    Select,
    IconField,
    InputIcon,
    ConfirmDialogComponent,
    SpinnerComponent,
    Card,
    FloatLabel
  ]
})
export class CustomerComponent implements OnInit, OnDestroy {
  translate = inject(TranslateService);
  primeNGConfig = inject(PrimeNG);
  customers?: ICustomer[];
  types: string[] = ['TOUT', 'ASSURE', 'STANDARD'];
  statuts: object[] = [
    { value: 'ENABLE', label: 'Actifs' },
    { value: 'DISABLE', label: 'Désactivés' }
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
  private readonly modalService = inject(NgbModal);
  private destroy$ = new Subject<void>();
  private readonly dialogService = inject(DialogService);
  private readonly spinner = viewChild.required<SpinnerComponent>('spinner');
  private readonly messageService = inject(MessageService);
  private readonly confimDialog = viewChild.required<ConfirmDialogComponent>('confirmDialog');

  constructor() {
    this.translate.use('fr');
    this.translate.stream('primeng').subscribe(data => {
      this.primeNGConfig.setTranslation(data);
    });

    this.newCustomerbuttons = [
      {
        label: 'Assuré',
        icon: 'pi pi-user-plus',
        command: () => this.addAssureCustomer('ASSURANCE')
      },
      {
        label: 'Carnet',
        icon: 'pi pi-user-plus',
        command: () => this.addCarnet('CARNET')
      },
      {
        label: 'Dépôt',
        icon: 'pi pi-user-plus',
        command: () => this.addCarnet('DEPOT')
      },
      {
        label: 'Standard',
        icon: 'pi pi-user-plus',
        command: () => this.addUninsuredCustomer()
      }
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
        status: this.statutSelected
      })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (res: HttpResponse<ICustomer[]>) => this.onSuccess(res.body, res.headers, pageToLoad, dontNavigate),
        error: () => this.onError()
      });
  }

  lazyLoading(event: TableLazyLoadEvent): void {
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
          status: this.statutSelected
        })
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (res: HttpResponse<ICustomer[]>) => this.onSuccess(res.body, res.headers, this.page, false),
          error: () => this.onError()
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
    this.handleServiceCall(this.customerService.delete(customer.id), () => this.loadPage());
  }

  deleteAssuredCustomer(customer: ICustomer): void {
    this.handleServiceCall(this.customerService.deleteAssuredCustomer(customer.id), () => this.loadPage());
  }

  lock(customer: ICustomer): void {
    this.handleServiceCall(this.customerService.lock(customer.id), () => this.loadPage());
  }

  confirmRemove(customer: ICustomer): void {
    this.confimDialog().onConfirm(
      () => {
        if (customer.categorie === 'ASSURE') {
          this.deleteAssuredCustomer(customer);
        } else {
          this.delete(customer);
        }
      },
      'SUPPRESSION DE CLIENT',
      'Voulez-vous vraiment supprimer ce client ?'
    );
  }

  confirmDesactivation(customer: ICustomer): void {
    this.confimDialog().onConfirm(() => this.lock(customer), 'DESACTIVATION DE CLIENT', 'Voulez-vous vraiment désativer ce client ?');
  }

  sort(): string[] {
    const result = [this.predicate + ',' + (this.ascending ? 'asc' : 'desc')];
    if (this.predicate !== 'id') {
      result.push('id');
    }
    return result;
  }

  addCarnet(categorie: string): void {
    showCommonModal(
      this.modalService,
      CustomerCarnetComponent,
      {
        entity: null,
        categorie: categorie,
        title: `FORMULAIRE DE CREATION DE CLIENT [ ${categorie} ]`
      },
      (resp: ICustomer) => {
        if (resp) {
          this.loadPage();
        }
      },
      'xl'
    );
  }

  addAssureCustomer(typeAssure: string): void {
    showCommonModal(
      this.modalService,
      AssureFormStepComponent,
      {
        entity: null,
        typeAssure,
        header: 'FORMULAIRE DE CREATION DE CLIENT '
      },
      (resp: ICustomer) => {
        if (resp) {
          this.loadPage();
        }
      },
      'xl',
      'modal-dialog-80'
    );
  }

  editAssureCustomer(customer: ICustomer): void {
    showCommonModal(
      this.modalService,
      AssureFormStepComponent,
      {
        entity: customer,
        header: `FORMULAIRE DE MODIFICATION DE CLIENT  [ ${customer.fullName}  ]`
      },
      (resp: ICustomer) => {
        if (resp) {
          this.loadPage();
        }
      },
      'xl',
      'modal-dialog-80'
    );
  }

  onRemoveTiersPayant(clientTiersPayant: IClientTiersPayant): void {
    this.confimDialog().onConfirm(
      () => {
        this.handleServiceCall(this.customerService.deleteTiersPayant(clientTiersPayant.id), () => this.loadPage());
      },
      'SUPPRESSION DE TIERS PAYANT',
      'Voulez-vous vraiment supprimer ce tiers payant ?'
    );
  }

  protected addUninsuredCustomer(): void {
    showCommonModal(
      this.modalService,
      UninsuredCustomerFormComponent,
      {
        entity: null,
        title: 'FORMULAIRE DE CREATION DE CLIENT '
      },
      (resp: ICustomer) => {
        if (resp) {
          this.loadPage();
        }
      },
      'xl'
    );
  }

  protected editUninsuredCustomer(customer: ICustomer): void {
    showCommonModal(
      this.modalService,
      UninsuredCustomerFormComponent,
      {
        entity: customer,
        title: `FORMULAIRE DE MODIFICATION DE CLIENT  [ ${customer.fullName}  ]`
      },
      (resp: ICustomer) => {
        if (resp) {
          this.loadPage();
        }
      },
      'xl'
    );
  }

  protected addAyantDroit(customer: ICustomer): void {
    showCommonModal(
      this.modalService,
      FormAyantDroitComponent,
      {
        entity: null,
        assure: customer,
        header: 'FORMULAIRE D\'AJOUT D\'AYANT DROIT '
      },
      (resp: ICustomer) => {
        if (resp) {
          this.loadPage();
        }
      }
    );
  }

  protected editAyantDroit(customer: ICustomer, ayantDroit: ICustomer): void {
    showCommonModal(
      this.modalService,
      FormAyantDroitComponent,
      {
        entity: ayantDroit,
        assure: customer,
        header: `FORMULAIRE DE MODIFICATION D'AYANT DROIT [ ${ayantDroit.fullName}  ]`
      },
      (resp: ICustomer) => {
        if (resp) {
          this.loadPage();
        }
      }
    );
  }

  protected confirmRemoveAyantDroit(ayantDroit: ICustomer): void {
    this.confimDialog().onConfirm(
      () => this.deleteAssuredCustomer(ayantDroit),
      'SUPPRESSION',
      'Voulez-vous vraiment supprimer cet ayant droit ?'
    );
  }

  protected onUploadJson(event: any): void {
    const formData: FormData = new FormData();
    const file = event.files[0];
    formData.append('importjson', file, file.name);
    this.jsonDialog = false;
    this.handleServiceCall(this.customerService.uploadJsonData(formData), () => this.onPocesJsonSuccess());
  }

  protected onAddNewTiersPayant(customer: ICustomer): void {
    showCommonModal(
      this.modalService,
      CustomerTiersPayantComponent,
      {
        entity: null,
        customer: customer,
        header: 'FORMULAIRE D\'AJOUT DE TIERS PAYANT '
      },
      (resp: ICustomer) => {
        if (resp) {
          this.loadPage();
        }
      },
      'xl'
    );
  }

  protected onEditTiersPayant(customer: ICustomer, clientTiersPayant: IClientTiersPayant): void {
    showCommonModal(
      this.modalService,
      CustomerTiersPayantComponent,
      {
        entity: clientTiersPayant,
        customer: customer,
        header: 'FORMULAIRE DE MODIFICATION DE TIERS PAYANT [ ' + clientTiersPayant.tiersPayantName + ' ]'
      },
      (resp: ICustomer) => {
        if (resp) {
          this.loadPage();
        }
      },
      'xl'
    );
  }


  protected onPocesJsonSuccess(): void {
    this.jsonDialog = false;
    this.spinner().hide();
    this.loadPage();
  }

  protected displayTiersPayantAddBtn(customer: ICustomer): boolean {
    return customer.typeTiersPayant === 'ASSURANCE' && customer.tiersPayants && customer.tiersPayants.length < 4;
  }


  private handleNavigation(): void {
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

  private onSuccess(data: ICustomer[] | null, headers: HttpHeaders, page: number, navigate: boolean): void {
    this.totalItems = Number(headers.get('X-Total-Count'));
    this.page = page;
    if (navigate) {
      this.router.navigate(['/customer'], {
        queryParams: {
          page: this.page,
          size: this.itemsPerPage,
          sort: this.predicate + ',' + (this.ascending ? 'asc' : 'desc')
        }
      });
    }
    this.customers = data || [];
    this.ngbPaginationPage = this.page;
    this.loading = false;
  }

  private onError(): void {
    this.loading = false;
    this.ngbPaginationPage = this.page ?? 1;
  }

  private handleServiceCall(observable: Observable<any>, successCallback: () => void): void {
    this.spinner().show();
    observable.pipe(takeUntil(this.destroy$)).subscribe({
      next: () => {
        this.spinner().hide();
        successCallback();
      },
      error: error => {
        this.spinner().hide();
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: error.error?.errorKey ? this.translate.instant(error.error.errorKey) : error.error?.title || 'Erreur interne du serveur.'
        });
      }
    });
  }

  private openCustomerModal(component: any, data: any, width?: string, header?: string): void {
    this.dialogService
      .open(component, {
        data,
        header,
        width: width || '85%',
        closeOnEscape: false,
        maximizable: true
      })
      .onClose.pipe(takeUntil(this.destroy$))
      .subscribe((resp: ICustomer) => {
        if (resp) {
          this.loadPage();
        }
      });
  }
}
