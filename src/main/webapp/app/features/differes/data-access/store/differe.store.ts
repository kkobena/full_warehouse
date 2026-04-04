import { computed } from '@angular/core';
import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';
import {
  IClientDiffere,
  IDiffere,
  IDiffereSummary,
  IDiffereSearchParams,
  IReglementDiffere,
  IReglementDiffereSummary,
} from '../models';

interface DiffereState {
  // Liste principale (onglet Différés)
  differes: IDiffere[];
  totalItems: number;
  loading: boolean;

  // Liste règlements (onglet Historique)
  reglements: IReglementDiffere[];
  totalReglements: number;
  loadingReglements: boolean;

  // Clients (chargé une seule fois)
  clients: IClientDiffere[];
  clientsLoaded: boolean;

  // KPI / résumés
  summary: IDiffereSummary | null;
  reglementSummary: IReglementDiffereSummary | null;

  // Sélection master/detail
  selectedDiffere: IDiffere | null;

  // Paramètres de recherche persistés (navigation retour)
  searchParams: IDiffereSearchParams | null;

  // UI
  isSaving: boolean;
  error: string | null;
}

export const DiffereStore = signalStore(
  { providedIn: 'root' },
  withState<DiffereState>({
    differes: [],
    totalItems: 0,
    loading: false,
    reglements: [],
    totalReglements: 0,
    loadingReglements: false,
    clients: [],
    clientsLoaded: false,
    summary: null,
    reglementSummary: null,
    selectedDiffere: null,
    searchParams: null,
    isSaving: false,
    error: null,
  }),
  withComputed(({ selectedDiffere, summary }) => ({
    panelOpen: computed(() => selectedDiffere() !== null),
    totalRestant: computed(() => summary()?.rest ?? 0),
  })),
  withMethods(store => ({
    setDifferes(differes: IDiffere[], totalItems: number): void {
      patchState(store, { differes, totalItems, loading: false });
    },
    setLoading(loading: boolean): void {
      patchState(store, { loading });
    },
    setClients(clients: IClientDiffere[]): void {
      patchState(store, { clients, clientsLoaded: true });
    },
    setSummary(summary: IDiffereSummary | null): void {
      patchState(store, { summary });
    },
    setReglements(reglements: IReglementDiffere[], totalReglements: number): void {
      patchState(store, { reglements, totalReglements, loadingReglements: false });
    },
    setLoadingReglements(loadingReglements: boolean): void {
      patchState(store, { loadingReglements });
    },
    setReglementSummary(reglementSummary: IReglementDiffereSummary | null): void {
      patchState(store, { reglementSummary });
    },
    selectDiffere(differe: IDiffere | null): void {
      patchState(store, { selectedDiffere: differe });
    },
    setSearchParams(searchParams: IDiffereSearchParams): void {
      patchState(store, { searchParams });
    },
    setSaving(isSaving: boolean): void {
      patchState(store, { isSaving });
    },
    setError(error: string | null): void {
      patchState(store, { error });
    },
    refreshDiffereInList(updated: IDiffere): void {
      patchState(store, {
        differes: store.differes().map(d =>
          d.customerId === updated.customerId ? updated : d,
        ),
        selectedDiffere:
          store.selectedDiffere()?.customerId === updated.customerId
            ? updated
            : store.selectedDiffere(),
      });
    },
    removeDiffereIfSolde(customerId: number): void {
      const updated = store.differes().filter(d => d.customerId !== customerId || (d.rest ?? 0) > 0);
      patchState(store, {
        differes: updated,
        selectedDiffere:
          store.selectedDiffere()?.customerId === customerId && (store.selectedDiffere()?.rest ?? 0) <= 0
            ? null
            : store.selectedDiffere(),
      });
    },
  })),
);
