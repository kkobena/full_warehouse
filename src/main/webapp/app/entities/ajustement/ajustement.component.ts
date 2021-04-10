import { Component, OnInit, OnDestroy } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { Subscription } from 'rxjs';
import { JhiEventManager } from 'ng-jhipster';

import { IAjustement } from 'app/shared/model/ajustement.model';
import { AjustementService } from './ajustement.service';
import * as moment from 'moment';
import { DD_MM_YYYY_HH_MM } from '../../shared/constants/input.constants';

@Component({
  selector: 'jhi-ajustement',
  templateUrl: './ajustement.component.html',
})
export class AjustementComponent implements OnInit, OnDestroy {
  ajustements?: IAjustement[];
  eventSubscriber?: Subscription;
  columnDefs: any[];
  rowData: any = [];
  constructor(protected ajustementService: AjustementService, protected eventManager: JhiEventManager) {
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
    this.loadAll();
    this.registerChangeInAjustements();
  }

  ngOnDestroy(): void {
    if (this.eventSubscriber) {
      this.eventManager.destroy(this.eventSubscriber);
    }
  }

  trackId(index: number, item: IAjustement): number {
    return item.id!;
  }

  registerChangeInAjustements(): void {
    this.eventSubscriber = this.eventManager.subscribe('ajustementListModification', () => this.loadAll());
  }
}
