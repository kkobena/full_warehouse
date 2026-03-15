import { inject, Injectable } from '@angular/core';
import { InventoryApiService } from '../services/inventory-api.service';
import { InventoryStore } from '../store/inventory.store';
import { IInventoryLine } from '../../models';

@Injectable({ providedIn: 'root' })
export class InventoryEditorFacade {
  private readonly api = inject(InventoryApiService);
  readonly store = inject(InventoryStore);

  // Expose store state
  readonly lines = this.store.lines;
  readonly totalLines = this.store.totalLines;
  readonly lotLines = this.store.lotLines;
  readonly lotTotalLines = this.store.lotTotalLines;
  readonly loadingLines = this.store.loadingLines;
  readonly pendingEdits = this.store.pendingEdits;
  readonly hasPendingEdits = this.store.hasPendingEdits;
  readonly pendingEditCount = this.store.pendingEditCount;
  readonly progress = this.store.progress;
  readonly progressPercent = this.store.progressPercent;
  readonly blindMode = this.store.blindMode;
  readonly isSavingBatch = this.store.isSavingBatch;
  readonly isImporting = this.store.isImporting;
  readonly lastImportResult = this.store.lastImportResult;
  readonly lastEvent = this.store.lastEvent;

  loadLines(inventoryId: number, params: any): void {
    this.store.setLoadingLines(true);
    this.store.setError(null);
    const queryParams = { ...params, storeInventoryId: inventoryId };
    this.api.getLines(queryParams).subscribe({
      next: resp => {
        const total = parseInt(resp.headers.get('X-Total-Count') ?? '0', 10);
        this.store.setLines(resp.body ?? [], total);
        this.store.setLoadingLines(false);
        this.store.emitEvent('LINES_LOADED');
      },
      error: err => {
        this.store.setError(err?.message ?? 'Erreur lors du chargement des lignes');
        this.store.setLoadingLines(false);
      },
    });
  }

  loadLotLines(inventoryId: number, params: any): void {
    this.store.setLoadingLines(true);
    this.store.setError(null);
    const queryParams = { ...params, storeInventoryId: inventoryId };
    this.api.getLotsPage(queryParams).subscribe({
      next: resp => {
        const total = parseInt(resp.headers.get('X-Total-Count') ?? '0', 10);
        this.store.setLotLines(resp.body ?? [], total);
        this.store.setLoadingLines(false);
      },
      error: err => {
        this.store.setError(err?.message ?? 'Erreur lors du chargement des lots');
        this.store.setLoadingLines(false);
      },
    });
  }

  editLine(lineId: number, quantityOnHand: number): void {
    this.store.addPendingEdit(lineId, quantityOnHand);
    this.store.emitEvent('LINE_EDITED', { lineId, quantityOnHand });
  }

  saveLine(line: IInventoryLine, inventoryId: number): void {
    this.api.updateLine(line).subscribe({
      next: resp => {
        const saved = resp.body;
        if (saved) {
          this.store.updateLine(saved);
        }
        this.store.emitEvent('LINE_SAVED', { lineId: line.id });
        this.refreshProgress(inventoryId);
      },
      error: err => {
        this.store.setError(err?.error?.detail ?? err?.message ?? 'Erreur lors de la sauvegarde');
        this.store.emitEvent('LINE_SAVE_ERROR', { lineId: line.id, error: err });
      },
    });
  }

  saveBatch(inventoryId: number): void {
    const edits = this.store.pendingEdits();
    const lines = Object.entries(edits).map(([id, qty]) => ({
      id: Number(id),
      quantityOnHand: qty,
    }));

    if (lines.length === 0) {
      return;
    }

    this.store.setIsSavingBatch(true);
    this.store.setError(null);

    this.api.batchSave(lines).subscribe({
      next: resp => {
        this.store.setIsSavingBatch(false);
        this.store.clearPendingEdits();
        this.store.emitEvent('BATCH_SAVED', resp.body);
        this.refreshProgress(inventoryId);
      },
      error: err => {
        this.store.setError(err?.message ?? "Erreur lors de la sauvegarde des lignes");
        this.store.setIsSavingBatch(false);
        this.store.emitEvent('BATCH_SAVE_ERROR', err);
      },
    });
  }

  importCsv(inventoryId: number, file: File): void {
    this.store.setIsImporting(true);
    this.store.setLastImportResult(null);
    this.store.setError(null);

    this.api.importCsv(inventoryId, file).subscribe({
      next: resp => {
        this.store.setIsImporting(false);
        this.store.setLastImportResult(resp.body ?? null);
        this.store.emitEvent('IMPORT_COMPLETED', resp.body);
        this.refreshProgress(inventoryId);
      },
      error: err => {
        this.store.setIsImporting(false);
        this.store.setError(err?.message ?? "Erreur lors de l'import CSV");
      },
    });
  }

  refreshProgress(inventoryId: number): void {
    this.api.getProgress(inventoryId).subscribe({
      next: resp => {
        this.store.setProgress(resp.body ?? null);
        this.store.emitEvent('PROGRESS_UPDATED', resp.body);
      },
      error: () => {
        // Silent fail for progress refresh
      },
    });
  }

  toggleBlindMode(): void {
    this.store.setBlindMode(!this.store.blindMode());
  }
}
