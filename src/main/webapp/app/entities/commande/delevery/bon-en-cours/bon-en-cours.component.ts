import { Component, inject, input, OnDestroy, OnInit, viewChild } from '@angular/core';
import { IDelivery } from '../../../../shared/model/delevery.model';
import { ITEMS_PER_PAGE } from '../../../../shared/constants/pagination.constants';
import { RouterModule } from '@angular/router';
import { DeliveryService } from '../delivery.service';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { WarehouseCommonModule } from '../../../../shared/warehouse-common/warehouse-common.module';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { TooltipModule } from 'primeng/tooltip';
import { CommandeService } from '../../commande.service';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { SpinnerComponent } from '../../../../shared/spinner/spinner.component';

export type ExpandMode = 'single' | 'multiple';

@Component({
  selector: 'jhi-bon-en-cours',
  templateUrl: './bon-en-cours.component.html',
  styleUrls: ['./bon-en-cours.component.scss'],
  imports: [WarehouseCommonModule, ButtonModule, TableModule, RouterModule, TooltipModule, SpinnerComponent],
})
export class BonEnCoursComponent implements OnInit, OnDestroy {
  search = input<string>('');
  protected readonly itemsPerPage = ITEMS_PER_PAGE;
  protected deliveries: IDelivery[] = [];
  protected readonly rowExpandMode: ExpandMode = 'single';
  protected loading!: boolean;
  protected selectedEl!: any;
  protected page = 0;
  protected ngbPaginationPage = 1;
  protected totalItems = 0;
  protected readonly selectedFilter = 'RECEIVED';
  private readonly commandeService = inject(CommandeService);
  private readonly spinner = viewChild.required<SpinnerComponent>('spinner');
  private readonly entityService = inject(DeliveryService);
  private destroy$ = new Subject<void>();

  ngOnInit(): void {
    this.onSearch();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  onRowExpand(event: any): void {
    if (!event.data.orderLines) {
      this.commandeService.fetchOrderLinesByCommandeId(event.data.commandeId).subscribe(res => {
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
    this.spinner().show();
    this.entityService
      .exportToPdf(delivery.commandeId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: blod => {
          this.spinner().hide();
          const blobUrl = URL.createObjectURL(blod);
          window.open(blobUrl);
        },
        error: () => this.spinner().hide(),
      });
  }

  protected onSuccess(data: IDelivery[] | null, headers: HttpHeaders, page: number): void {
    this.totalItems = Number(headers.get('X-Total-Count'));
    this.page = page;
    this.deliveries = data || [];
    this.loading = false;
  }

  protected onError(): void {
    this.ngbPaginationPage = this.page ?? 1;
  }
}
