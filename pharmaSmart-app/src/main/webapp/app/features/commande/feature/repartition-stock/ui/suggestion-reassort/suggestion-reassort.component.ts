import { Component, computed, effect, inject, input, resource, signal } from '@angular/core';
import { CommonModule} from '@angular/common';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { ToastModule } from 'primeng/toast';
import { Tooltip } from 'primeng/tooltip';
import { MessageService } from 'primeng/api';
import { AgGridAngular } from 'ag-grid-angular';
import {
  CellValueChangedEvent,
  ColDef,
  GridOptions,
  GridReadyEvent,
  ICellRendererParams,
  ValueFormatterParams,
  CellClassParams,
  ModuleRegistry,
  AllCommunityModule,
} from 'ag-grid-community';
import { firstValueFrom } from 'rxjs';
import { ILigneReassort, ISuggestionReassort } from '../../../../../../entities/repartition-stock/repartition-stock.model';
import { RepartitionStockService } from '../../../../../../entities/repartition-stock/repartition-stock.service';
import { NgbConfirmDialogService } from 'app/shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive';
import { formatNumber } from 'app/shared/util/warehouse-util';
ModuleRegistry.registerModules([AllCommunityModule]);

@Component({
  selector: 'app-suggestion-reassort',
  templateUrl: './suggestion-reassort.component.html',
  styleUrls: ['./suggestion-reassort.scss'],
  imports: [CommonModule, TableModule, ButtonModule, TagModule, ToastModule, Tooltip, AgGridAngular],
  providers: [MessageService],
})
export class AppSuggestionReassortComponent {
  readonly typeReassort = input<string>('RAYON');

  private readonly repartitionService = inject(RepartitionStockService);
  private readonly confirmDialog = inject(NgbConfirmDialogService);
  private readonly messageService = inject(MessageService);

  private readonly suggestionsResource = resource({
    loader: async () => {
      const type = this.typeReassort();
      const res = await firstValueFrom(this.repartitionService.getOpenSuggestions(type));
      return res.body ?? [];
    },
  });

  readonly suggestions = signal<ISuggestionReassort[]>([]);
  readonly loading = computed(() => this.suggestionsResource.isLoading());
  readonly expandedRows = signal<Record<string, boolean>>({});

  constructor() {
    effect(() => {
      const value = this.suggestionsResource.value();
      if (value && Array.isArray(value)) {
        this.suggestions.set(value);
        const firstId = value[0]?.id;
        if (firstId) {
          this.expandedRows.set({ [firstId]: true });
        }
      }
    });
  }

  protected columnDefs: ColDef[] = [
    {
      headerName: '#',
      valueGetter: 'node.rowIndex + 1',
      width: 60,
      maxWidth: 60,
      pinned: 'left',
      cellClass: 'text-center',
      suppressSizeToFit: false,
    },
    {
      headerName: 'Produit',
      field: 'produitLibelle',
      minWidth: 200,
      flex: 3,
      cellClass: 'pharma-entity-name',
    },
    {
      headerName: 'Code CIP',
      field: 'produitCode',
      width: 120,
      minWidth: 100,
      cellClass: 'pharma-code',
    },
    {
      headerName: 'Stockage',
      field: 'storageName',
      width: 140,
      minWidth: 120,
      cellRenderer: (params: ICellRendererParams) => {
        return `<span class="p-tag p-tag-secondary">${params.value ?? ''}</span>`;
      },
    },
    {
      headerName: 'Stock Actuel',
      field: 'stockActuel',
      width: 110,
      minWidth: 100,
      type: 'numericColumn',
      valueFormatter: (params: ValueFormatterParams) => this.formatNumber(params.value),
      cellClass: 'text-right',
      cellStyle: (params: CellClassParams) => {
        const stockActuel = params.data?.stockActuel ?? 0;
        const seuilMini = params.data?.seuilMini ?? 0;
        if (stockActuel <= 0) {
          return { color: '#dc2626', fontWeight: '700', backgroundColor: '#fef2f2' };
        }
        if (stockActuel < seuilMini) {
          return { color: '#c2410c', fontWeight: '600', backgroundColor: '#fff7ed' };
        }
        if (seuilMini > 0 && stockActuel <= seuilMini * 1.3) {
          return { color: '#d97706', backgroundColor: '#fffbeb' };
        }
        return { color: '#16a34a' };
      },
      tooltipValueGetter: (params: any) => {
        const stockActuel = params.data?.stockActuel ?? 0;
        const seuilMini = params.data?.seuilMini ?? 0;
        if (stockActuel <= 0) return '⛔ Rupture de stock';
        if (stockActuel < seuilMini) return `⚠️ Stock critique — seuil mini : ${seuilMini}`;
        if (seuilMini > 0 && stockActuel <= seuilMini * 1.3) return `⚠️ Stock faible — seuil mini : ${seuilMini}`;
        return `✅ Stock OK`;
      },
    },
    {
      headerName: 'Seuil Mini',
      field: 'seuilMini',
      width: 100,
      minWidth: 90,
      type: 'numericColumn',
      valueFormatter: (params: ValueFormatterParams) => this.formatNumber(params.value),
      cellClass: 'text-right',
      cellStyle: (params: CellClassParams) => {
        const stockActuel = params.data?.stockActuel ?? 0;
        const seuilMini = params.data?.seuilMini ?? 0;
        if (seuilMini > 0 && stockActuel < seuilMini) {
          return { fontWeight: '600', color: '#c2410c' };
        }
        return null;
      },
    },
    {
      headerName: 'Stock Dispo',
      field: 'stockAvailable',
      width: 110,
      minWidth: 100,
      type: 'numericColumn',
      valueFormatter: (params: ValueFormatterParams) => this.formatNumber(params.value),
      cellClass: 'pharma-qty-value text-right',
    },
    {
      headerName: 'Qté à Réassortir',
      field: 'quantity',
      width: 150,
      minWidth: 130,
      editable: true,
      type: ['rightAligned', 'numericColumn'],
      cellEditorParams: {
        min: 1,
        max: (params: any) => params.data.stockAvailable || 999999,
        precision: 0,
      },
      valueFormatter: (params: ValueFormatterParams) => this.formatNumber(params.value),
      cellClass: 'pharma-qty-highlight text-right editable-cell',
      cellStyle: { cursor: 'pointer' },
    },
    {
      headerName: 'Actions',
      width: 90,
      maxWidth: 90,
      pinned: 'right',
      suppressSizeToFit: false,
      cellRenderer: (_params: ICellRendererParams) => {
        return `
          <div class="flex justify-content-center">
            <button class="delete-btn p-button p-button-rounded p-button-text p-button-danger p-button-sm">
              <i class="pi pi-trash"></i>
            </button>
          </div>
        `;
      },
      onCellClicked: params => {
        const target = params.event?.target as HTMLElement;
        if (target.closest('.delete-btn')) {
          this.deleteLigne(params.data, params.context.suggestion);
        }
      },
    },
  ];

  protected defaultColDef: ColDef = {
    sortable: true,
    filter: false,
    resizable: true,
  };

  protected gridOptions: GridOptions = {
    animateRows: true,
    rowSelection: {
      mode: 'multiRow',
      enableClickSelection: false,
      checkboxes: true,
      headerCheckbox: true,
    },
    enableCellTextSelection: true,
    enterNavigatesVertically: true,
    enterNavigatesVerticallyAfterEdit: true,
    suppressMovableColumns: true,
    suppressMenuHide: true,
    enableBrowserTooltips: true,
    tooltipShowDelay: 300,
    singleClickEdit: true,
    stopEditingWhenCellsLoseFocus: true,
    navigateToNextCell: params => {
      const previousCell = params.previousCellPosition;
      const suggestedNextCell = params.nextCellPosition;
      if (!suggestedNextCell) {
        return null;
      }
      if (previousCell?.column.getColId() === 'quantity') {
        if (params.key === 'Enter' || params.key === 'Tab') {
          return {
            rowIndex: previousCell.rowIndex + 1,
            column: params.api.getColumnDefs()?.[7] as any,
            rowPinned: null,
          };
        }
      }
      return suggestedNextCell;
    },
  };

  onGridReady(params: GridReadyEvent): void {
    params.api.sizeColumnsToFit();
  }

  getGridHeight(rowCount: number): string {
    const headerHeight = 42;
    const rowHeight = 42;
    const scrollbarBuffer = 20;
    const calculatedHeight = headerHeight + rowCount * rowHeight + scrollbarBuffer;
    return `${Math.min(Math.max(calculatedHeight, 200), 600)}px`;
  }

  reloadSuggestions(): void {
    this.suggestionsResource.reload();
  }

  onCellValueChanged(event: CellValueChangedEvent, _suggestion: ISuggestionReassort): void {
    const ligne: ILigneReassort = event.data;
    const newQuantity = event.newValue;
    if (ligne.id && newQuantity > 0) {
      if (ligne.stockAvailable && newQuantity > ligne.stockAvailable) {
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: 'La quantité ne peut pas dépasser le stock disponible',
        });
        event.node.setDataValue('quantity', event.oldValue);
        setTimeout(() => {
          event.api.setFocusedCell(event.node.rowIndex!, 'quantity');
          event.api.startEditingCell({ rowIndex: event.node.rowIndex!, colKey: 'quantity' });
        }, 100);
        return;
      }
      this.repartitionService.updateLigneQuantity(ligne.id, newQuantity).subscribe({
        error: () => {
          this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Erreur lors de la mise à jour' });
          event.node.setDataValue('quantity', event.oldValue);
        },
      });
    }
  }

  deleteLigne(ligne: ILigneReassort, suggestion: ISuggestionReassort): void {
    this.confirmDialog.onConfirm(
      () => {
        if (ligne.id) {
          this.repartitionService.deleteLigne(ligne.id).subscribe({
            next: () => {
              this.suggestions.update(current =>
                current
                  .map(s => {
                    if (s.id === suggestion.id) {
                      return { ...s, ligneReassorts: s.ligneReassorts?.filter(l => l.id !== ligne.id) };
                    }
                    return s;
                  })
                  .filter(s => s.ligneReassorts && s.ligneReassorts.length > 0),
              );
              this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Ligne supprimée' });
            },
            error: () => this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Erreur lors de la suppression' }),
          });
        }
      },
      'Confirmation',
      'Êtes-vous sûr de vouloir supprimer cette ligne?',
    );
  }

  validateSuggestion(suggestion: ISuggestionReassort): void {
    this.confirmDialog.onConfirm(
      () => {
        if (suggestion.id) {
          this.repartitionService.validateSuggestion(suggestion.id).subscribe({
            next: () => {
              this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Suggestion validée et stock déplacé' });
              this.reloadSuggestions();
            },
            error: () => this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Erreur lors de la validation' }),
          });
        }
      },
      'Confirmation',
      'Êtes-vous sûr de vouloir valider cette suggestion? Cette action déplacera le stock.',
    );
  }

  deleteSuggestion(suggestion: ISuggestionReassort): void {
    this.confirmDialog.onConfirm(
      () => {
        if (suggestion.id) {
          this.repartitionService.deleteSuggestion(suggestion.id).subscribe({
            next: () => {
              this.suggestions.update(current => current.filter(s => s.id !== suggestion.id));
              this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Suggestion supprimée' });
            },
            error: () => this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Erreur lors de la suppression' }),
          });
        }
      },
      'Confirmation',
      'Êtes-vous sûr de vouloir supprimer cette suggestion?',
    );
  }

  private formatNumber(value: number | null | undefined): string {
    if (value === null || value === undefined) {
      return '';
    }
    return formatNumber(value);
  }
}
