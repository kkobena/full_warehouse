import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map, tap } from 'rxjs/operators';
import { InventoryApiService } from '../services/inventory-api.service';
import { InventoryStore } from '../store/inventory.store';
import { IInventoryLine } from '../../models';
import { ErrorService } from '../../../../shared/error.service';

@Injectable({ providedIn: 'root' })
export class InventoryEditorFacade {
  private readonly api = inject(InventoryApiService);
  private readonly errorService = inject(ErrorService);
  readonly store = inject(InventoryStore);
  /** Comptages écrits depuis ce poste depuis la dernière lecture de la progression */
  private localCounts = 0;

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
        this.store.setError(
          this.errorService.getErrorMessage(err, 'Erreur lors du chargement des lignes'));
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
        this.store.setError(
          this.errorService.getErrorMessage(err, 'Erreur lors du chargement des lots'));
        this.store.setLoadingLines(false);
      },
    });
  }

  editLine(lineId: number, quantityOnHand: number): void {
    this.store.addPendingEdit(lineId, quantityOnHand);
    this.store.emitEvent('LINE_EDITED', { lineId, quantityOnHand });
  }

  /**
   * Sauvegarde immédiate d'une ligne comptée.
   *
   * Pas de `refreshProgress` ici : il déclencherait un rechargement de la page en pleine
   * saisie (cf. plan « saisie sans rechargement »). La progression se rafraîchit au
   * chargement de chaque page et par la scrutation périodique.
   */
  /**
   * Sauvegarde immédiate d'une ligne comptée.
   *
   * Renvoie le flux au lieu de le souscrire : l'appelant doit pouvoir **invalider sa
   * saisie** si l'écriture échoue. Tant que la façade souscrivait elle-même, la grille
   * marquait la ligne comme comptée et avançait sans jamais savoir que le serveur avait
   * refusé — backend coupé, l'opérateur croyait compter.
   */
  saveLine(line: IInventoryLine): Observable<IInventoryLine | null> {
    return this.api.updateLine(line).pipe(
      map(resp => resp.body ?? null),
      tap({
        next: saved => {
          // Comptabilisé au succès seulement : une écriture refusée ne fait pas bouger
          // la progression, et ne doit donc rien défalquer.
          if (!line.updated) {
            this.localCounts++;
          }
          if (saved) {
            // Remplacement en place : ni l'ordre ni la composition de la liste ne changent,
            // la grille ne se décale donc pas sous l'opérateur.
            this.store.updateLine({ ...saved, saveFailed: false });
          }
          this.store.emitEvent('LINE_SAVED', { lineId: line.id });
        },
        error: err => {
          // La ligne est marquée non persistée : sans ce signal, une coupure réseau
          // creuserait un trou silencieux dans l'inventaire.
          if (line.id != null) {
            this.store.markLineSaveFailed(line.id);
          }
          // 409 : comptage concurrent — la valeur serveur fait foi, on recharge
          const message = err?.status === 409
            ? 'Cette ligne vient d\'être comptée par un autre opérateur — valeurs rechargées'
            : this.errorService.getErrorMessage(err, 'Erreur lors de la sauvegarde');
          this.store.setError(message);
          this.store.emitEvent('LINE_SAVE_ERROR', { lineId: line.id, error: err });
        },
      }),
    );
  }

  /**
   * Signale un comptage effectué localement mais écrit hors de {@link saveLine} — la
   * grille lot passe par l'API lot en direct. Même finalité : ne pas confondre nos
   * propres comptages avec ceux d'un autre poste.
   */
  notifyLocalCount(): void {
    this.localCounts++;
  }

  /**
   * Nombre de comptages locaux depuis la dernière lecture, puis remise à zéro.
   * Consommé par l'éditeur pour interpréter une variation de la progression.
   */
  consumeLocalCounts(): number {
    const count = this.localCounts;
    this.localCounts = 0;
    return count;
  }

  saveBatch(inventoryId: number): void {
    const edits = this.store.pendingEdits();
    const loaded = this.store.lines();
    // La version lue accompagne chaque ligne : le serveur rejette (409 / conflictedIds)
    // toute saisie calculée sur une valeur périmée par un comptage concurrent.
    const lines = Object.entries(edits).map(([id, qty]) => ({
      id: Number(id),
      quantityOnHand: qty,
      version: loaded.find(l => l.id === Number(id))?.version,
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
        this.store.setError(
          this.errorService.getErrorMessage(err, 'Erreur lors de la sauvegarde des lignes'));
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
        this.store.setError(
          this.errorService.getErrorMessage(err, "Erreur lors de l'import CSV"));
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
