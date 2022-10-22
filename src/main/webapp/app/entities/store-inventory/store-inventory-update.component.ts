import { Component, OnInit, ViewChild } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs';
import { IStoreInventory } from 'app/shared/model/store-inventory.model';
import { StoreInventoryService } from './store-inventory.service';
import { AgGridAngular } from 'ag-grid-angular';
import { AllCommunityModules } from '@ag-grid-community/all-modules';
import { StoreInventoryLineService } from '../store-inventory-line/store-inventory-line.service';

@Component({
  selector: 'jhi-store-inventory-update',
  templateUrl: './store-inventory-update.component.html',
})
export class StoreInventoryUpdateComponent implements OnInit {
  isSaving = false;
  @ViewChild('productGrid') productGrid!: AgGridAngular;
  public modules: any[] = AllCommunityModules;
  columnDefs: any[];
  rowData: any = [];
  event: any;
  searchValue?: string;
  storeInventory?: IStoreInventory;

  constructor(
    protected storeInventoryService: StoreInventoryService,
    protected storeInventoryLineService: StoreInventoryLineService,
    protected activatedRoute: ActivatedRoute
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
        valueGetter: this.setGap,
        cellStyle: this.cellClass,
      },
    ];
  }

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ storeInventory }) => {
      this.storeInventory = storeInventory;
      if (this.storeInventory) {
        this.rowData = this.storeInventory.storeInventoryLines;
      }
    });
  }

  previousState(): void {
    window.history.back();
  }
  save(): void {
    this.isSaving = true;
    if (this.storeInventory && this.storeInventory.id != null) {
      this.subscribeToSaveResponse(this.storeInventoryService.close(this.storeInventory.id));
    }
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<IStoreInventory>>): void {
    result.subscribe(
      () => this.onSaveSuccess(),
      () => this.onSaveError()
    );
  }

  protected onSaveSuccess(): void {
    this.isSaving = false;
    this.previousState();
  }

  protected onSaveError(): void {
    this.isSaving = false;
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

  formatNumber(number: any): string {
    return Math.floor(number.value)
      .toString()
      .replace(/(\d)(?=(\d{3})+(?!\d))/g, '$1 ');
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

  stockOnHandcellStyle(params: any): any {
    if (params.data.updated) {
      return { backgroundColor: '#c6c6c6' };
    }
    return;
  }
  protected subscribeToLineResponse(result: Observable<HttpResponse<IStoreInventory>>): void {
    result.subscribe(
      (res: HttpResponse<IStoreInventory>) => this.onSaveLineSuccess(res.body),
      () => this.onSaveError()
    );
  }
  protected onSaveLineSuccess(storeInventory: IStoreInventory | null): void {
    if (storeInventory) {
      this.storeInventory = storeInventory;
      this.rowData = this.storeInventory.storeInventoryLines;
    }
  }
}
