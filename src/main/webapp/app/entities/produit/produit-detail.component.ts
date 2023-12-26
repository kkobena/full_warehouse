import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { APPEND_TO, ITEMS_PER_PAGE, PRODUIT_COMBO_MIN_LENGTH, PRODUIT_NOT_FOUND } from 'app/shared/constants/pagination.constants';
import { IInventoryTransaction } from 'app/shared/model/inventory-transaction.model';
import moment from 'moment';
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
  produits: IProduit[] = [];
  rowData: any = [];
  typeMouvement: SelectItem[] = [];
  selectedTypeMouvement = -1;
  totalItems = 0;
  itemsPerPage = ITEMS_PER_PAGE;
  page!: number;
  predicate = 'createdAt';
  ascending!: boolean;
  ngbPaginationPage = 1;
  startDate = '';
  endDate = '';
  event: any;
  public columnDefs: any[];
  @ViewChild('quantyBox', { static: false })
  quantyBox?: ElementRef;
  searchValue?: string;
  entites: IInventoryTransaction[] = [];
  protected readonly PRODUIT_COMBO_MIN_LENGTH = PRODUIT_COMBO_MIN_LENGTH;
  protected readonly PRODUIT_NOT_FOUND = PRODUIT_NOT_FOUND;
  protected readonly APPEND_TO = APPEND_TO;
  protected fromDate: Date = new Date();
  protected toDate: Date = new Date();

  constructor(
    protected activatedRoute: ActivatedRoute,
    protected router: Router,
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
    this.loadPage();
  }

  loadProduits(): void {
    this.produitService
      .query({
        page: 0,
        size: 10,
        withdetail: false,
        search: this.searchValue,
      })
      .subscribe((res: HttpResponse<IProduit[]>) => this.onProduitSuccess(res.body));
  }

  searchFn(event: any): void {
    this.searchValue = event.query;
    this.loadProduits();
  }

  loadPage(page?: number): void {
    const pageToLoad: number = page || this.page || 1;
    this.inventoryTransactionService
      .query({
        page: pageToLoad - 1,
        size: this.itemsPerPage,
        sort: this.sort(),
        produitId: this.produit?.id,
        startDate: this.fromDate ? moment(this.fromDate).format('yyyy-MM-DD') : null,
        endDate: this.toDate ? moment(this.toDate).format('yyyy-MM-DD') : null,
        type: this.selectedTypeMouvement,
      })
      .subscribe({
        next: (res: HttpResponse<IInventoryTransaction[]>) => this.onSuccessPage(res.body, res.headers, pageToLoad),
        error: () => this.onError(),
      });
  }

  sort(): string[] {
    const result = [this.predicate + ',' + (this.ascending ? 'asc' : 'desc')];
    if (this.predicate !== 'createdAt') {
      result.push('createdAt');
    }
    return result;
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

  protected onSuccessPage(data: IInventoryTransaction[] | null, headers: HttpHeaders, page: number): void {
    this.totalItems = Number(headers.get('X-Total-Count'));
    this.page = page;

    this.rowData = data || [];
    this.entites = data || [];
    this.ngbPaginationPage = this.page;
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
