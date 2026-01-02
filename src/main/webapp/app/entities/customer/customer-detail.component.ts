import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ISales } from 'app/shared/model/sales.model';
import { ISalesLine } from 'app/shared/model/sales-line.model';
import { ICustomer } from 'app/shared/model/customer.model';
import { CustomerService } from './customer.service';
import { HttpResponse } from '@angular/common/http';
import { MagasinService } from '../magasin/magasin.service';
import { IMagasin } from 'app/shared/model/magasin.model';
import { SalesService } from '../sales/sales.service';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { Button } from 'primeng/button';

@Component({
  selector: 'jhi-customer-detail',

  templateUrl: './customer-detail.component.html',
  styleUrls: ['./customer-detail.component.scss'],
  imports: [WarehouseCommonModule, Button],
})
export class CustomerDetailComponent implements OnInit, OnDestroy {
  customer: ICustomer | null = null;
  sales: ISales[] = [];
  selectedRowIndex?: number;
  selectedRowSaleLines?: ISalesLine[] = [];
  saleSelected?: ISales;
  magasin?: IMagasin;
  protected activatedRoute = inject(ActivatedRoute);
  protected customerService = inject(CustomerService);
  protected magasinService = inject(MagasinService);
  protected salesService = inject(SalesService);
  private destroy$ = new Subject<void>();

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ customer }) => (this.customer = customer));
    this.loadSales();
    this.selectedRowIndex = 0;
    this.magasinService.findCurrentUserMagasin().then(magasin => {
      this.magasin = magasin;
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  previousState(): void {
    window.history.back();
  }

  loadSales(): void {
    this.customerService
      .purchases({
        customerId: this.customer.id,
      })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (res: HttpResponse<ISales[]>) => this.onSuccess(res.body),
        error: () => this.onError(),
      });
  }

  clickRow(item: ISales): void {
    this.selectedRowIndex = item.id;
    this.selectedRowSaleLines = item.salesLines;
    this.saleSelected = item;
  }

  print(): void {
    if (this.saleSelected !== null && this.saleSelected !== undefined) {
      this.salesService
        .print(this.saleSelected.saleId)
        .pipe(takeUntil(this.destroy$))
        .subscribe(blod => {
          const blobUrl = URL.createObjectURL(blod);
          window.open(blobUrl);
        });
    }
  }

  protected onSuccess(data: ISales[] | null): void {
    this.sales = data || [];
  }

  protected onError(): void {}
}
