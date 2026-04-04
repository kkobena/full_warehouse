import { computed } from '@angular/core';
import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';
import { IFacture, IFacturationKpi, IInvoiceSearchParams } from '../models';

interface FacturationState {
  // Liste
  factures: IFacture[];
  totalItems: number;
  loading: boolean;

  // Sélection (master/detail)
  selectedFacture: IFacture | null;

  // Sélection multiple (bulk actions)
  selectedFactures: IFacture[];
  clearSelectionTrigger: number;

  // Filtres de recherche
  searchParams: IInvoiceSearchParams;

  // KPI
  kpi: IFacturationKpi | null;
  kpiLoading: boolean;

  // Etat sauvegarde
  isSaving: boolean;
  error: string | null;
}

const defaultSearchParams: IInvoiceSearchParams = {
  startDate: '',
  endDate: '',
  factureGroupees: false,
};

export const FacturationStore = signalStore(
  { providedIn: 'root' },
  withState<FacturationState>({
    factures: [],
    totalItems: 0,
    loading: false,
    selectedFacture: null,
    selectedFactures: [],
    clearSelectionTrigger: 0,
    searchParams: defaultSearchParams,
    kpi: null,
    kpiLoading: false,
    isSaving: false,
    error: null,
  }),
  withComputed(({ factures, selectedFactures, selectedFacture, kpi }) => ({
    panelOpen: computed(() => selectedFacture() !== null),
    hasSelection: computed(() => selectedFactures().length > 0),
    facturesNonReglees: computed(() => factures().filter(f => f.statut === 'NOT_PAID')),
    facturesEnRetard: computed(() => factures().filter(f => f.enRetard === true)),
    tauxRecouvrement: computed(() => kpi()?.tauxRecouvrement ?? 0),
  })),
  withMethods(store => ({
    setFactures(factures: IFacture[], totalItems: number): void {
      patchState(store, { factures, totalItems, loading: false });
    },
    setLoading(loading: boolean): void {
      patchState(store, { loading });
    },
    selectFacture(facture: IFacture | null): void {
      patchState(store, { selectedFacture: facture });
    },
    setSelectedFactures(selectedFactures: IFacture[]): void {
      patchState(store, { selectedFactures });
    },
    clearSelection(): void {
      patchState(store, {
        selectedFactures: [],
        clearSelectionTrigger: store.clearSelectionTrigger() + 1,
      });
    },
    setSearchParams(searchParams: IInvoiceSearchParams): void {
      patchState(store, { searchParams });
    },
    setKpi(kpi: IFacturationKpi | null): void {
      patchState(store, { kpi, kpiLoading: false });
    },
    setKpiLoading(kpiLoading: boolean): void {
      patchState(store, { kpiLoading });
    },
    setSaving(isSaving: boolean): void {
      patchState(store, { isSaving });
    },
    setError(error: string | null): void {
      patchState(store, { error });
    },
    updateFactureInList(updated: IFacture): void {
      patchState(store, {
        factures: store.factures().map(f =>
          f.factureItemId?.id === updated.factureItemId?.id ? updated : f,
        ),
        selectedFacture:
          store.selectedFacture()?.factureItemId?.id === updated.factureItemId?.id
            ? updated
            : store.selectedFacture(),
      });
    },
    removeFactureFromList(factureId: number): void {
      patchState(store, {
        factures: store.factures().filter(f => f.factureItemId?.id !== factureId),
        selectedFacture:
          store.selectedFacture()?.factureItemId?.id === factureId
            ? null
            : store.selectedFacture(),
        totalItems: store.totalItems() - 1,
      });
    },
  })),
);
