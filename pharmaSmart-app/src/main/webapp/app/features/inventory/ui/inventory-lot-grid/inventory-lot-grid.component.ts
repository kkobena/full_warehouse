import {Component, computed, effect, inject, input, output} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {
  AllCommunityModule,
  CellValueChangedEvent,
  ClientSideRowModelModule,
  ColDef,
  GridApi,
  GridReadyEvent,
  ModuleRegistry,
  themeAlpine,
} from 'ag-grid-community';
import {AgGridAngular} from 'ag-grid-angular';
import {Select} from 'primeng/select';
import {Tooltip} from 'primeng/tooltip';
import {InventoryApiService} from '../../data-access/services/inventory-api.service';
import {InventoryEditorFacade} from '../../data-access/facades/inventory-editor.facade';
import {InventoryStore} from '../../data-access/store/inventory.store';
import {IInventoryLotLine, InventoryLineFilter, LINE_FILTERS} from '../../models';
import {IStorage} from '../../../../shared/model/magasin.model';
import {IRayon} from '../../../../shared/model/rayon.model';

ModuleRegistry.registerModules([AllCommunityModule, ClientSideRowModelModule]);

@Component({
  selector: 'app-inventory-lot-grid',
  imports: [CommonModule, FormsModule, AgGridAngular, Select, Tooltip],
  templateUrl: './inventory-lot-grid.component.html',
  styleUrl: './inventory-lot-grid.component.scss',
})
export class InventoryLotGridComponent {
  readonly inventoryId = input.required<number>();
  readonly blindMode = input<boolean>(false);
  readonly storages = input<IStorage[]>([]);
  readonly rayons = input<IRayon[]>([]);
  readonly readOnly = input<boolean>(false);
  readonly pageSize = input<number>(20);

  readonly filterChange = output<{
    lineFilter: InventoryLineFilter;
    storageId: number | null;
    rayonId: number | null;
    search: string;
  }>();
  readonly storageChange = output<number | null>();
  readonly nextPage = output<void>();

  readonly editorFacade = inject(InventoryEditorFacade);
  readonly store = inject(InventoryStore);
  private readonly api = inject(InventoryApiService);

  lotFilters = LINE_FILTERS;
  selectedLotFilter: InventoryLineFilter = 'NONE';
  selectedStorageId: number | null = null;
  selectedRayonId: number | null = null;
  quickFilterText = '';

  readonly lots = computed(() => this.store.lotLines());
  readonly loading = computed(() => this.store.loadingLines());

  protected readonly theme = themeAlpine;
  private gridApi: GridApi | null = null;
  private suppressRowDataRefresh = false;
  private pendingFocusRow0 = false;

  readonly columnDefs = computed<ColDef[]>(() => [
    {
      field: 'produitCip',
      headerName: 'Code CIP',
      width: 120,
      editable: false,
      sortable: true,
      filter: true,
    },
    {
      field: 'produitLibelle',
      headerName: 'Produit',
      flex: 1,
      editable: false,
      sortable: true,
      filter: true,
    },
    {
      field: 'numLot',
      headerName: 'N° Lot',
      width: 130,
      editable: false,
      sortable: true,
      filter: true,
    },
    {
      field: 'expiryDate',
      headerName: 'Date expiration',
      width: 130,
      editable: false,
      sortable: true,
    },
    {
      field: 'quantityInit',
      headerName: 'Stock initial',
      width: 110,
      editable: false,
      type: ['rightAligned', 'numericColumn'],
      hide: this.blindMode(),
      valueFormatter: params => params.value ?? '—',
    },
    {
      field: 'quantityOnHand',
      headerName: 'Qté constatée',
      width: 130,
      editable: true,
      type: ['rightAligned', 'numericColumn'],
      cellStyle: params => {
        if (params.data?.updated) {
          return {backgroundColor: '#f0fff4', fontWeight: 'bold'};
        }
        return null;
      },
      cellEditorParams: {preventStepping: true},
    },
    {
      field: 'gap',
      headerName: 'Écart',
      width: 90,
      editable: false,
      type: 'numericColumn',
      cellStyle: params => {
        const val = params.value;
        if (val == null) return null;
        if (val < 0) return {color: '#dc3545', backgroundColor: '#fde8ea', fontWeight: 'bold'};
        if (val > 0) return {color: '#0d6efd', backgroundColor: '#e7f1ff', fontWeight: 'bold'};
        return null;
      },
    },
    {
      field: 'classePareto',
      headerName: 'ABC',
      width: 70,
      editable: false,
      cellStyle: {textAlign: 'center'},
      headerClass: 'ag-header-cell-center',
      cellRenderer: (params: any) => {
        const cls = params.value;
        if (!cls) return '';
        const colors: Record<string, string> = {A: '#198754', B: '#0d6efd', C: '#6c757d'};
        const color = colors[cls] ?? '#6c757d';
        return `<span style="display:inline-block;padding:1px 6px;border-radius:10px;font-size:11px;font-weight:700;color:#fff;background:${color}">${cls}</span>`;
      },
    },
    {
      field: 'updated',
      headerName: 'Saisi',
      width: 70,
      editable: false,
      cellStyle: {textAlign: 'center'},
      headerClass: 'ag-header-cell-center',
      cellRenderer: (params: any) => params.value ? '<i class="pi pi-check text-success"></i>' : '',
    },
  ]);

  readonly defaultColDef: ColDef = {
    resizable: true,
    suppressMovable: false,
  };

  constructor() {
    effect(() => {
      const lines = this.lots();
      if (!this.gridApi || this.suppressRowDataRefresh) return;
      this.gridApi.setGridOption('rowData', lines);
      if (this.pendingFocusRow0 && lines.length > 0) {
        this.pendingFocusRow0 = false;
        setTimeout(() => {
          this.gridApi?.startEditingCell({rowIndex: 0, colKey: 'quantityOnHand'});
        }, 100);
      }
    });
  }

  onGridReady(event: GridReadyEvent): void {
    this.gridApi = event.api;
    this.gridApi.setGridOption('rowData', this.lots());
  }

  onQuickFilterChange(): void {
    this.emitFilterChange();
  }

  onLotFilterChange(): void {
    this.emitFilterChange();
  }

  onStorageFilterChange(): void {
    this.selectedRayonId = null;
    this.storageChange.emit(this.selectedStorageId);
    this.emitFilterChange();
  }

  onRayonFilterChange(): void {
    this.emitFilterChange();
  }

  onCellValueChanged(event: CellValueChangedEvent): void {
    if (event.column.getColId() !== 'quantityOnHand' || this.readOnly()) return;

    const lotData = event.data as IInventoryLotLine;
    if (!lotData?.id) return;

    const numValue = Number(event.newValue);
    if (!Number.isFinite(numValue) || numValue < 0) {
      event.node.setDataValue('quantityOnHand', event.oldValue);
      setTimeout(() => {
        this.gridApi?.startEditingCell({rowIndex: event.rowIndex!, colKey: 'quantityOnHand'});
      });
      return;
    }

    const rowIndex = event.rowIndex!;
    this.suppressRowDataRefresh = true;

    this.api.updateLot(lotData.id, {
      ...lotData,
      quantityOnHand: numValue,
      storeInventoryLineId: lotData.storeInventoryLineId,
    }).subscribe({
      next: updated => {
        event.node.setDataValue('gap', updated.gap);
        event.node.setDataValue('updated', true);
        this.navigateToNextRow(rowIndex);
      },
      error: () => {
        event.node.setDataValue('quantityOnHand', event.oldValue);
        this.suppressRowDataRefresh = false;
      },
    });
  }

  private navigateToNextRow(currentRowIndex: number): void {
    const rowCount = this.gridApi?.getDisplayedRowCount() ?? 0;
    if (currentRowIndex >= rowCount - 1) {
      this.suppressRowDataRefresh = false;
      this.pendingFocusRow0 = true;
      this.nextPage.emit();
    } else {
      setTimeout(() => {
        this.gridApi?.startEditingCell({rowIndex: currentRowIndex + 1, colKey: 'quantityOnHand'});
        setTimeout(() => {
          this.suppressRowDataRefresh = false;
        }, 200);
      }, 50);
    }
  }

  private emitFilterChange(): void {
    this.filterChange.emit({
      lineFilter: this.selectedLotFilter,
      storageId: this.selectedStorageId,
      rayonId: this.selectedRayonId,
      search: this.quickFilterText,
    });
  }
}
