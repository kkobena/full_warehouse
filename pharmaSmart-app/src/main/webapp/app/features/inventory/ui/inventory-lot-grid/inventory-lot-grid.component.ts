import {Component, computed, effect, inject, input, output, ChangeDetectionStrategy} from '@angular/core';
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
import {NgbTooltip} from '@ng-bootstrap/ng-bootstrap';
import {InventoryApiService} from '../../data-access/services/inventory-api.service';
import {InventoryEditorFacade} from '../../data-access/facades/inventory-editor.facade';
import {InventoryStore} from '../../data-access/store/inventory.store';
import {
  IInventoryLotLine,
  InventoryLineFilter,
  isGapLineFilter,
  lineFiltersFor,
  renderParetoBadge,
} from '../../models';
import {IStorage} from '../../../../shared/model/magasin.model';
import {IRayon} from '../../../../shared/model/rayon.model';
import {SelectComponent} from '../../../../shared/ui';

ModuleRegistry.registerModules([AllCommunityModule, ClientSideRowModelModule]);

@Component({
  selector: 'app-inventory-lot-grid',
  imports: [CommonModule, FormsModule, AgGridAngular, SelectComponent, NgbTooltip],
  templateUrl: './inventory-lot-grid.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
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

  /** Filtres d'écart retirés en mode aveugle (privilège pr-voir-stock-inventaire) */
  readonly lotFilters = computed(() => lineFiltersFor(this.blindMode()));
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
      // Seule colonne saisissable — et seulement tant que l'inventaire est ouvert.
      // `readOnly` ne gardait que `onCellValueChanged` : la cellule s'ouvrait quand même
      // à la saisie, et la valeur tapée était silencieusement rejetée.
      editable: !this.readOnly(),
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
      // L'écart se déduit directement du stock théorique : même privilège que lui
      hide: this.blindMode(),
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
      hide: true,
      cellStyle: {textAlign: 'center'},
      headerClass: 'ag-header-cell-center',
      cellRenderer: (params: any) => renderParetoBadge(params.value),
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
    // Filtre d'écart déjà actif au moment où le mode aveugle s'applique : on le
    // retombe sur « Tous », sinon la grille resterait filtrée sur une information
    // que l'opérateur n'a pas le droit de voir.
    effect(() => {
      if (this.blindMode() && isGapLineFilter(this.selectedLotFilter)) {
        this.selectedLotFilter = 'NONE';
        this.emitFilterChange();
      }
    });

    effect(() => {
      const lines = this.lots();
      if (!this.gridApi || this.suppressRowDataRefresh) return;
      this.gridApi.setGridOption('rowData', lines);
      if (this.pendingFocusRow0 && lines.length > 0) {
        this.pendingFocusRow0 = false;
        setTimeout(() => {
          this.gridApi?.ensureIndexVisible(0, 'top');
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

  /**
   * Fait suivre la grille : la ligne suivante est amenée dans le viewport avant
   * d'ouvrir son éditeur. Sans `ensureIndexVisible`, une ligne sortie du viewport
   * n'est plus rendue (virtualisation AG Grid) et `startEditingCell` reste sans
   * effet — la saisie s'arrêtait au bas de l'écran.
   */
  private navigateToNextRow(currentRowIndex: number): void {
    const rowCount = this.gridApi?.getDisplayedRowCount() ?? 0;
    if (currentRowIndex >= rowCount - 1) {
      this.suppressRowDataRefresh = false;
      this.pendingFocusRow0 = true;
      this.nextPage.emit();
    } else {
      setTimeout(() => {
        this.gridApi?.ensureIndexVisible(currentRowIndex + 1, 'middle');
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
