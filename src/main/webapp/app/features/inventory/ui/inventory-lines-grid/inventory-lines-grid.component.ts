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
import {InventoryEditorFacade} from '../../data-access/facades/inventory-editor.facade';
import {InventoryStore} from '../../data-access/store/inventory.store';
import {IInventoryLine, InventoryLineFilter, LINE_FILTERS} from '../../models';
import {IStorage} from '../../../../shared/model/magasin.model';
import {IRayon} from '../../../../shared/model/rayon.model';

ModuleRegistry.registerModules([AllCommunityModule, ClientSideRowModelModule]);

@Component({
  selector: 'app-inventory-lines-grid',
  imports: [CommonModule, FormsModule, AgGridAngular, Select, Tooltip],
  templateUrl: './inventory-lines-grid.component.html',
  styleUrl: './inventory-lines-grid.component.scss',
})
export class InventoryLinesGridComponent {
  readonly inventoryId = input.required<number>();
  readonly blindMode = input<boolean>(false);
  readonly storages = input<IStorage[]>([]);
  readonly rayons = input<IRayon[]>([]);
  readonly pageSize = input<number>(20);

  readonly filterChange = output<{
    lineFilter: InventoryLineFilter;
    storageId: number | null;
    rayonId: number | null
  }>();
  readonly storageChange = output<number | null>();
  readonly nextPage = output<void>();

  readonly editorFacade = inject(InventoryEditorFacade);
  readonly store = inject(InventoryStore);

  lineFilters = LINE_FILTERS;
  selectedLineFilter: InventoryLineFilter = 'NONE';
  selectedStorageId: number | null = null;
  selectedRayonId: number | null = null;
  quickFilterText = '';
  readonly rowData = computed(() => this.store.lines());
  readonly columnDefs = computed<ColDef[]>(() => [
    {
      field: 'produitCip',
      headerName: 'Code CIP',
      width: 120,
      editable: false,
      sortable: true,
      filter: true,
      type: 'text',
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
      field: 'quantityInit',
      headerName: 'Stock actuel',
      width: 100,
      editable: false,

      type: ['rightAligned', 'numericColumn'],
      hide: this.blindMode(),
      valueFormatter: params => (params.value ?? '—'),
    },
    {
      field: 'quantityOnHand',
      headerName: 'Qté inventoriée',
      width: 130,
      editable: true,
      type: ['rightAligned', 'numericColumn'],
      cellStyle: params => {
        if (params.data?.updated) {
          return {backgroundColor: '#f0fff4', fontWeight: 'bold'};
        }
        return null;
      },
      cellEditorParams: {
        preventStepping: true
      }
    },
    {
      field: 'gap',
      headerName: 'Écart',
      width: 100,
      editable: false,
      type: 'numericColumn',
      cellStyle: params => {
        const val = params.value;
        if (val == null) {
          return null;
        }
        if (val < 0) {
          return {color: '#dc3545', backgroundColor: '#fde8ea', fontWeight: 'bold'};
        }
        if (val > 0) {
          return {color: '#0d6efd', backgroundColor: '#e7f1ff', fontWeight: 'bold'};
        }
        return null;
      },
    },
    {
      field: 'updated',
      headerName: 'Saisi',
      width: 80,
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
  protected readonly theme = themeAlpine;
  private suppressRowDataRefresh = false;
  private pendingFocusRow0 = false;
  private gridApi: GridApi | null = null;

  constructor() {
    effect(() => {
      const lines = this.rowData();
      if (!this.gridApi) {
        return;
      }
      if (this.suppressRowDataRefresh) {
        return;
      }
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
    this.gridApi.setGridOption('rowData', this.rowData());
  }

  onQuickFilterChange(): void {
    this.gridApi?.setGridOption('quickFilterText', this.quickFilterText);
  }

  onLineFilterChange(): void {
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
    if (event.column.getColId() !== 'quantityOnHand') {
      return;
    }

    const newValue = event.newValue;
    const lineData = event.data as IInventoryLine;
    if (lineData?.id == null) {
      return;
    }

    const numValue = Number(newValue);
    const isValid = Number.isFinite(numValue) && numValue >= 0;

    if (!isValid) {
      // Invalid input: revert and stay editing the same cell
      event.node.setDataValue('quantityOnHand', event.oldValue);
      setTimeout(() => {
        this.gridApi?.startEditingCell({rowIndex: event.rowIndex!, colKey: 'quantityOnHand'});
      });
      return;
    }

    // Build the line payload for the API
    const lineToSave: IInventoryLine = {
      ...lineData,
      quantityOnHand: numValue,
      storeInventoryId: this.inventoryId(),
    };

    const rowIndex = event.rowIndex!;

    // Suppress effect-driven rowData refresh so it doesn't kill editing state
    this.suppressRowDataRefresh = true;

    // Save immediately via API
    this.editorFacade.saveLine(lineToSave, this.inventoryId());

    // Mark the row as updated visually
    event.node.setDataValue('updated', true);

    // Navigate to next row
    this.navigateToNextRow(rowIndex);
  }

  private navigateToNextRow(currentRowIndex: number): void {
    const rowCount = this.gridApi?.getDisplayedRowCount() ?? 0;
    if (currentRowIndex >= rowCount - 1) {
      // Last row of the server page: load next page and focus row 0
      this.suppressRowDataRefresh = false;
      this.pendingFocusRow0 = true;
      this.nextPage.emit();
    } else {
      // Move to next row in current page
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
      lineFilter: this.selectedLineFilter,
      storageId: this.selectedStorageId,
      rayonId: this.selectedRayonId,
    });
  }
}
