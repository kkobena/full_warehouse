import { computed } from '@angular/core';
import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';
import {
  IDossierFactureProjection,
  IInvoicePaymentParam,
  IReglement,
  IReglementFactureDossier,
} from '../models';

interface ReglementState {
  // Historique des règlements (liste factures réglées)
  reglements: IReglement[];
  totalItems: number;
  loading: boolean;

  // Paramètres de recherche persistés (navigation retour)
  searchParams: IInvoicePaymentParam | null;

  // Contexte du règlement en cours (depuis une facture)
  dossierFactureProjection: IDossierFactureProjection | null;
  reglementFactureDossiers: IReglementFactureDossier[];
  dossierLoading: boolean;

  // UI
  isSaving: boolean;
  error: string | null;
}

export const ReglementStore = signalStore(
  { providedIn: 'root' },
  withState<ReglementState>({
    reglements: [],
    totalItems: 0,
    loading: false,
    searchParams: null,
    dossierFactureProjection: null,
    reglementFactureDossiers: [],
    dossierLoading: false,
    isSaving: false,
    error: null,
  }),
  withComputed(({ reglementFactureDossiers, dossierFactureProjection }) => ({
    montantRestantTotal: computed(() => {
      const projection = dossierFactureProjection();
      if (!projection) return 0;
      return projection.montantTotal - projection.montantDetailRegle;
    }),
    hasDossiers: computed(() => reglementFactureDossiers().length > 0),
  })),
  withMethods(store => ({
    setReglements(reglements: IReglement[], totalItems: number): void {
      patchState(store, { reglements, totalItems, loading: false });
    },
    setLoading(loading: boolean): void {
      patchState(store, { loading });
    },
    setSearchParams(searchParams: IInvoicePaymentParam): void {
      patchState(store, { searchParams });
    },
    setDossierContext(
      projection: IDossierFactureProjection | null,
      dossiers: IReglementFactureDossier[],
    ): void {
      patchState(store, {
        dossierFactureProjection: projection,
        reglementFactureDossiers: dossiers,
        dossierLoading: false,
      });
    },
    setDossierLoading(dossierLoading: boolean): void {
      patchState(store, { dossierLoading });
    },
    setSaving(isSaving: boolean): void {
      patchState(store, { isSaving });
    },
    setError(error: string | null): void {
      patchState(store, { error });
    },
    resetDossierContext(): void {
      patchState(store, {
        dossierFactureProjection: null,
        reglementFactureDossiers: [],
      });
    },
    removeReglement(paymentId: number): void {
      patchState(store, {
        reglements: store.reglements().filter(r => r.id.id !== paymentId),
        totalItems: store.totalItems() - 1,
      });
    },
  })),
);
