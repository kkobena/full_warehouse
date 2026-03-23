import { computed } from '@angular/core';
import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';
import { IAjust } from '../../../../shared/model/ajust.model';
import { IAjustement } from '../../../../shared/model/ajustement.model';
import { IMotifAjustement } from '../../../../shared/model/motif-ajustement.model';
import { IStorage } from '../../../../shared/model/magasin.model';
import { IProduit } from '../../../../shared/model/produit.model';
import { AjustDirection, AjustEvent, AjustEventType, ILotItem } from '../../models';

interface AjustementState {
  // History list
  history: IAjust[];
  totalHistory: number;
  loadingHistory: boolean;

  // Active record (en cours de saisie)
  currentAjust: IAjust | null;
  lines: IAjustement[];
  loadingLines: boolean;

  // Form state
  direction: AjustDirection;
  selectedStorage: IStorage | null;
  selectedMotif: IMotifAjustement | null;
  selectedProduit: IProduit | null;

  // Lot management
  gestionLot: boolean;
  availableLots: ILotItem[];
  selectedLot: ILotItem | null;
  loadingLots: boolean;

  // Reference data
  storages: IStorage[];
  motifs: IMotifAjustement[];

  // UI
  isSaving: boolean;
  error: string | null;

  // Event bus
  lastEvent: AjustEvent | null;
  _eventSeq: number;
}

const initialState: AjustementState = {
  history: [],
  totalHistory: 0,
  loadingHistory: false,

  currentAjust: null,
  lines: [],
  loadingLines: false,

  direction: 'OUT',
  selectedStorage: null,
  selectedMotif: null,
  selectedProduit: null,

  gestionLot: false,
  availableLots: [],
  selectedLot: null,
  loadingLots: false,

  storages: [],
  motifs: [],

  isSaving: false,
  error: null,

  lastEvent: null,
  _eventSeq: 0,
};

export const AjustementStore = signalStore(
  { providedIn: 'root' },

  withState(initialState),

  withComputed(store => ({
    /** Stock actuel selon le type de stockage sélectionné */
    stockActuel: computed((): number => {
      const p = store.selectedProduit();
      if (!p) return 0;
      const st = store.selectedStorage()?.storageType;
      return st === 'SAFETY_STOCK' ? (p.qtyReserve ?? 0) : (p.totalQuantity ?? 0);
    }),

    hasLines: computed(() => store.lines().length > 0),
    lotSelectionVisible: computed(() =>
      store.gestionLot() && store.direction() === 'IN' && !!store.selectedProduit(),
    ),
  })),

  withMethods(store => ({
    // ── History ──────────────────────────────────────────────────────────────
    setHistory(history: IAjust[], total: number): void {
      patchState(store, { history, totalHistory: total, loadingHistory: false });
    },
    setLoadingHistory(loadingHistory: boolean): void {
      patchState(store, { loadingHistory });
    },

    // ── Active record ────────────────────────────────────────────────────────
    setCurrentAjust(currentAjust: IAjust | null): void {
      patchState(store, { currentAjust });
    },
    setLines(lines: IAjustement[]): void {
      patchState(store, { lines, loadingLines: false });
    },
    setLoadingLines(loadingLines: boolean): void {
      patchState(store, { loadingLines });
    },

    // ── Form ─────────────────────────────────────────────────────────────────
    setDirection(direction: AjustDirection): void {
      // Changer de direction vide la sélection de lot
      patchState(store, { direction, selectedLot: null, availableLots: [] });
    },
    setStorage(selectedStorage: IStorage | null): void {
      patchState(store, { selectedStorage });
    },
    setMotif(selectedMotif: IMotifAjustement | null): void {
      patchState(store, { selectedMotif });
    },
    setProduit(selectedProduit: IProduit | null): void {
      patchState(store, { selectedProduit, selectedLot: null, availableLots: [] });
    },

    // ── Lots ─────────────────────────────────────────────────────────────────
    setGestionLot(gestionLot: boolean): void {
      patchState(store, { gestionLot });
    },
    setAvailableLots(availableLots: ILotItem[]): void {
      patchState(store, { availableLots, loadingLots: false });
    },
    setLoadingLots(loadingLots: boolean): void {
      patchState(store, { loadingLots });
    },
    setSelectedLot(selectedLot: ILotItem | null): void {
      patchState(store, { selectedLot });
    },

    // ── Reference data ────────────────────────────────────────────────────────
    setStorages(storages: IStorage[]): void {
      patchState(store, { storages });
    },
    setMotifs(motifs: IMotifAjustement[]): void {
      patchState(store, { motifs });
    },

    // ── UI ────────────────────────────────────────────────────────────────────
    setIsSaving(isSaving: boolean): void {
      patchState(store, { isSaving });
    },
    setError(error: string | null): void {
      patchState(store, { error });
    },

    // ── Event bus ─────────────────────────────────────────────────────────────
    emitEvent(type: AjustEventType, payload?: unknown): void {
      const nextSeq = store._eventSeq() + 1;
      patchState(store, { lastEvent: { type, payload, seq: nextSeq }, _eventSeq: nextSeq });
    },

    // ── Reset ─────────────────────────────────────────────────────────────────
    resetForm(): void {
      patchState(store, {
        currentAjust: null,
        lines: [],
        selectedMotif: null,
        selectedProduit: null,
        selectedLot: null,
        availableLots: [],
        direction: 'OUT',
        isSaving: false,
        error: null,
      });
    },
  })),
);
