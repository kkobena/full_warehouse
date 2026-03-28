import { Component, inject, OnInit } from '@angular/core';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';
import { SelectModule } from 'primeng/select';
import { DatePicker } from 'primeng/datepicker';
import { FloatLabel } from 'primeng/floatlabel';
import { InputTextModule } from 'primeng/inputtext';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { CommonModule } from '@angular/common';
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

@Component({
  selector: 'app-list-bons',
  templateUrl: './list-bons.component.html',
  styleUrls: ['./list-bons.scss'],
  imports: [
    CommonModule,
    FormsModule,
    ButtonModule,
    TooltipModule,
    SelectModule,
    DatePicker,
    FloatLabel,
    InputTextModule,
    IconField,
    InputIcon,
    SpinnerComponent,
  ],
})
export class AppListBonsComponent implements OnInit {
  protected search = '';
  protected selectFournisseurId: number | null = null;
  protected dtStart: Date | null = new Date();
  protected dtEnd: Date | null = new Date();
  protected selectedStatut: string | null = null;
  protected fournisseurs: IFournisseur[] = [];
  protected deliveries: IDelivery[] = [];
  protected loading = false;
  protected itemsPerPage = ITEMS_PER_PAGE;
  protected page = 0;
  protected totalItems = 0;

  protected readonly expandedIds = new Set<number>();

  protected readonly statutOptions = [
    { label: 'Tous les bons', value: null },
    { label: 'En attente de saisie', value: 'RECEIVED' },
    { label: 'Clôturé', value: 'CLOSED' },
  ];

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

  loadPage(page: number): void {
    this.expandedIds.clear();
    this.fetchDeliveries(page, this.itemsPerPage);
  }

  onFournisseurChange(event: any): void {
    this.selectFournisseurId = event.value ?? null;
    setTimeout(() => this.onSearch(), 50);
  }

  protected toggle(id: number): void {
    if (this.expandedIds.has(id)) {
      this.expandedIds.delete(id);
    } else {
      this.expandedIds.add(id);
    }
  }

  protected isExpanded(id: number): boolean {
    return this.expandedIds.has(id);
  }

  protected isReceived(delivery: IDelivery): boolean {
    return delivery.orderStatus === 'RECEIVED' || (delivery as any).statut === 'RECEIVED';
  }

  protected get totalPages(): number {
    return Math.max(1, Math.ceil(this.totalItems / this.itemsPerPage));
  }

  goToPage(p: number): void {
    if (p < 0 || p >= this.totalPages) return;
    this.loadPage(p);
  }

  printEtiquette(delivery: IDelivery, event: Event): void {
    event.stopPropagation();
    showCommonModal(
      this.modalService,
      EtiquetteComponent,
      {
        entity: delivery,
        header: `IMPRIMER LES ETIQUETTES DU BON DE LIVRAISON [ ${(delivery as any).receiptReference} ] `,
      },
      () => {},
      'lg',
    );
  }

  exportPdf(delivery: IDelivery, event: Event): void {
    event.stopPropagation();
    this.spinner().show();
    this.entityService.exportToPdf(delivery.commandeId).subscribe({
      next: blob => {
        this.spinner().hide();
        if (this.tauriPrinterService.isRunningInTauri()) {
          handleBlobForTauri(blob, 'bon-livraison');
        } else {
          window.open(URL.createObjectURL(blob));
        }
      },
      error: () => this.spinner().hide(),
    });
  }

  private fetchDeliveries(page: number, size: number): void {
    this.loading = true;
    const query: any = { page, size, search: this.search };
    if (this.selectedStatut) query.statut = this.selectedStatut;
    if (this.selectFournisseurId) query.fournisseurId = this.selectFournisseurId;
    if (this.dtStart) query.fromDate = DATE_FORMAT_ISO_DATE(this.dtStart);
    if (this.dtEnd) query.toDate = DATE_FORMAT_ISO_DATE(this.dtEnd);
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
