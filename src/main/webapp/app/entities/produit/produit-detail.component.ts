import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ITEMS_PER_PAGE } from 'app/shared/constants/pagination.constants';
import { IInventoryTransaction } from 'app/shared/model/inventory-transaction.model';
import * as moment from 'moment';
import { IProduit } from 'app/shared/model/produit.model';
import { InventoryTransactionService } from '../inventory-transaction/inventory-transaction.service';
import { ProduitService } from './produit.service';
import { DD_MM_YYYY_HH_MM } from 'app/shared/constants/input.constants';
import { SelectItem } from 'primeng/api';
@Component({
  selector: 'jhi-produit-detail',
  styles: [
    `
      .ag-theme-alpine {
        max-height: 700px;
        height: 500px;
        min-height: 400px;
      }
    `,
  ],
  templateUrl: './produit-detail.component.html',
})
export class ProduitDetailComponent implements OnInit {
  produit: IProduit | null = null;
  produitSelected!: IProduit | null;
  produits: IProduit[] = [];
  rowData: any = [];
  typeMouvement: SelectItem[] = [];
  selectedTypeMouvement = -1;
  totalItems = 0;
  itemsPerPage = ITEMS_PER_PAGE;
  page!: number;
  predicate!: string;
  ascending!: boolean;
  ngbPaginationPage = 1;
  startDate = '';
  endDate = '';
  event: any;
  public columnDefs: any[];
  @ViewChild('quantyBox', { static: false })
  quantyBox?: ElementRef;
  constructor(
    protected activatedRoute: ActivatedRoute,
    protected inventoryTransactionService: InventoryTransactionService,
    protected produitService: ProduitService
  ) {
    this.columnDefs = [
      {
        headerName: 'Date mouvement',
        field: 'updatedAt',
        sortable: true,
        flex: 1,
        valueFormatter: this.formatDate,
      },
      {
        headerName: 'Type mouvement',
        field: 'transactionType',
        sortable: true,
        flex: 1,
      },
      {
        headerName: 'Quantité mouvement',
        field: 'quantity',
        flex: 1,
        type: ['rightAligned', 'numericColumn'],
      },
      {
        headerName: 'Quantité avant',
        flex: 1,
        field: 'quantityBefor',
        type: ['rightAligned', 'numericColumn'],
      },
      {
        headerName: 'Quantité après',
        flex: 1,
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
    this.typeMouvement.push({ label: 'TOUT', value: -1 });
    this.populate();
  }

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ produit }) => (this.produit = produit));
    this.loadPage();
    this.loadProduits();
  }

  previousState(): void {
    window.history.back();
  }
  formatDate(date: any): string {
    return moment(date.value).format(DD_MM_YYYY_HH_MM);
  }
  onSelect(event: any): void {
    this.event = event;
    if (this.quantyBox) {
      this.quantyBox.nativeElement.focus();
    }
  }
  loadProduits(): void {
    this.produitService
      .query({
        page: 0,
        size: 9999,
        withdetail: true,
      })
      .subscribe((res: HttpResponse<IProduit[]>) => this.onProduitSuccess(res.body));
  }

  loadPage(): void {
    this.inventoryTransactionService
      .query({
        produitId: this.produit?.id,
        startDate: this.startDate,
        endDate: this.endDate,
        type: this.selectedTypeMouvement,
      })
      .subscribe(
        (res: HttpResponse<IInventoryTransaction[]>) => this.onSuccess(res.body, res.headers),
        () => this.onError()
      );
  }
  filtreTypeMouvement(event: any): void {
    this.selectedTypeMouvement = event.value;
    this.loadPage();
  }
  async populate(): Promise<void> {
    const result = await this.inventoryTransactionService.allTypeTransaction();
    result.forEach(e => {
      this.typeMouvement.push({ label: e.name, value: e.value });
    });
  }
  protected onSuccess(data: IInventoryTransaction[] | null, headers: HttpHeaders): void {
    this.totalItems = Number(headers.get('X-Total-Count'));
    this.rowData = data || [];
  }

  protected onError(): void {
    this.ngbPaginationPage = this.page ?? 1;
  }
  protected onProduitSuccess(data: IProduit[] | null): void {
    this.produits = data || [];
  }
}
