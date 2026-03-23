import { Component, DestroyRef, inject, OnInit, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { TooltipModule } from 'primeng/tooltip';
import { ToolbarModule } from 'primeng/toolbar';
import { InputTextModule } from 'primeng/inputtext';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { SpinnerComponent } from 'app/shared/spinner/spinner.component';
import { IDelivery } from 'app/shared/model/delevery.model';
import { ITEMS_PER_PAGE } from 'app/shared/constants/pagination.constants';
import { DeliveryService } from '../../../../entities/commande/delevery/delivery.service';
import { CommandeService } from '../../../../entities/commande/commande.service';
import { TauriPrinterService } from 'app/shared/services/tauri-printer.service';
import { handleBlobForTauri } from 'app/shared/util/tauri-util';
import {CommonModule} from "@angular/common";

export type ExpandMode = 'single' | 'multiple';

@Component({
  selector: 'app-bon-en-cours',
  templateUrl: './bon-en-cours.component.html',
  styleUrls: ['./bon-en-cours.scss'],
  imports: [
    FormsModule,
    ButtonModule,
    ToolbarModule,
    InputTextModule,
    IconField,
    InputIcon,
    CommonModule,
    TableModule,
    RouterModule,
    TooltipModule,
    SpinnerComponent,
  ],
})
export class AppBonEnCoursComponent implements OnInit {
  protected search = '';
  protected readonly itemsPerPage = ITEMS_PER_PAGE;
  protected deliveries: IDelivery[] = [];
  protected readonly rowExpandMode: ExpandMode = 'single';
  protected loading = false;
  protected page = 0;
  protected totalItems = 0;

  private readonly destroyRef = inject(DestroyRef);
  private readonly commandeService = inject(CommandeService);
  private readonly entityService = inject(DeliveryService);
  private readonly tauriPrinter = inject(TauriPrinterService);
  private readonly spinner = viewChild.required<SpinnerComponent>('spinner');

  ngOnInit(): void {
    this.onSearch();
  }

  onSearch(): void {
    this.loadPage(0);
  }

  loadPage(page?: number): void {
    const pageToLoad = page ?? this.page;
    this.loading = true;
    this.commandeService
      .query({
        page: pageToLoad,
        size: this.itemsPerPage,
        search: this.search,
        orderStatuts: ['RECEIVED'],
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res: HttpResponse<IDelivery[]>) => this.onSuccess(res.body, res.headers, pageToLoad),
        error: () => (this.loading = false),
      });
  }

  onRowExpand(event: any): void {
    if (!event.data.orderLines) {
      this.commandeService.fetchOrderLinesByCommandeId(event.data.commandeId).subscribe(res => {
        event.data.orderLines = res.body;
      });
    }
  }

  exportPdf(delivery: IDelivery): void {
    this.spinner().show();
    this.entityService
      .exportToPdf(delivery.commandeId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: blob => {
          this.spinner().hide();
          if (this.tauriPrinter.isRunningInTauri()) {
            handleBlobForTauri(blob, 'bon_en_cours');
          } else {
            window.open(URL.createObjectURL(blob));
          }
        },
        error: () => this.spinner().hide(),
      });
  }

  private onSuccess(data: IDelivery[] | null, headers: HttpHeaders, page: number): void {
    this.totalItems = Number(headers.get('X-Total-Count'));
    this.page = page;
    this.deliveries = data ?? [];
    this.loading = false;
  }
}
