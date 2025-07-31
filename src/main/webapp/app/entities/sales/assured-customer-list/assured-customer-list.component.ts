import { Component, inject } from '@angular/core';
import { LazyLoadEvent } from 'primeng/api';
import { ICustomer } from '../../../shared/model/customer.model';
import { CustomerService } from '../../customer/customer.service';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { RippleModule } from 'primeng/ripple';
import { TableModule } from 'primeng/table';
import { TooltipModule } from 'primeng/tooltip';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { ITEMS_PER_PAGE } from '../../../shared/constants/pagination.constants';
import { CurrentSaleService } from '../service/current-sale.service';
import { ISales } from '../../../shared/model/sales.model';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';

@Component({
  selector: 'jhi-assured-customer-list',
  templateUrl: './assured-customer-list.component.html',
  imports: [
    WarehouseCommonModule,
    FormsModule,
    TooltipModule,
    ButtonModule,
    InputTextModule,
    RippleModule,
    TableModule,
    IconField,
    InputIcon,
  ],
})
export class AssuredCustomerListComponent {
  customers: ICustomer[] = [];
  searchString?: string | null = '';
  headerLibelle: string;
  protected itemsPerPage = ITEMS_PER_PAGE;
  protected page!: number;
  protected ngbPaginationPage = 1;
  protected totalItems = 0;
  protected loading!: boolean;
  private readonly currentSaleService = inject(CurrentSaleService);
  private readonly customerService = inject(CustomerService);
  private readonly activeModal = inject(NgbActiveModal);

  protected onDbleClick(customer: ICustomer): void {
    this.onSelect(customer);
  }

  protected onSelect(customer: ICustomer): void {
    this.activeModal.close(customer);
  }

  protected cancel(): void {
    this.activeModal.dismiss();
  }

  /*  addAssureCustomer(): void {

     this.ref = this.dialogService.open(AssureFormStepComponent, {
       data: { entity: null, typeAssure: this.currentSaleService.typeVo() },
       header: 'FORMULAIRE DE CREATION DE CLIENT ',
       width: '85%',
       closeOnEscape: false,
       //  modal: true,
       //  focusOnShow: false,
     });
     this.ref.onClose.subscribe((customer: ICustomer) => {
       this.ref.close(customer);
     });
   } */

  protected loadPage(page?: number): void {
    const pageToLoad: number = page || this.page || 1;
    this.customerService
      .queryAssuredCustomer({
        page: pageToLoad - 1,
        size: this.itemsPerPage,
        search: this.searchString,
        typeTiersPayant: this.currentSaleService.typeVo(),
      })
      .subscribe({
        next: (res: HttpResponse<ICustomer[]>) => this.onSuccess(res.body, res.headers, pageToLoad),
        error: () => this.onError(),
      });
  }

  protected lazyLoading(event: LazyLoadEvent): void {
    if (event) {
      this.page = event.first / event.rows;
      this.loading = true;
      this.customerService
        .queryAssuredCustomer({
          page: this.page,
          size: event.rows,
          search: this.searchString,
          typeTiersPayant: this.currentSaleService.typeVo(),
        })
        .subscribe({
          next: (res: HttpResponse<ISales[]>) => this.onSuccess(res.body, res.headers, this.page),
          error: () => this.onError(),
        });
    }
  }

  private onSuccess(data: ICustomer[] | null, headers: HttpHeaders, page: number): void {
    this.totalItems = Number(headers.get('X-Total-Count'));

    this.page = page;
    this.customers = data || [];
    this.ngbPaginationPage = this.page;
    this.loading = false;
  }

  private onError(): void {
    this.ngbPaginationPage = this.page ?? 1;
    this.loading = false;
  }
}
