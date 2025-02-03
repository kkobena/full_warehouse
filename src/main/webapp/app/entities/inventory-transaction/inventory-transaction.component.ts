import { Component, OnInit, inject } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';

import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { IInventoryTransaction } from 'app/shared/model/inventory-transaction.model';
import { ITEMS_PER_PAGE } from 'app/shared/constants/pagination.constants';
import { InventoryTransactionService } from './inventory-transaction.service';
import moment from 'moment';
import { DD_MM_YYYY_HH_MM } from 'app/shared/constants/input.constants';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';
import { AgGridAngular } from 'ag-grid-angular';
import { AllCommunityModule, ClientSideRowModelModule, ModuleRegistry, provideGlobalGridOptions, themeAlpine } from 'ag-grid-community';

ModuleRegistry.registerModules([AllCommunityModule, ClientSideRowModelModule]);
provideGlobalGridOptions({ theme: themeAlpine });

@Component({
  selector: 'jhi-inventory-transaction',
  styles: [
    `
      .master {
        padding: 14px 12px;
        border-radius: 12px;
        box-shadow: 0 4px 8px rgb(0 0 0 / 16%);
        justify-content: space-between;
      }

      .ag-theme-alpine {
        max-height: 700px;
        height: 500px;
        min-height: 400px;
      }
    `,
  ],
  templateUrl: './inventory-transaction.component.html',
  imports: [WarehouseCommonModule, AgGridAngular],
})
export class InventoryTransactionComponent implements OnInit {
  protected inventoryTransactionService = inject(InventoryTransactionService);
  protected activatedRoute = inject(ActivatedRoute);
  protected router = inject(Router);
  protected modalService = inject(NgbModal);

  totalItems = 0;
  itemsPerPage = ITEMS_PER_PAGE;
  page!: number;
  predicate!: string;
  ascending!: boolean;
  ngbPaginationPage = 1;
  rowData: any = [];
  columnDefs: any[];

  /** Inserted by Angular inject() migration for backwards compatibility */
  constructor(...args: unknown[]);

  constructor() {
    this.columnDefs = [
      {
        headerName: 'Article',
        field: 'produitLibelle',
        sortable: true,
        flex: 1.5,
      },
      {
        headerName: 'Date mouvement',
        field: 'updatedAt',
        sortable: true,
        flex: 0.6,
        valueFormatter: this.formatDate,
      },
      {
        headerName: 'Type mouvement',
        field: 'transactionType',
        sortable: true,
        flex: 0.5,
      },
      {
        headerName: 'Quantité mouvement',
        field: 'quantity',
        flex: 0.6,
        type: ['rightAligned', 'numericColumn'],
      },
      {
        headerName: 'Quantité avant',
        flex: 0.5,
        field: 'quantityBefor',
        type: ['rightAligned', 'numericColumn'],
      },
      {
        headerName: 'Quantité après',
        flex: 0.5,
        field: 'quantityAfter',
        type: ['rightAligned', 'numericColumn'],
      },
      {
        headerName: 'Opérateur',
        field: 'userFullName',
        sortable: true,
        flex: 1,
      },
    ];
  }

  ngOnInit(): void {
    this.loadPage();
  }

  loadPage(): void {
    this.inventoryTransactionService.query().subscribe({
      next: (res: HttpResponse<IInventoryTransaction[]>) => this.onSuccess(res.body),
      error: () => this.onError(),
    });
  }

  formatDate(date: any): string {
    return moment(date.value).format(DD_MM_YYYY_HH_MM);
  }

  protected onSuccess(data: IInventoryTransaction[] | null): void {
    this.rowData = data || [];
  }

  protected onError(): void {
    this.ngbPaginationPage = this.page ?? 1;
  }
}
