import { Component, OnInit, OnDestroy } from '@angular/core';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { ActivatedRoute, ParamMap, Router, Data } from '@angular/router';
import { Subscription, combineLatest } from 'rxjs';
import { JhiEventManager } from 'ng-jhipster';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { ISales } from 'app/shared/model/sales.model';
import { ITEMS_PER_PAGE } from 'app/shared/constants/pagination.constants';
import { SalesService } from './sales.service';
import { SalesDeleteDialogComponent } from './sales-delete-dialog.component';
import { ISalesLine } from 'app/shared/model/sales-line.model';
import { SalesLineService } from '../sales-line/sales-line.service';
import { faPrint } from '@fortawesome/free-solid-svg-icons';
import { saveAs } from 'file-saver';

@Component({
  selector: 'jhi-sales',
  styles: [
    `
      .table tr:hover {
        cursor: pointer;
      }
      .active {
        background-color: #95caf9 !important;
      }
      .master {
        /*padding: 14px 12px;
 border-radius: 12px;*/
        box-shadow: 0 2px 2px rgb(0 0 0 / 16%);
        justify-content: space-between;
      }
      .ag-theme-alpine {
        max-height: 700px;
        height: 600px;
        min-height: 500px;
      }
    `,
  ],
  templateUrl: './sales.component.html',
})
export class SalesComponent implements OnInit, OnDestroy {
  sales?: ISales[];
  eventSubscriber?: Subscription;
  totalItems = 0;
  itemsPerPage = ITEMS_PER_PAGE;
  page!: number;
  predicate!: string;
  ascending!: boolean;
  ngbPaginationPage = 1;
  showBtnDele: boolean;
  saleSelected?: ISales;
  selectedRowIndex?: number;
  public columnDefs: any[];
  rowData: any = [];
  faPrint = faPrint;
  constructor(
    protected salesService: SalesService,
    protected activatedRoute: ActivatedRoute,
    protected router: Router,
    protected eventManager: JhiEventManager,
    protected modalService: NgbModal,
    protected saleLineService: SalesLineService
  ) {
    this.showBtnDele = false;
    this.columnDefs = [
      { headerName: 'Libellé', field: 'produitLibelle', minWidth: 200, flex: 2 },

      {
        headerName: 'Quantité vendue',
        width: 150,
        field: 'quantitySold',
        type: ['rightAligned', 'numericColumn'],
        valueFormatter: this.formatNumber,
      },
      {
        headerName: 'Prix unitaire',
        width: 150,
        field: 'regularUnitPrice',
        type: ['rightAligned', 'numericColumn'],
        valueFormatter: this.formatNumber,
      },
      {
        headerName: 'Montant vente',
        width: 150,
        field: 'salesAmount',
        type: ['rightAligned', 'numericColumn'],
        valueFormatter: this.formatNumber,
      },
    ];
  }

  loadPage(page?: number, dontNavigate?: boolean): void {
    const pageToLoad: number = page || this.page || 1;

    this.salesService
      .query({
        page: pageToLoad - 1,
        size: this.itemsPerPage,
        sort: this.sort(),
      })
      .subscribe(
        (res: HttpResponse<ISales[]>) => this.onSuccess(res.body, res.headers, pageToLoad, !dontNavigate),
        () => this.onError()
      );
  }

  ngOnInit(): void {
    this.handleNavigation();
    this.registerChangeInSales();
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

  trackId(index: number, item: ISales): number {
    // eslint-disable-next-line @typescript-eslint/no-unnecessary-type-assertion
    return item.id!;
  }

  registerChangeInSales(): void {
    this.eventSubscriber = this.eventManager.subscribe('salesListModification', () => this.loadPage());
  }

  delete(sales: ISales): void {
    const modalRef = this.modalService.open(SalesDeleteDialogComponent, { size: 'lg', backdrop: 'static' });
    modalRef.componentInstance.sales = sales;
  }
  print(sales: ISales): void {
    this.salesService.print(sales.id!).subscribe(blod => saveAs(blod));
  }
  sort(): string[] {
    const result = [this.predicate + ',' + (this.ascending ? 'asc' : 'desc')];
    if (this.predicate !== 'updatedAt') {
      result.push('updatedAt');
    }
    return result;
  }

  protected onSuccess(data: ISales[] | null, headers: HttpHeaders, page: number, navigate: boolean): void {
    this.totalItems = Number(headers.get('X-Total-Count'));
    this.page = page;
    if (navigate) {
      this.router.navigate(['/sales'], {
        queryParams: {
          page: this.page,
          size: this.itemsPerPage,
          sort: this.predicate + ',' + (this.ascending ? 'asc' : 'desc'),
        },
      });
    }
    this.sales = data || [];
    this.ngbPaginationPage = this.page;
  }

  protected onError(): void {
    this.ngbPaginationPage = this.page ?? 1;
  }

  clickRow(sale: ISales): void {
    this.selectedRowIndex = sale.id;
    this.saleSelected = sale;

    this.loadLines();
  }
  protected onLineSuccess(data: ISalesLine[] | null): void {
    this.rowData = data || [];
  }
  formatNumber(number: any): string {
    return Math.floor(number.value)
      .toString()
      .replace(/(\d)(?=(\d{3})+(?!\d))/g, '$1  ');
  }
  loadLines(): void {
    this.saleLineService.queryBySale(this.saleSelected?.id).subscribe(
      (res: HttpResponse<ISalesLine[]>) => this.onLineSuccess(res.body),
      () => this.onError()
    );
  }
}
