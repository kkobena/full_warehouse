import { Component, inject, input, OnInit } from '@angular/core';
import { IDelivery } from '../../../../shared/model/delevery.model';
import { ITEMS_PER_PAGE } from '../../../../shared/constants/pagination.constants';
import { DynamicDialogModule, DynamicDialogRef } from 'primeng/dynamicdialog';
import { RouterModule } from '@angular/router';
import { NgxSpinnerModule, NgxSpinnerService } from 'ngx-spinner';
import { DeliveryService } from '../delivery.service';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { WarehouseCommonModule } from '../../../../shared/warehouse-common/warehouse-common.module';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { TooltipModule } from 'primeng/tooltip';
import { CommandeService } from '../../commande.service';

export type ExpandMode = 'single' | 'multiple';

@Component({
  selector: 'jhi-bon-en-cours',
  templateUrl: './bon-en-cours.component.html',
  imports: [WarehouseCommonModule, ButtonModule, TableModule, NgxSpinnerModule, RouterModule, DynamicDialogModule, TooltipModule],
})
export class BonEnCoursComponent implements OnInit {
  protected readonly itemsPerPage = ITEMS_PER_PAGE;
  search = input<string>('');
  protected deliveries: IDelivery[] = [];
  protected readonly rowExpandMode: ExpandMode = 'single';
  protected loading!: boolean;
  protected selectedEl!: any;
  protected page = 0;
  protected ngbPaginationPage = 1;
  protected totalItems = 0;
  protected ref?: DynamicDialogRef;
  protected readonly selectedFilter = 'RECEIVED';
  private readonly commandeService = inject(CommandeService);
  private readonly spinner = inject(NgxSpinnerService);
  private readonly entityService = inject(DeliveryService);

  ngOnInit(): void {
    this.onSearch();
  }

  onRowExpand(event: any): void {
    if (!event.data.orderLines) {
      this.commandeService.fetchOrderLinesByCommandeId(event.data.id).subscribe(res => {
        event.data.orderLines = res.body;
      });
    }
  }

  loadPage(page?: number): void {
    const pageToLoad: number = page || this.page;
    this.loading = true;
    this.commandeService
      .query({
        page: pageToLoad,
        size: this.itemsPerPage,
        search: this.search(),
        orderStatuts: [this.selectedFilter],
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
        const blobUrl = URL.createObjectURL(blod);
        window.open(blobUrl);
      },
      error: () => this.spinner.hide(),
    });
  }

  protected onSuccess(data: IDelivery[] | null, headers: HttpHeaders, page: number): void {
    this.totalItems = Number(headers.get('X-Total-Count'));
    this.page = page;
    /*  this.router.navigate(['/gestion-entree'], {
      queryParams: {
        page: this.page,
        size: this.itemsPerPage,
        search: this.search,
      },
    });*/
    this.deliveries = data || [];
    this.loading = false;
  }

  protected onError(): void {
    this.ngbPaginationPage = this.page ?? 1;
  }
}
