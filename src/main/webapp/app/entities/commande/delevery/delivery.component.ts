import { Component, OnInit } from '@angular/core';
import { ConfirmationService, LazyLoadEvent, MenuItem } from 'primeng/api';
import { DialogService } from 'primeng/dynamicdialog';
import { IDelivery } from '../../../shared/model/delevery.model';
import { DeliveryService } from './delivery.service';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { ICommande } from '../../../shared/model/commande.model';
import { ITEMS_PER_PAGE } from '../../../shared/constants/pagination.constants';
import { Router } from '@angular/router';

@Component({
  selector: 'jhi-delevery',
  styles: [``],
  templateUrl: './delivery.component.html',
  providers: [ConfirmationService, DialogService],
})
export class DeliveryComponent implements OnInit {
  deliveries: IDelivery[] = [];
  commandebuttons: MenuItem[];
  rowExpandMode = 'single';
  loading!: boolean;
  selectedEl!: any;
  itemsPerPage = ITEMS_PER_PAGE;
  search = '';
  page = 0;
  ngbPaginationPage = 1;
  totalItems = 0;

  constructor(protected router: Router, protected entityService: DeliveryService) {}

  loadPage(page?: number): void {
    const pageToLoad: number = page || this.page;
    this.loading = true;
    this.entityService
      .query({
        page: pageToLoad,
        size: this.itemsPerPage,
        search: this.search,
      })
      .subscribe({
        next: (res: HttpResponse<IDelivery[]>) => this.onSuccess(res.body, res.headers, pageToLoad),
        error: () => this.onError(),
      });
  }

  ngOnInit(): void {
    this.loadPage();
  }

  onSearch(): void {
    this.loadPage(0);
  }

  lazyLoading(event: LazyLoadEvent): void {
    if (event) {
      this.page = event.first! / event.rows!;
      this.loading = true;
      this.entityService
        .query({
          page: this.page,
          size: event.rows,
          search: this.search,
        })
        .subscribe({
          next: (res: HttpResponse<IDelivery[]>) => this.onSuccess(res.body, res.headers, this.page),
          error: () => this.onError(),
        });
    }
  }

  protected onSuccess(data: ICommande[] | null, headers: HttpHeaders, page: number): void {
    this.totalItems = Number(headers.get('X-Total-Count'));
    this.page = page;
    this.router.navigate(['/commande'], {
      queryParams: {
        page: this.page,
        size: this.itemsPerPage,
        search: this.search,
      },
    });
    this.deliveries = data || [];
    this.loading = false;
  }

  protected onError(): void {
    this.ngbPaginationPage = this.page ?? 1;
  }
}
