import { Component, OnInit, ViewChild } from '@angular/core';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs';
import { IStoreInventory } from 'app/shared/model/store-inventory.model';
import { StoreInventoryService } from './store-inventory.service';

import { StoreInventoryLineService } from '../store-inventory-line/store-inventory-line.service';
import { APPEND_TO, ITEMS_PER_PAGE, NOT_FOUND, PRODUIT_COMBO_MIN_LENGTH } from '../../shared/constants/pagination.constants';
import { RayonService } from '../rayon/rayon.service';
import { IRayon } from '../../shared/model/rayon.model';
import { ConfirmationService, LazyLoadEvent, MessageService } from 'primeng/api';
import { DialogService } from 'primeng/dynamicdialog';
import { Storage } from '../storage/storage.model';
import { StorageService } from '../storage/storage.service';
import { IStoreInventoryLine } from '../../shared/model/store-inventory-line.model';
import { AlertInfoComponent } from '../../shared/alert/alert-info.component';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { ErrorService } from '../../shared/error.service';
import { NgxSpinnerService } from 'ngx-spinner';
import { Table } from 'primeng/table';
import { EditableColumn } from 'primeng/table/table';

@Component({
  selector: 'jhi-store-inventory-update',
  templateUrl: './store-inventory-update.component.html',
  providers: [ConfirmationService, DialogService, MessageService],
})
export class StoreInventoryUpdateComponent implements OnInit {
  isSaving = false;
  @ViewChild('editTable') productGrid: Table;

  protected storages: Storage[];
  protected rayons: IRayon[] = [];
  protected storeInventoryLines?: IStoreInventoryLine[] = [];
  protected selectedRayon: IRayon | null;
  protected selectedStorage: Storage | null;
  protected columnDefs: any[];
  protected event: any;
  protected totalItems = 0;
  protected searchValue?: string;
  protected storeInventory?: IStoreInventory;
  protected readonly APPEND_TO = APPEND_TO;
  protected readonly PRODUIT_COMBO_MIN_LENGTH = PRODUIT_COMBO_MIN_LENGTH;
  protected readonly NOT_FOUND = NOT_FOUND;
  protected readonly filtres: any = [
    { name: 'UPDATED', label: 'Mise à jour' },
    { name: 'NOT_UPDATED', label: 'Pas mise à jour' },
    { name: 'GAP', label: 'Ecart' },
    { name: 'GAP_POSITIF', label: 'Ecart positif' },
    { name: 'GAP_NEGATIF', label: 'Ecart négatif' },
  ];
  protected selectedfiltres: any | null;
  protected showStock: boolean = true;
  protected page = 0;
  protected loading!: boolean;
  protected itemsPerPage = 20;
  protected editRowIndex: number;
  protected readonly ITEMS_PER_PAGE = ITEMS_PER_PAGE;
  protected moveToNext: boolean = false;
  protected openCurrentCell: boolean = false;

  constructor(
    protected storeInventoryService: StoreInventoryService,
    protected storeInventoryLineService: StoreInventoryLineService,
    protected activatedRoute: ActivatedRoute,
    protected rayonService: RayonService,
    private storageService: StorageService,
    protected modalService: NgbModal,
    private errorService: ErrorService,
    private spinner: NgxSpinnerService
  ) {
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
        headerName: 'Quantité inventoriée',
        flex: 0.5,
        field: 'quantityOnHand',
        editable: true,
        type: ['rightAligned', 'numericColumn'],
        cellStyle: this.stockOnHandcellStyle,
      },
      {
        headerName: 'Ecart',
        flex: 0.5,
        type: ['rightAligned', 'numericColumn'],

        cellStyle: this.cellClass,
      },
    ];
  }

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ storeInventory }) => {
      this.storeInventory = storeInventory;
      if (this.storeInventory) {
        this.loadStorage();
      }
    });
  }

  previousState(): void {
    window.history.back();
  }

  onSelect(): void {
    this.onSearch();
  }

  save(): void {
    this.isSaving = true;
    if (this.storeInventory && this.storeInventory.id != null) {
      this.subscribeToSaveResponse(this.storeInventoryService.close(this.storeInventory.id));
    }
  }

  onFilterTextBoxChanged(event: any): void {
    this.searchValue = event.target.value;
  }

  onCellValueChanged(params: any): void {
    this.subscribeToLineResponse(
      this.storeInventoryLineService.update({
        id: params.data.id,
        quantityOnHand: params.value,
        storeInventoryId: this.storeInventory?.id,
      })
    );
  }

  cellClass(item: IStoreInventoryLine): any {
    if (item.updated) {
      return item.gap >= 0 ? { backgroundColor: 'lightgreen' } : { backgroundColor: 'lightcoral' };
    }
    return;
  }

  getGap(item: IStoreInventoryLine): number {
    if (item.updated) {
      return item.quantityOnHand - item.quantityInit;
    }
    return undefined;
  }

  stockOnHandcellStyle(params: any): any {
    if (params.data.updated) {
      return { backgroundColor: '#c6c6c6' };
    }
    return;
  }

  itemTableColor(item: IStoreInventoryLine): string {
    if (!item.updated) {
      return 'bg-warning';
    } else if (item.quantityInit > item.quantityOnHand) {
      return 'bg-danger';
    } else if (item.quantityInit <= item.quantityOnHand) {
      return 'bg-success';
    } else return '';
  }

  searchFn(event: any): void {
    this.loadRayons(event.query, null);
  }

  loadRayons(search: string, storageId?: number): void {
    this.rayonService
      .query({
        page: 0,
        size: 10,
        search: search,
        storageId: storageId,
      })
      .subscribe((res: HttpResponse<IRayon[]>) => this.onLoadRayonSuccess(res.body));
  }

  onFilter(): void {
    this.onSearch();
  }

  onEditComplete(evt: any): void {
    //   console.log(evt);
  }

  onEditCancel(evt: any): void {
    console.log(evt, 'onedit');
  }

  //(keydown.enter)="onUpdateQuantity(item,$event,rowIndex,editTable)"
  onUpdateQuantity(item: IStoreInventoryLine, event: any, rowIndex: number, editTable: any): void {
    const editableColumn = editTable.editableColumn as EditableColumn;

    if (event.key === 'Enter') {
      const oldQuantityOnHand = item.quantityOnHand;

      const newQuantity = Number(event.target.value);

      if (newQuantity !== undefined && newQuantity !== null) {
        item.quantityOnHand = newQuantity;
        item.storeInventoryId = this.storeInventory.id;
        this.subscribeToSaveItemResponse(oldQuantityOnHand, item, this.storeInventoryLineService.update(item));
      }
    }
  }

  lazyLoading(event: LazyLoadEvent): void {
    if (event) {
      this.page = event.first! / event.rows!;
      this.loading = true;
      this.storeInventoryLineService
        .query({
          page: this.page,
          size: event.rows,
          ...this.buildQuery(),
        })
        .subscribe({
          next: (res: HttpResponse<IStoreInventoryLine[]>) => this.onSuccess(res.body, res.headers, this.page),
          error: () => {
            this.loading = false;
          },
        });
    }
  }

  loadPage(page?: number): void {
    const pageToLoad: number = page || this.page;
    this.loading = true;
    this.storeInventoryLineService
      .query({
        page: pageToLoad,
        size: this.itemsPerPage,
        ...this.buildQuery(),
      })
      .subscribe({
        next: (res: HttpResponse<IStoreInventoryLine[]>) => this.onSuccess(res.body, res.headers, pageToLoad),
        error: () => {
          this.loading = false;
          this.storeInventoryLines = [];
        },
      });
  }

  protected onSuccess(data: IStoreInventoryLine[] | null, headers: HttpHeaders, page: number): void {
    this.totalItems = Number(headers.get('X-Total-Count'));
    this.page = page;
    this.storeInventoryLines = data || [];
    this.loading = false;
  }

  protected onSearch(): void {
    this.loadPage();
  }

  protected onClear() {
    this.selectedRayon = null;
    this.onSearch();
  }

  protected onSelectStrorage(evt: any): void {
    this.onSearch();
    this.loadRayons(null, evt.value.id);
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<IStoreInventory>>): void {
    result.subscribe({
      next: () => this.onSaveSuccess(),
      error: () => this.onSaveError(),
    });
  }

  protected onSaveSuccess(): void {
    this.isSaving = false;
    this.previousState();
  }

  protected onSaveError(): void {
    this.isSaving = false;
  }

  protected subscribeToLineResponse(result: Observable<HttpResponse<IStoreInventory>>): void {
    result.subscribe({
      next: (res: HttpResponse<IStoreInventory>) => this.onSaveLineSuccess(res.body),
      error: () => this.onSaveError(),
    });
  }

  protected onLoadRayonSuccess(rayons: IRayon[] | null): void {
    this.rayons = rayons || [];
  }

  protected onSaveLineSuccess(storeInventory: IStoreInventory | null): void {
    if (storeInventory) {
      this.storeInventory = storeInventory;
      this.storeInventoryLines = this.storeInventory.storeInventoryLines;
    }
  }

  protected onLoadItems(data: IStoreInventoryLine[] | null): void {
    this.storeInventoryLines = data || [];
  }

  protected onUpadateIem(oldItem: IStoreInventoryLine, storeInventoryLine: IStoreInventoryLine): void {
    if (storeInventoryLine) {
      oldItem = storeInventoryLine;
      this.moveToNext = true;
      this.openCurrentCell = false;
    }
  }

  protected subscribeLoadItemsResponse(result: Observable<HttpResponse<IStoreInventoryLine[]>>): void {
    result.subscribe({
      next: (res: HttpResponse<IStoreInventoryLine[]>) => this.onLoadItems(res.body),
    });
  }

  protected onCommonError(error: any): void {
    if (error.error && error.error.status === 500) {
      this.openInfoDialog('Erreur applicative', 'alert alert-danger');
    } else {
      this.errorService.getErrorMessageTranslation(error.error.errorKey).subscribe({
        next: translatedErrorMessage => {
          this.openInfoDialog(translatedErrorMessage, 'alert alert-danger');
        },
        error: () => this.openInfoDialog(error.error.title, 'alert alert-danger'),
      });
    }
  }

  protected onSaveItemError(error: any, oldQuantityOnHand: number, item: IStoreInventoryLine): void {
    item.quantityOnHand = oldQuantityOnHand;
    this.moveToNext = false;
    this.openCurrentCell = true;
    console.log(this.openCurrentCell);
    /*  if (error.error && error.error.status === 500) {
        this.openInfoDialog('Erreur applicative', 'alert alert-danger');
      } else {
        this.errorService.getErrorMessageTranslation(error.error.errorKey).subscribe({
          next: translatedErrorMessage => {
            this.openInfoDialog(translatedErrorMessage, 'alert alert-danger');
          },
          error: () => this.openInfoDialog(error.error.title, 'alert alert-danger'),
        });
      }*/
  }

  protected subscribeToSaveItemResponse(
    oldQuantityOnHand: number,
    item: IStoreInventoryLine,
    result: Observable<HttpResponse<IStoreInventoryLine>>
  ): void {
    result.subscribe({
      next: (res: HttpResponse<IStoreInventoryLine>) => this.onUpadateIem(item, res.body),
      error: (err: any) => this.onSaveItemError(err, oldQuantityOnHand, item),
    });
  }

  private buildQuery(): any {
    return {
      storeInventoryId: this.storeInventory.id,
      search: this.searchValue,
      storageIds: [this.selectedStorage?.id],
      rayonId: this.selectedRayon?.id,
      selectedFilter: this.selectedfiltres ? this.selectedfiltres.name : 'NONE',
    };
  }

  private loadStorage(): void {
    this.storageService.query().subscribe((res: HttpResponse<Storage[]>) => {
      this.storages = res.body || [];
    });
  }

  private openInfoDialog(message: string, infoClass: string): void {
    const modalRef = this.modalService.open(AlertInfoComponent, {
      backdrop: 'static',
      centered: true,
    });
    modalRef.componentInstance.message = message;
    modalRef.componentInstance.infoClass = infoClass;
  }
}
