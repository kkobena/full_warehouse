import { Component, inject, OnInit, signal, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpResponse } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { Table, TableModule } from 'primeng/table';
import { TooltipModule } from 'primeng/tooltip';
import { TagModule } from 'primeng/tag';
import { ToastModule } from 'primeng/toast';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { RippleModule } from 'primeng/ripple';
import { ToolbarModule } from 'primeng/toolbar';
import { SelectModule } from 'primeng/select';
import { DatePicker } from 'primeng/datepicker';
import { FloatLabel } from 'primeng/floatlabel';
import { WarehouseCommonModule } from 'app/shared/warehouse-common/warehouse-common.module';
import { IRetourDepot } from 'app/shared/model/retour-depot.model';
import { RetourStatut } from 'app/shared/model/enumerations/retour-statut.model';
import { RetourDepotService } from '../retour-depot.service';
import { MagasinService } from '../../magasin/magasin.service';
import { IMagasin } from '../../../shared/model/magasin.model';
import { ITEMS_PER_PAGE } from 'app/shared/constants/pagination.constants';
import moment from 'moment';
import { DATE_FORMAT_ISO_DATE } from '../../../shared/util/warehouse-util';

export type ExpandMode = 'single' | 'multiple';

@Component({
  selector: 'jhi-depot-retour-list',
  imports: [
    CommonModule,
    FormsModule,
    ButtonModule,
    TableModule,
    TooltipModule,
    TagModule,
    ToastModule,
    ConfirmDialogModule,
    RippleModule,
    WarehouseCommonModule,
    ToolbarModule,
    SelectModule,
    DatePicker,
    FloatLabel,
    RouterLink,
  ],
  providers: [MessageService, ConfirmationService],
  templateUrl: './depot-retour-list.component.html',
  styleUrl: './depot-retour-list.component.scss',
})
export class DepotRetourListComponent implements OnInit {
  private readonly retourDepotService = inject(RetourDepotService);
  private readonly magasinService = inject(MagasinService);
  private readonly messageService = inject(MessageService);

  @ViewChild('dt') table: Table | undefined;

  protected depots = signal<IMagasin[]>([]);
  protected selectedDepot: IMagasin | null = null;
  protected fromDate: Date | null = new Date();
  protected toDate: Date | null = new Date();
  protected search: string = '';

  protected retourDepots = signal<IRetourDepot[]>([]);
  protected loading = signal<boolean>(false);
  protected totalRecords = signal<number>(0);
  protected itemsPerPage = ITEMS_PER_PAGE;
  protected page = signal<number>(0);

  readonly rowExpandMode: ExpandMode;
  protected readonly RetourStatut = RetourStatut;

  ngOnInit(): void {
    this.loadDepots();
  }

  protected loadDepots(): void {
    this.magasinService.fetchAllDepots().subscribe({
      next: (res: HttpResponse<IMagasin[]>) => {
        this.depots.set(res.body || []);
      },
      error: () => {
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: 'Erreur lors du chargement des dépôts',
        });
      },
    });
  }

  protected onDepotChange(): void {
    this.page.set(0);
    this.loadAll();
  }

  protected onSearch(): void {
    this.page.set(0);
    this.loadAll();
  }

  protected loadAll(): void {
    this.loading.set(true);
    const query: any = {
      page: this.page(),
      size: this.itemsPerPage,
    };

    if (this.fromDate) {
      query.dtStart = DATE_FORMAT_ISO_DATE(this.fromDate);
    }
    if (this.toDate) {
      query.dtEnd = DATE_FORMAT_ISO_DATE(this.toDate);
    }

    if (this.search) {
      query.search = this.search;
    }

    if (this.selectedDepot) {
      query.depotId = this.selectedDepot.id;
    }

    this.retourDepotService.query(query).subscribe({
      next: (res: HttpResponse<IRetourDepot[]>) => {
        this.onSuccess(res.body, res.headers);
      },
      error: () => {
        this.onError();
      },
      complete: () => {
        this.loading.set(false);
      },
    });
  }

  protected onSuccess(data: IRetourDepot[] | null, headers: any): void {
    this.totalRecords.set(Number(headers.get('X-Total-Count')));
    this.retourDepots.set(data || []);
  }

  protected onError(): void {
    this.messageService.add({
      severity: 'error',
      summary: 'Erreur',
      detail: 'Erreur lors du chargement des retours dépôt',
    });
  }

  protected onPageChange(event: any): void {
    this.page.set(event.page);
    this.loadAll();
  }

  protected formatDate(date: string | undefined): string {
    return date ? moment(date).format('DD/MM/YYYY HH:mm') : '';
  }

  protected getTotalItems(retourDepot: IRetourDepot): number {
    return retourDepot.retourDepotItems?.length || 0;
  }

  protected getTotalQuantity(retourDepot: IRetourDepot): number {
    return retourDepot.retourDepotItems?.reduce((sum, item) => sum + (item.qtyMvt || 0), 0) || 0;
  }
}
