import { Component, inject, input, OnInit, viewChild } from '@angular/core';
import { IDelivery } from '../../../../shared/model/delevery.model';
import { ITEMS_PER_PAGE } from '../../../../shared/constants/pagination.constants';
import { RouterModule } from '@angular/router';
import { DeliveryService } from '../delivery.service';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { EtiquetteComponent } from '../etiquette/etiquette.component';
import { WarehouseCommonModule } from '../../../../shared/warehouse-common/warehouse-common.module';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { TooltipModule } from 'primeng/tooltip';
import { showCommonModal } from '../../../sales/selling-home/sale-helper';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { SpinnerComponent } from '../../../../shared/spinner/spinner.component';
import { TauriPrinterService } from '../../../../shared/services/tauri-printer.service';
import { handleBlobForTauri } from '../../../../shared/util/tauri-util';

export type ExpandMode = 'single' | 'multiple';

@Component({
  selector: 'jhi-list-bons',
  templateUrl: './list-bons.component.html',
  styleUrl: './list-bons.component.scss',
  imports: [WarehouseCommonModule, ButtonModule, RouterModule, RippleModule, TableModule, TooltipModule, SpinnerComponent],
})
export class ListBonsComponent implements OnInit {
  readonly search = input('');
  protected deliveries: IDelivery[] = [];
  protected rowExpandMode: ExpandMode = 'single';
  protected loading!: boolean;
  protected selectedEl!: any;
  protected itemsPerPage = ITEMS_PER_PAGE;
  protected page = 0;
  protected ngbPaginationPage = 1;
  protected totalItems = 0;
  protected selectedFilter = 'CLOSED';
  private readonly entityService = inject(DeliveryService);
  private readonly spinner = viewChild.required<SpinnerComponent>('spinner');
  private readonly modalService = inject(NgbModal);
  private readonly tauriPrinterService = inject(TauriPrinterService);
  ngOnInit(): void {
    this.onSearch();
  }

  loadPage(page?: number): void {
    const pageToLoad: number = page || this.page;
    this.fetchDeliveries(pageToLoad, this.itemsPerPage);
  }

  onSearch(): void {
    this.loadPage(0);
  }

  lazyLoading(event: TableLazyLoadEvent): void {
    if (event) {
      this.page = event.first / event.rows;
      this.itemsPerPage = event.rows;
      this.fetchDeliveries(this.page, this.itemsPerPage);
    }
  }

  printEtiquette(delivery: IDelivery): void {
    showCommonModal(
      this.modalService,
      EtiquetteComponent,
      {
        entity: delivery,
        header: `IMPRIMER LES ETIQUETTES DU BON DE LIVRAISON [ ${delivery.receiptReference} ] `,
      },
      () => {},
      'lg',
    );
  }

  exportPdf(delivery: IDelivery): void {
    this.spinner().show();
    this.entityService.exportToPdf(delivery.commandeId).subscribe({
      next: blob => {
        this.spinner().hide();

        if (this.tauriPrinterService.isRunningInTauri()) {
          handleBlobForTauri(blob, 'liste-bons-livraison');
        } else {
          window.open(URL.createObjectURL(blob));
        }
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

  private fetchDeliveries(page: number, size: number): void {
    this.loading = true;
    this.entityService
      .query({
        page,
        size,
        search: this.search(),
        statut: this.selectedFilter,
      })
      .subscribe({
        next: (res: HttpResponse<IDelivery[]>) => this.onSuccess(res.body, res.headers, page),
        error: () => this.onError(),
      });
  }
}
