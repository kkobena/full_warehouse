import { Component, OnInit, OnDestroy } from '@angular/core';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { Observable, Subscription } from 'rxjs';
import { JhiEventManager, JhiParseLinks } from 'ng-jhipster';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';

import { IStoreInventory } from 'app/shared/model/store-inventory.model';

import { ITEMS_PER_PAGE } from 'app/shared/constants/pagination.constants';
import { StoreInventoryService } from './store-inventory.service';
import { StoreInventoryDeleteDialogComponent } from './store-inventory-delete-dialog.component';
import { NgxSpinnerService } from 'ngx-spinner';

@Component({
  selector: 'jhi-store-inventory',
  templateUrl: './store-inventory.component.html',
  styles: [
    `
      .table tr:hover {
        cursor: pointer;
      }
      .active {
        background-color: #95caf9 !important;
      }
      .ag-theme-alpine {
        min-height: 400px;
        height: 550px;
        max-height: 700px;
      }
    `,
  ],
})
export class StoreInventoryComponent implements OnInit, OnDestroy {
  storeInventories: IStoreInventory[];
  eventSubscriber?: Subscription;
  selectedRowIndex?: number;
  itemsPerPage: number;
  links: any;
  page: number;
  predicate: string;
  ascending: boolean;
  columnDefs: any[];
  rowData: any = [];
  event: any;
  searchValue?: string;

  constructor(
    protected storeInventoryService: StoreInventoryService,
    protected eventManager: JhiEventManager,
    protected modalService: NgbModal,
    protected parseLinks: JhiParseLinks,
    private spinner: NgxSpinnerService
  ) {
    this.storeInventories = [];
    this.itemsPerPage = ITEMS_PER_PAGE;
    this.page = 0;
    this.links = {
      last: 0,
    };
    this.predicate = 'id';
    this.ascending = true;
    this.columnDefs = [
      {
        headerName: 'Libellé',
        field: 'produitLibelle',
        sortable: true,
        filter: 'agTextColumnFilter',
        minWidth: 300,
        flex: 1.2,
      },
      {
        headerName: 'Stock initial',
        field: 'quantityInit',
        type: ['rightAligned', 'numericColumn'],
        editable: true,
        width: 120,
        valueFormatter: this.formatNumber,
      },
      {
        headerName: 'Quantité saisie',
        width: 140,
        field: 'quantityOnHand',
        editable: true,
        type: ['rightAligned', 'numericColumn'],
      },
      {
        headerName: 'Ecart',
        width: 80,
        type: ['rightAligned', 'numericColumn'],
        valueGetter: this.setGap,
        cellStyle: this.cellClass,
      },
    ];
  }
  cellClass(params: any): any {
    if (params.data.updated) {
      const ecart = Number(params.data.quantityOnHand) - Number(params.data.quantityInit);
      return ecart >= 0 ? { backgroundColor: 'lightgreen' } : { backgroundColor: 'lightcoral' };
    }
    return;
  }
  setGap(params: any): number {
    if (params.data.updated) {
      return params.data.quantityOnHand - params.data.quantityInit;
    }
    return 0;
  }
  loadAll(): void {
    this.storeInventoryService.query({}).subscribe((res: HttpResponse<IStoreInventory[]>) => this.onSuccess(res.body));
  }

  reset(): void {
    this.page = 0;
    this.storeInventories = [];
    this.loadAll();
  }

  loadPage(page: number): void {
    this.page = page;
    this.loadAll();
  }

  ngOnInit(): void {
    this.loadAll();
    // this.registerChangeInStoreInventories();
  }

  ngOnDestroy(): void {
    if (this.eventSubscriber) {
      this.eventManager.destroy(this.eventSubscriber);
    }
  }

  trackId(index: number, item: IStoreInventory): number {
    // eslint-disable-next-line @typescript-eslint/no-unnecessary-type-assertion
    return item.id!;
  }

  registerChangeInStoreInventories(): void {
    this.eventSubscriber = this.eventManager.subscribe('storeInventoryListModification', () => this.reset());
  }

  delete(storeInventory: IStoreInventory): void {
    const modalRef = this.modalService.open(StoreInventoryDeleteDialogComponent, { size: 'lg', backdrop: 'static' });
    modalRef.componentInstance.storeInventory = storeInventory;
  }

  sort(): string[] {
    const result = [this.predicate + ',' + (this.ascending ? 'asc' : 'desc')];
    if (this.predicate !== 'id') {
      result.push('id');
    }
    return result;
  }

  protected paginateStoreInventories(data: IStoreInventory[] | null, headers: HttpHeaders): void {
    const headersLink = headers.get('link');
    this.links = this.parseLinks.parse(headersLink ? headersLink : '');
    if (data) {
      for (let i = 0; i < data.length; i++) {
        this.storeInventories.push(data[i]);
      }
    }
  }

  onFilterTextBoxChanged(event: any): void {
    this.searchValue = event.target.value;
    /*  if(this.produitSelected!==null && this.produitSelected!==undefined &&
        Number(event.target.value)!==0){

      }*/
  }

  formatNumber(number: any): string {
    return Math.floor(number.value)
      .toString()
      .replace(/(\d)(?=(\d{3})+(?!\d))/g, '$1 ');
  }

  init(): void {
    this.spinner.show();
    this.subscribeToSaveResponse(this.storeInventoryService.init());
    /* setTimeout(() => {

      this.spinner.hide();
    }, 5000);
*/
  }
  protected subscribeToSaveResponse(result: Observable<HttpResponse<IStoreInventory>>): void {
    result.subscribe(
      () => this.onSaveSuccess(),
      () => this.onSaveError()
    );
  }
  protected onSaveSuccess(): void {
    this.spinner.hide();
    this.loadAll();
  }
  protected onSaveError(): void {
    this.spinner.hide();
  }

  protected onSuccess(data: IStoreInventory[] | null): void {
    if (data) {
      this.storeInventories = data;
    }
  }
  clickRow(storeInventory: IStoreInventory): void {
    this.selectedRowIndex = storeInventory.id;
    this.rowData = storeInventory.storeInventoryLines;
  }
}
