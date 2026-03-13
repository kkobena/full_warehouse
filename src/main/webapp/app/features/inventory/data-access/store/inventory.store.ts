import {computed} from '@angular/core';
import {patchState, signalStore, withComputed, withMethods, withState} from '@ngrx/signals';
import {IStoreInventory} from '../../../../shared/model';
import {
  IInventoryLine,
  ImportResultRecord,
  InventoryEvent,
  InventoryEventType,
  InventoryProgressRecord,
} from '../../models';

interface InventoryState {
  // List
  inventories: IStoreInventory[];
  totalInventories: number;
  loadingList: boolean;

  // Editor
  currentInventory: IStoreInventory | null;
  lines: IInventoryLine[];
  totalLines: number;
  loadingLines: boolean;

  // Pending batch edits (buffered locally, not yet saved)
  pendingEdits: Record<number, number>; // lineId -> quantityOnHand

  // Progress
  progress: InventoryProgressRecord | null;

  // Import
  lastImportResult: ImportResultRecord | null;
  isImporting: boolean;

  // UI
  blindMode: boolean;
  isSavingBatch: boolean;

  // Errors
  error: string | null;

  // Event bus
  lastEvent: InventoryEvent | null;
  _eventSeq: number;
}

const initialState: InventoryState = {
  inventories: [],
  totalInventories: 0,
  loadingList: false,

  currentInventory: null,
  lines: [],
  totalLines: 0,
  loadingLines: false,

  pendingEdits: {},

  progress: null,

  lastImportResult: null,
  isImporting: false,

  blindMode: false,
  isSavingBatch: false,

  error: null,

  lastEvent: null,
  _eventSeq: 0,
};

export const InventoryStore = signalStore(
  {providedIn: 'root'},

  withState(initialState),

  withComputed(store => ({
    hasPendingEdits: computed(() => Object.keys(store.pendingEdits()).length > 0),
    pendingEditCount: computed(() => Object.keys(store.pendingEdits()).length),
    progressPercent: computed(() => store.progress()?.progressPercent ?? 0),
  })),

  withMethods(store => ({
    setInventories(inventories: IStoreInventory[], total: number): void {
      patchState(store, {inventories, totalInventories: total});
    },

    setLoadingList(loadingList: boolean): void {
      patchState(store, {loadingList});
    },

    setCurrentInventory(currentInventory: IStoreInventory | null): void {
      patchState(store, {currentInventory});
    },

    setLines(lines: IInventoryLine[], total: number): void {
      patchState(store, {lines, totalLines: total});
    },

    updateLine(updated: IInventoryLine): void {
      const lines = store.lines().map(l => l.id === updated.id ? updated : l);
      patchState(store, {lines});
    },

    setLoadingLines(loadingLines: boolean): void {
      patchState(store, {loadingLines});
    },

    addPendingEdit(lineId: number, quantityOnHand: number): void {
      const current = store.pendingEdits();
      patchState(store, {pendingEdits: {...current, [lineId]: quantityOnHand}});
    },

    clearPendingEdits(): void {
      patchState(store, {pendingEdits: {}});
    },

    setProgress(progress: InventoryProgressRecord | null): void {
      patchState(store, {progress});
    },

    setBlindMode(blindMode: boolean): void {
      patchState(store, {blindMode});
    },

    setLastImportResult(lastImportResult: ImportResultRecord | null): void {
      patchState(store, {lastImportResult});
    },

    setIsImporting(isImporting: boolean): void {
      patchState(store, {isImporting});
    },

    setIsSavingBatch(isSavingBatch: boolean): void {
      patchState(store, {isSavingBatch});
    },

    setError(error: string | null): void {
      patchState(store, {error});
    },

    emitEvent(type: InventoryEventType, payload?: any): void {
      const nextSeq = store._eventSeq() + 1;
      patchState(store, {lastEvent: {type, payload, seq: nextSeq}, _eventSeq: nextSeq});
    },

    reset(): void {
      patchState(store, initialState);
    },

    resetEditor(): void {
      patchState(store, {
        currentInventory: null,
        lines: [],
        totalLines: 0,
        loadingLines: false,
        pendingEdits: {},
        progress: null,
        lastImportResult: null,
        isImporting: false,
        blindMode: false,
        isSavingBatch: false,
        error: null,
        lastEvent: null,
      });
    },
  })),
);
