import { Component, Input, OnInit } from '@angular/core';
import { ConfirmationService, LazyLoadEvent, MessageService } from 'primeng/api';
import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';
import { IDelivery } from '../../../shared/model/delevery.model';
import { DeliveryService } from './delivery.service';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { ITEMS_PER_PAGE } from '../../../shared/constants/pagination.constants';
import { Router } from '@angular/router';
import { EtiquetteComponent } from './etiquette/Etiquette.component';
import { ImportDeliveryFormComponent } from './form/import/import-delivery-form.component';
import { ICommandeResponse } from '../../../shared/model/commande-response.model';

import { saveAs } from 'file-saver';
import { NgxSpinnerService } from 'ngx-spinner';

@Component({
  selector: 'jhi-delevery',
  styles: [``],
  templateUrl: './delivery.component.html',
  providers: [ConfirmationService, DialogService, MessageService],
})
export class DeliveryComponent implements OnInit {
  deliveries: IDelivery[] = [];

  rowExpandMode = 'single';
  loading!: boolean;
  selectedEl!: any;
  itemsPerPage = ITEMS_PER_PAGE;
  search = '';
  page = 0;
  ngbPaginationPage = 1;
  totalItems = 0;
  @Input() activeIndex: number;
  ref?: DynamicDialogRef;
  filtres: any[] = [];
  selectedFilter = 'ANY';
  loadingSelectedFilter = false;

  constructor(
    protected router: Router,
    private spinner: NgxSpinnerService,
    protected entityService: DeliveryService,
    private dialogService: DialogService
  ) {}

  ngOnInit(): void {
    this.filtres = [
      { label: 'TOUS', value: 'ANY' },
      { label: 'EN COUR DE TRAITEMENT', value: 'PENDING' },
      { label: 'SOLDE', value: 'PAID' },
      { label: 'REGLEMENT PARTIEL', value: 'NOT_SOLD' },
      { label: 'NON REGLE', value: 'UNPAID' },
    ];
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

  lazyLoading(event: LazyLoadEvent): void {
    if (event) {
      this.page = event.first! / event.rows!;
      this.loading = true;
      this.entityService
        .query({
          page: this.page,
          size: event.rows,
          search: this.search,
          statut: this.selectedFilter,
        })
        .subscribe({
          next: (res: HttpResponse<IDelivery[]>) => this.onSuccess(res.body, res.headers, this.page),
          error: () => this.onError(),
        });
    }
  }

  printEtiquette(delivery: IDelivery): void {
    this.ref = this.dialogService.open(EtiquetteComponent, {
      data: { entity: delivery },
      width: '40%',
      header: `IMPRIMER LES ETIQUETTES DU BON DE LIVRAISON [ ${delivery.receiptRefernce} ] `,
    });
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

  onImportNew(): void {
    this.ref = this.dialogService.open(ImportDeliveryFormComponent, {
      header: 'IMPORTATION DE NOUVEAU BON DE LIVRAISON',
      width: '40%',
    });
    this.ref.onClose.subscribe((response: ICommandeResponse) => {
      if (response) {
        this.gotoEntreeStockComponent(response.entity.id);
      }
    });
  }

  gotoEntreeStockComponent(delivery: IDelivery): void {
    this.router.navigate(['/gestion-entree', delivery.id, 'stock-entry']);
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
