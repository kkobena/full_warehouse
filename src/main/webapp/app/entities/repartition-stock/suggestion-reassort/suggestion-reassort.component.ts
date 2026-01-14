import { Component, computed, effect, inject, input, resource, signal } from '@angular/core';
import { CommonModule, DatePipe, DecimalPipe } from '@angular/common';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { ILigneReassort, ISuggestionReassort } from '../repartition-stock.model';
import { RepartitionStockService } from '../repartition-stock.service';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ToastModule } from 'primeng/toast';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { Tooltip } from 'primeng/tooltip';
import { acceptButtonProps, rejectButtonProps } from '../../../shared/util/modal-button-props';
import { AgGridAngular } from 'ag-grid-angular';
import {
  CellValueChangedEvent,
  ColDef,
  GridOptions,
  GridReadyEvent,
  ICellRendererParams,
  ValueFormatterParams,
  ModuleRegistry,
  AllCommunityModule,
} from 'ag-grid-community';
import { firstValueFrom } from 'rxjs';

// Register AG Grid modules
ModuleRegistry.registerModules([AllCommunityModule]);

@Component({
  selector: 'jhi-suggestion-reassort',
  templateUrl: './suggestion-reassort.component.html',
  styleUrls: ['./suggestion-reassort.component.scss'],
  imports: [CommonModule, DatePipe, TableModule, ButtonModule, TagModule, ToastModule, ConfirmDialogModule, Tooltip, AgGridAngular],
  providers: [ConfirmationService, MessageService, DecimalPipe],
})
export class SuggestionReassortComponent {
  // Signal input
  readonly typeReassort = input<string>('RAYON');

  // Services
  private readonly repartitionService = inject(RepartitionStockService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly messageService = inject(MessageService);
  private readonly decimalPipe = inject(DecimalPipe);

  // Resource pour charger les suggestions de manière réactive
  private readonly suggestionsResource = resource({
    loader: async () => {
      const type = this.typeReassort();
      const res = await firstValueFrom(this.repartitionService.getOpenSuggestions(type));
      return res.body ?? [];
    },
  });

  // Writable signal for suggestions (allows local mutations)
  readonly suggestions = signal<ISuggestionReassort[]>([]);
  readonly loading = computed(() => this.suggestionsResource.isLoading());

  // Expanded rows - auto-expand first suggestion
  readonly expandedRows = signal<Record<string, boolean>>({});

  constructor() {
    // Synchroniser suggestions avec le resource
    effect(() => {
      const value = this.suggestionsResource.value();
      if (value && Array.isArray(value)) {
        this.suggestions.set(value);
        // Auto-expand first suggestion
        const firstId = value[0]?.id;
        if (firstId) {
          this.expandedRows.set({ [firstId]: true });
        }
      }
    });
  }

  // AG Grid configuration
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
      cellClass: 'pharma-qty-value text-right',
    },
    {
      headerName: 'Seuil Mini',
      field: 'seuilMini',
      width: 100,
      minWidth: 90,
      type: 'numericColumn',
      valueFormatter: (params: ValueFormatterParams) => this.formatNumber(params.value),
      cellClass: 'pharma-qty-value text-right',
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
      cellRenderer: (params: ICellRendererParams) => {
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
    /* {
      headerName: '',
      width: 50,
      maxWidth: 50,
      pinned: 'right',
      suppressSizeToFit: false,
       cellClass: 'text-center',
        headerClass: 'text-center',
      checkboxSelection: true,
      headerCheckboxSelection: true,
      lockPosition: 'right',
      sortable: false,
      filter: false,
      resizable: false,
    },*/
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
    singleClickEdit: true, // Double-click to edit
    stopEditingWhenCellsLoseFocus: true,

    // Excel-like navigation
    navigateToNextCell: params => {
      const previousCell = params.previousCellPosition;
      const suggestedNextCell = params.nextCellPosition;

      if (!suggestedNextCell) {
        return null;
      }

      // If we're in the quantity column and pressing Enter or Tab, move to next row
      if (previousCell?.column.getColId() === 'quantity') {
        if (params.key === 'Enter' || params.key === 'Tab') {
          return {
            rowIndex: previousCell.rowIndex + 1,
            column: params.api.getColumnDefs()?.[7] as any, // quantity column
            rowPinned: null,
          };
        }
      }

      return suggestedNextCell;
    },
  };

  onGridReady(params: GridReadyEvent): void {
    // Auto-size columns to fit the grid width
    params.api.sizeColumnsToFit();
  }

  getGridHeight(rowCount: number): string {
    // Calculate grid height based on row count
    // Header: 42px, Row: 42px each, Min: 200px, Max: 600px
    const headerHeight = 42;
    const rowHeight = 42;
    const scrollbarBuffer = 20;
    const calculatedHeight = headerHeight + rowCount * rowHeight + scrollbarBuffer;

    const minHeight = 200;
    const maxHeight = 600;

    return `${Math.min(Math.max(calculatedHeight, minHeight), maxHeight)}px`;
  }

  reloadSuggestions(): void {
    this.suggestionsResource.reload();
  }

  onCellValueChanged(event: CellValueChangedEvent, suggestion: ISuggestionReassort): void {
    const ligne: ILigneReassort = event.data;
    const newQuantity = event.newValue;

    if (ligne.id && newQuantity > 0) {
      // Validate quantity
      if (ligne.stockAvailable && newQuantity > ligne.stockAvailable) {
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: 'La quantité ne peut pas dépasser le stock disponible',
        });
        // Revert the change
        event.node.setDataValue('quantity', event.oldValue);

        // Remettre le focus sur la cellule pour correction
        setTimeout(() => {
          event.api.setFocusedCell(event.node.rowIndex!, 'quantity');
          event.api.startEditingCell({
            rowIndex: event.node.rowIndex!,
            colKey: 'quantity',
          });
        }, 100);
        return;
      }

      // Update quantity on server
      this.repartitionService.updateLigneQuantity(ligne.id, newQuantity).subscribe({
        error: () => {
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: 'Erreur lors de la mise à jour',
          });
          // Revert the change
          event.node.setDataValue('quantity', event.oldValue);
        },
      });
    }
  }

  deleteLigne(ligne: ILigneReassort, suggestion: ISuggestionReassort): void {
    this.confirmationService.confirm({
      message: 'Êtes-vous sûr de vouloir supprimer cette ligne?',
      header: 'Confirmation',
      icon: 'pi pi-exclamation-triangle',
      rejectButtonProps: rejectButtonProps(),
      acceptButtonProps: acceptButtonProps(),
      accept: () => {
        if (ligne.id) {
          this.repartitionService.deleteLigne(ligne.id).subscribe({
            next: () => {
              // Mise à jour immutable avec signal.update()
              this.suggestions.update(current =>
                current
                  .map(s => {
                    if (s.id === suggestion.id) {
                      const updatedLignes = s.ligneReassorts?.filter(l => l.id !== ligne.id);
                      return { ...s, ligneReassorts: updatedLignes };
                    }
                    return s;
                  })
                  .filter(s => s.ligneReassorts && s.ligneReassorts.length > 0),
              );

              this.messageService.add({
                severity: 'success',
                summary: 'Succès',
                detail: 'Ligne supprimée',
              });
            },
            error: () => {
              this.messageService.add({
                severity: 'error',
                summary: 'Erreur',
                detail: 'Erreur lors de la suppression',
              });
            },
          });
        }
      },
    });
  }

  validateSuggestion(suggestion: ISuggestionReassort): void {
    this.confirmationService.confirm({
      message: 'Êtes-vous sûr de vouloir valider cette suggestion? Cette action déplacera le stock.',
      header: 'Confirmation',
      icon: 'pi pi-exclamation-triangle',
      rejectButtonProps: rejectButtonProps(),
      acceptButtonProps: acceptButtonProps(),
      accept: () => {
        if (suggestion.id) {
          this.repartitionService.validateSuggestion(suggestion.id).subscribe({
            next: () => {
              this.messageService.add({
                severity: 'success',
                summary: 'Succès',
                detail: 'Suggestion validée et stock déplacé',
              });
              this.reloadSuggestions();
            },
            error: () => {
              this.messageService.add({
                severity: 'error',
                summary: 'Erreur',
                detail: 'Erreur lors de la validation',
              });
            },
          });
        }
      },
    });
  }

  deleteSuggestion(suggestion: ISuggestionReassort): void {
    this.confirmationService.confirm({
      message: 'Êtes-vous sûr de vouloir supprimer cette suggestion?',
      header: 'Confirmation',
      icon: 'pi pi-exclamation-triangle',
      rejectButtonProps: rejectButtonProps(),
      acceptButtonProps: acceptButtonProps(),
      accept: () => {
        if (suggestion.id) {
          this.repartitionService.deleteSuggestion(suggestion.id).subscribe({
            next: () => {
              // Mise à jour immutable avec signal.update()
              this.suggestions.update(current => current.filter(s => s.id !== suggestion.id));

              this.messageService.add({
                severity: 'success',
                summary: 'Succès',
                detail: 'Suggestion supprimée',
              });
            },
            error: () => {
              this.messageService.add({
                severity: 'error',
                summary: 'Erreur',
                detail: 'Erreur lors de la suppression',
              });
            },
          });
        }
      },
    });
  }

  private formatNumber(value: number | null | undefined): string {
    if (value === null || value === undefined) {
      return '';
    }
    return this.decimalPipe.transform(value, '1.0-0') ?? '';
  }
}
