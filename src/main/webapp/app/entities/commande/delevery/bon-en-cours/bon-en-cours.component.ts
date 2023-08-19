import { Component, Input, OnInit } from '@angular/core';
import { IDelivery } from '../../../../shared/model/delevery.model';
import { ITEMS_PER_PAGE } from '../../../../shared/constants/pagination.constants';
import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';
import { Router } from '@angular/router';
import { NgxSpinnerService } from 'ngx-spinner';
import { DeliveryService } from '../delivery.service';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { saveAs } from 'file-saver';

@Component({
  selector: 'jhi-bon-en-cours',
  templateUrl: './bon-en-cours.component.html',
})
export class BonEnCoursComponent implements OnInit {
  @Input() search = '';
  protected deliveries: IDelivery[] = [];
  protected rowExpandMode = 'single';
  protected loading!: boolean;
  protected selectedEl!: any;
  protected itemsPerPage = ITEMS_PER_PAGE;
  protected page = 0;
  protected ngbPaginationPage = 1;
  protected totalItems = 0;
  protected ref?: DynamicDialogRef;
  protected selectedFilter = 'PENDING';
  protected loadingSelectedFilter = false;

  constructor(
    protected router: Router,
    private spinner: NgxSpinnerService,
    protected entityService: DeliveryService,
    private dialogService: DialogService
  ) {}

  ngOnInit(): void {
    this.onSearch();
  }

  loadPage(page?: number): void {
    const pageToLoad: number = page || this.page;
    this.loading = true;
    this.entityService
      .query({
        page: pageToLoad,
        size: this.itemsPerPage,
        search: this.search,
        statut: this.selectedFilter,
      })
      .subscribe({
        next: (res: HttpResponse<IDelivery[]>) => this.onSuccess(res.body, res.headers, pageToLoad),
        error: () => this.onError(),
      });
  }

  onSearch(): void {
    this.loadPage(0);
  }

  exportPdf(delivery: IDelivery): void {
    this.spinner.show();
    this.entityService.exportToPdf(delivery.id).subscribe({
      next: blod => {
        this.spinner.hide();
        saveAs(blod);
      },
      error: () => this.spinner.hide(),
    });
  }

  protected onSuccess(data: IDelivery[] | null, headers: HttpHeaders, page: number): void {
    this.totalItems = Number(headers.get('X-Total-Count'));
    this.page = page;
    this.router.navigate(['/gestion-entree'], {
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
