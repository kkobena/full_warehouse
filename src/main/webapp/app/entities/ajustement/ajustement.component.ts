import {Component, OnInit} from '@angular/core';
import {HttpHeaders, HttpResponse} from '@angular/common/http';
import {combineLatest, Subscription} from 'rxjs';


import {IAjustement} from 'app/shared/model/ajustement.model';
import {AjustementService} from './ajustement.service';
import moment from 'moment';
import {DD_MM_YYYY_HH_MM} from '../../shared/constants/input.constants';
import {IAjust} from '../../shared/model/ajust.model';
import {ITEMS_PER_PAGE} from '../../shared/constants/pagination.constants';

import {ActivatedRoute, Data, ParamMap, Router} from '@angular/router';

@Component({
  selector: 'jhi-ajustement',
  templateUrl: './ajustement.component.html',
})
export class AjustementComponent implements OnInit {
  ajustements?: IAjustement[];
  eventSubscriber?: Subscription;
  columnDefs: any[];
  rowData: IAjust[] = [];
  totalItems = 0;
  itemsPerPage = ITEMS_PER_PAGE;
  page!: number;
  predicate!: string;
  ascending!: boolean;
  ngbPaginationPage = 1;
  search: string;

  constructor(
    protected ajustementService: AjustementService,
    protected router: Router,
    protected activatedRoute: ActivatedRoute
  ) {
    this.search = '';
    this.columnDefs = [
      {
        headerName: 'Libellé',
        field: 'produitLibelle',
        sortable: true,
        filter: 'agTextColumnFilter',
        minWidth: 300,
        flex: 1.2,
      },
      {
        headerName: 'Date',
        field: 'dateMtv',
        sortable: true,
        valueFormatter: this.formatDate,
      },
      {
        headerName: 'Quantité ajustée',
        field: 'qtyMvt',
        type: ['rightAligned', 'numericColumn'],
      },

      {
        headerName: 'Stock avant ajustement',
        field: 'stockBefore',
        type: ['rightAligned', 'numericColumn'],
        editable: true,
        valueFormatter: this.formatNumber,
      },
      {
        headerName: 'Stock après ajustement',
        field: 'stockAfter',
        type: ['rightAligned', 'numericColumn'],
        valueFormatter: this.formatNumber,
      },
      {
        headerName: 'Opérateur',
        field: 'userFullName',
      },
    ];
  }

  formatDate(date: any): string {
    return moment(date.value).format(DD_MM_YYYY_HH_MM);
  }

  formatNumber(number: any): string {
    return Math.floor(number.value)
      .toString()
      .replace(/(\d)(?=(\d{3})+(?!\d))/g, '$1 ');
  }

  loadAll(): void {
    this.ajustementService.queryAll().subscribe((res: HttpResponse<IAjustement[]>) => (this.rowData = res.body || []));
  }

  ngOnInit(): void {
    this.handleNavigation();
    this.registerChangeInAjustements();
  }


  trackId(index: number, item: IAjustement): number {
    return item.id!;
  }

  registerChangeInAjustements(): void {
    this.loadPage();
  }

  sort(): string[] {
    const result = [this.predicate + ',' + (this.ascending ? 'asc' : 'desc')];
    if (this.predicate !== 'dateMtv') {
      result.push('dateMtv');
    }
    return result;
  }

  loadPage(page?: number, dontNavigate?: boolean): void {
    const pageToLoad: number = page || this.page || 1;

    this.ajustementService
      .queryAjustement({
        page: pageToLoad - 1,
        size: this.itemsPerPage,
        //  sort: this.sort(),
        search: this.search || '',
      })
      .subscribe(
        (res: HttpResponse<IAjust[]>) => this.onSuccess(res.body, res.headers, pageToLoad, !dontNavigate),
        () => this.onError()
      );
  }

  protected onSuccess(data: IAjust[] | null, headers: HttpHeaders, page: number, navigate: boolean): void {
    this.totalItems = Number(headers.get('X-Total-Count'));
    this.page = page;
    if (navigate) {
      this.router.navigate(['/ajustement'], {
        queryParams: {
          page: this.page,
          size: this.itemsPerPage,
          sort: this.predicate + ',' + (this.ascending ? 'asc' : 'desc'),
        },
      });
    }
    this.rowData = data || [];
    this.ngbPaginationPage = this.page;
  }

  protected onError(): void {
    this.ngbPaginationPage = this.page ?? 1;
  }

  protected handleNavigation(): void {
    combineLatest(this.activatedRoute.data, this.activatedRoute.queryParamMap, (data: Data, params: ParamMap) => {
      const page = params.get('page');
      const pageNumber = page !== null ? +page : 1;
      /*const sort = (params.get('sort') ?? data['defaultSort']).split(',');
      const predicate = sort[0];
      const ascending = sort[1] === 'asc';*/
      if (pageNumber !== this.page) {
        // if (pageNumber !== this.page || predicate !== this.predicate || ascending !== this.ascending) {
        //  this.predicate = predicate;
        //  this.ascending = ascending;
        this.loadPage(pageNumber, true);
      }
    }).subscribe();
  }
}
