import { Component, inject, OnInit } from '@angular/core';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { TooltipModule } from 'primeng/tooltip';
import { ToolbarModule } from 'primeng/toolbar';
import { InputTextModule } from 'primeng/inputtext';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { SelectModule } from 'primeng/select';
import { DatePicker } from 'primeng/datepicker';
import { FloatLabel } from 'primeng/floatlabel';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { SpinnerComponent } from 'app/shared/spinner/spinner.component';
import { IDelivery } from 'app/shared/model/delevery.model';
import { IFournisseur } from 'app/shared/model/fournisseur.model';
import { ITEMS_PER_PAGE } from 'app/shared/constants/pagination.constants';
import { DATE_FORMAT_ISO_DATE } from 'app/shared/util/warehouse-util';
import { DeliveryService } from '../../../../entities/commande/delevery/delivery.service';
import { FournisseurService } from '../../../../entities/fournisseur/fournisseur.service';
import { TauriPrinterService } from 'app/shared/services/tauri-printer.service';
import { handleBlobForTauri } from 'app/shared/util/tauri-util';
import { showCommonModal } from '../../../../entities/sales/selling-home/sale-helper';
import { EtiquetteComponent } from '../delivery/etiquette/etiquette.component';
import { viewChild } from '@angular/core';
import {CommonModule} from "@angular/common";

export type ExpandMode = 'single' | 'multiple';

@Component({
  selector: 'app-list-bons',
  templateUrl: './list-bons.component.html',
  styleUrls: ['./list-bons.scss'],
  imports: [
    FormsModule,
    ButtonModule,
    ToolbarModule,
    InputTextModule,
    IconField,
    InputIcon,
    SelectModule,
    DatePicker,
    FloatLabel,
    CommonModule,
    RouterModule,
    RippleModule,
    TableModule,
    TooltipModule,
    SpinnerComponent,
  ],
})
export class AppListBonsComponent implements OnInit {
  protected search = '';
  protected selectFournisseurId: number | null = null;
  protected dtStart: Date | null = new Date();
  protected dtEnd: Date | null = new Date();
  protected fournisseurs: IFournisseur[] = [];
  protected deliveries: IDelivery[] = [];
  protected rowExpandMode: ExpandMode = 'single';
  protected loading = false;
  protected itemsPerPage = ITEMS_PER_PAGE;
  protected page = 0;
  protected totalItems = 0;

  private readonly entityService = inject(DeliveryService);
  private readonly fournisseurService = inject(FournisseurService);
  private readonly modalService = inject(NgbModal);
  private readonly tauriPrinterService = inject(TauriPrinterService);
  private readonly spinner = viewChild.required<SpinnerComponent>('spinner');

  ngOnInit(): void {
    this.fournisseurService
      .query({ page: 0, size: 999 })
      .subscribe((res: HttpResponse<IFournisseur[]>) => (this.fournisseurs = res.body ?? []));
    this.onSearch();
  }

  onSearch(): void {
    this.loadPage(0);
  }

  loadPage(page?: number): void {
    const pageToLoad = page ?? this.page;
    this.fetchDeliveries(pageToLoad, this.itemsPerPage);
  }

  lazyLoading(event: TableLazyLoadEvent): void {
    if (event) {
      this.page = event.first / event.rows;
      this.itemsPerPage = event.rows;
      this.fetchDeliveries(this.page, this.itemsPerPage);
    }
  }

  onFournisseurChange(event: any): void {
    this.selectFournisseurId = event.value ?? null;
    setTimeout(() => this.onSearch(), 50);
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

  private fetchDeliveries(page: number, size: number): void {
    this.loading = true;
    const query: any = {
      page,
      size,
      search: this.search,
      statut: 'CLOSED',
    };
    if (this.selectFournisseurId) {
      query.fournisseurId = this.selectFournisseurId;
    }
    if (this.dtStart) {
      query.fromDate = DATE_FORMAT_ISO_DATE(this.dtStart);
    }
    if (this.dtEnd) {
      query.toDate = DATE_FORMAT_ISO_DATE(this.dtEnd);
    }
    this.entityService.query(query).subscribe({
      next: (res: HttpResponse<IDelivery[]>) => this.onSuccess(res.body, res.headers, page),
      error: () => (this.loading = false),
    });
  }

  private onSuccess(data: IDelivery[] | null, headers: HttpHeaders, page: number): void {
    this.totalItems = Number(headers.get('X-Total-Count'));
    this.page = page;
    this.deliveries = data ?? [];
    this.loading = false;
  }
}
