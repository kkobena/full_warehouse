import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ISales } from 'app/shared/model/sales.model';
import { ISalesLine } from 'app/shared/model/sales-line.model';
import { ICustomer } from 'app/shared/model/customer.model';
import { CustomerService } from './customer.service';
import { HttpResponse } from '@angular/common/http';
import { MagasinService } from '../magasin/magasin.service';
import { IMagasin } from 'app/shared/model/magasin.model';
import { SalesService } from '../sales/sales.service';
import { saveAs } from 'file-saver';
@Component({
  selector: 'jhi-customer-detail',
  styles: [
    `
      .table tr:hover {
        cursor: pointer;
      }

      .invoice {
        padding: 0.5rem;
      }

      .invoice .invoice-header {
        display: -ms-flexbox;
        display: flex;
        -ms-flex-pack: justify;
        justify-content: space-between;
      }

      .invoice .invoice-to {
        margin-top: 0rem;
        padding-top: 0.5rem;
        border-top: 1px solid #dee2e6;
      }

      .invoice .invoice-items {
        margin-top: 0.5rem;
        padding-top: 0;
      }

      .invoice .invoice-company .company-name {
        font-weight: 700;
        font-size: 1.2rem;
      }

      .bill-to {
        font-weight: 700;
      }

      .invoice .invoice-company div {
        margin-bottom: 0.1rem;
      }

      .invoice .invoice-title {
        font-size: 1rem;
        margin-bottom: 1rem;
        text-align: right;
      }

      .invoice .invoice-details {
        width: 15rem;
        display: -ms-flexbox;
        display: flex;
        -ms-flex-wrap: wrap;
        flex-wrap: wrap;
      }

      .invoice .invoice-details .invoice-label {
        text-align: left;
        font-weight: 700;
      }

      .invoice .invoice-details .invoice-value {
        text-align: right;
      }

      .invoice .invoice-details > div {
        width: 50%;
        margin-bottom: 0.1rem;
      }

      .invoice .invoice-items table {
        width: 100%;
        border-collapse: collapse;
      }

      .invoice .invoice-items table tr {
        border-bottom: 1px solid #dee2e6;
        line-height: 0.5 !important;
      }

      invoice .invoice-items table th:first-child,
      .invoice .invoice-items table td:first-child {
        text-align: left;
      }

      .invoice .invoice-items table th,
      .invoice .invoice-items table td {
        padding: 1rem;
        text-align: right;
      }

      .invoice .invoice-items table th {
        font-weight: 700;
      }

      .invoice .invoice-items table th,
      .invoice .invoice-items table td {
        padding: 1rem;
        text-align: right;
      }

      .invoice .invoice-items table tr {
        border-bottom: 1px solid #dee2e6;
      }

      .invoice .invoice-items table th:first-child,
      .invoice .invoice-items table td:first-child {
        text-align: left;
      }

      invoice .invoice-items table th,
      .invoice .invoice-items table td {
        padding: 1rem;
        text-align: right;
      }

      .invoice .invoice-summary {
        display: -ms-flexbox;
        display: flex;
        -ms-flex-pack: justify;
        justify-content: space-between;
        margin-top: 0;
        padding-top: 0.5rem;
      }

      .active {
        background-color: #95caf9 !important;
      }

      .master {
        height: 100%;
        padding: 8px 12px;
        border-radius: 12px;
        box-shadow: 0 4px 8px rgb(0 0 0 / 16%);
        display: flex;
        flex-flow: column nowrap;
        justify-content: space-between;
      }
    `,
  ],
  templateUrl: './customer-detail.component.html',
})
export class CustomerDetailComponent implements OnInit {
  customer: ICustomer | null = null;
  sales: ISales[] = [];
  selectedRowIndex?: number;
  selectedRowSaleLines?: ISalesLine[] = [];
  saleSelected?: ISales;
  invoiceConent?: any;
  magasin?: IMagasin;

  constructor(
    protected activatedRoute: ActivatedRoute,
    protected customerService: CustomerService,
    protected magasinService: MagasinService,
    protected salesService: SalesService
  ) {}

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ customer }) => (this.customer = customer));
    this.loadSales();
    this.selectedRowIndex = 0;
    this.magasinService.findPromise().then(magasin => {
      this.magasin = magasin;
    });
  }

  previousState(): void {
    window.history.back();
  }

  protected onSuccess(data: ISales[] | null): void {
    this.sales = data || [];
  }

  protected onError(): void {}

  loadSales(): void {
    this.customerService
      .purchases({
        customerId: this.customer?.id,
      })
      .subscribe(
        (res: HttpResponse<ICustomer[]>) => this.onSuccess(res.body),
        () => this.onError()
      );
  }

  trackId(index: number, item: ISales): number {
    return item.id!;
  }

  clickRow(item: ISales): void {
    this.selectedRowIndex = item.id;
    this.selectedRowSaleLines = item.salesLines;
    this.saleSelected = item;
  }

  print(): void {
    if (this.saleSelected !== null && this.saleSelected !== undefined) {
      this.salesService.print(this.saleSelected.id!).subscribe(blod => saveAs(blod));
    }
  }
}
