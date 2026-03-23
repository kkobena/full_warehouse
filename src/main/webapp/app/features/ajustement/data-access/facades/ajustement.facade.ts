import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { AjustementApiService } from '../services/ajustement-api.service';
import { AjustementStore } from '../store/ajustement.store';
import { ConfigurationService } from '../../../../shared/configuration.service';
import { StorageService } from '../../../../entities/storage/storage.service';
import { Params } from '../../../../shared/model/enumerations/params.model';
import { IStorage } from '../../../../shared/model/magasin.model';
import { IProduit } from '../../../../shared/model';
import { IMotifAjustement } from '../../../../shared/model/motif-ajustement.model';
import { Ajust, IAjust } from '../../../../shared/model/ajust.model';
import { Ajustement, IAjustement } from '../../../../shared/model/ajustement.model';
import { AjustDirection, ILotItem } from '../../models';

@Injectable({ providedIn: 'root' })
export class AjustementFacade {
  private readonly api = inject(AjustementApiService);
  private readonly config = inject(ConfigurationService);
  private readonly storageService = inject(StorageService);
  readonly store = inject(AjustementStore);

  // Expose store signals as read-only
  readonly history         = this.store.history;
  readonly totalHistory    = this.store.totalHistory;
  readonly loadingHistory  = this.store.loadingHistory;
  readonly currentAjust    = this.store.currentAjust;
  readonly lines           = this.store.lines;
  readonly loadingLines    = this.store.loadingLines;
  readonly direction       = this.store.direction;
  readonly selectedStorage = this.store.selectedStorage;
  readonly selectedMotif   = this.store.selectedMotif;
  readonly selectedProduit = this.store.selectedProduit;
  readonly gestionLot      = this.store.gestionLot;
  readonly availableLots   = this.store.availableLots;
  readonly selectedLot     = this.store.selectedLot;
  readonly loadingLots     = this.store.loadingLots;
  readonly storages        = this.store.storages;
  readonly motifs          = this.store.motifs;
  readonly isSaving        = this.store.isSaving;
  readonly error           = this.store.error;
  readonly lastEvent       = this.store.lastEvent;

  // Computed
  readonly stockActuel         = this.store.stockActuel;
  readonly lotSelectionVisible = this.store.lotSelectionVisible;
  readonly hasLines            = this.store.hasLines;

  // ── Initialisation ──────────────────────────────────────────────────────

  init(): void {
    this.loadMotifs();
    this.loadStorages();
    this.checkGestionLot();
  }

  // ── History ──────────────────────────────────────────────────────────────

  loadHistory(params: Record<string, unknown>): void {
    this.store.setLoadingHistory(true);
    this.api.listHistory({ ...params, statut: 'CLOSED' }).subscribe({
      next: res => {
        const total = parseInt(res.headers.get('X-Total-Count') ?? '0', 10);
        this.store.setHistory(res.body ?? [], total);
        this.store.emitEvent('HISTORY_LOADED');
      },
      error: err => this.handleError(err),
    });
  }

  // ── Form state ────────────────────────────────────────────────────────────

  setDirection(dir: AjustDirection): void {
    this.store.setDirection(dir);
    // Recharger les lots si un produit est déjà sélectionné et direction=IN
    const produit = this.store.selectedProduit();
    if (produit && dir === 'IN' && this.store.gestionLot()) {
      this.loadLotsForProduit(produit.id!);
    }
  }

  setStorage(storage: IStorage): void {
    this.store.setStorage(storage);
  }

  setMotif(motif: IMotifAjustement | null): void {
    this.store.setMotif(motif);
  }

  setProduit(produit: IProduit | null): void {
    this.store.setProduit(produit);
    // Charger les lots dès la sélection du produit si gestion lot active.
    // La visibilité du sélecteur est pilotée par le signe de la quantité saisie.
    if (produit && this.store.gestionLot()) {
      this.loadLotsForProduit(produit.id!);
    }
  }

  setLot(lot: ILotItem | null): void {
    this.store.setSelectedLot(lot);
  }

  // ── Line CRUD ─────────────────────────────────────────────────────────────

  /** signedQty : positif = entrée, négatif = sortie. */
  addLine(signedQty: number): void {
    const produit = this.store.selectedProduit();
    const motif   = this.store.selectedMotif();
    const storage = this.store.selectedStorage();
    const lot     = this.store.selectedLot();
    const ajust   = this.store.currentAjust();

    if (!produit || !motif || signedQty === 0) return;

    const line: IAjustement = {
      ...new Ajustement(),
      produitId: produit.id,
      qtyMvt: signedQty,
      ajustId: ajust?.id,
      motifAjustementId: motif.id,
      storageId: storage?.id,
      lotId: lot?.id,
    };

    this.store.setIsSaving(true);
    if (ajust?.id) {
      this.api.addLine(line).subscribe({
        next: () => {
          this.store.setIsSaving(false);
          this.reloadLines();
          this.store.emitEvent('LINE_ADDED');
        },
        error: err => this.handleError(err),
      });
    } else {
      const newAjust: IAjust = { ...new Ajust(), ajustements: [line] };
      this.api.create(newAjust).subscribe({
        next: res => {
          this.store.setCurrentAjust(res.body);
          this.store.setIsSaving(false);
          this.reloadLines();
          this.store.emitEvent('AJUST_CREATED', res.body);
        },
        error: err => this.handleError(err),
      });
    }
  }

  removeLine(id: number): void {
    this.api.deleteLine(id).subscribe({
      next: () => { this.reloadLines(); this.store.emitEvent('LINE_REMOVED', { id }); },
      error: err => this.handleError(err),
    });
  }

  removeLines(ids: number[]): void {
    this.api.deleteLines(ids).subscribe({
      next: () => { this.reloadLines(); this.store.emitEvent('LINE_REMOVED'); },
      error: err => this.handleError(err),
    });
  }

  updateLineQty(line: IAjustement, signedQty: number): void {
    this.api.updateLine({ ...line, qtyMvt: signedQty }).subscribe({
      next: () => { this.reloadLines(); this.store.emitEvent('LINE_UPDATED'); },
      error: err => this.handleError(err),
    });
  }

  searchLines(search: string): void {
    const id = this.store.currentAjust()?.id;
    if (!id) return;
    this.store.setLoadingLines(true);
    this.api.getLines(id, search).subscribe({
      next: res => this.store.setLines(res.body ?? []),
      error: err => this.handleError(err),
    });
  }

  // ── Finalisation ─────────────────────────────────────────────────────────

  finalise(commentaire: string): void {
    const ajust = this.store.currentAjust();
    if (!ajust?.id) return;
    this.store.setIsSaving(true);
    this.api.finalise({ ...ajust, commentaire }).subscribe({
      next: () => {
        this.store.setIsSaving(false);
        this.store.resetForm();
        this.store.emitEvent('AJUST_FINALIZED');
      },
      error: err => this.handleError(err),
    });
  }

  exportToPdf(id: number): Observable<Blob> {
    return this.api.exportToPdf(id);
  }

  resetForm(): void {
    this.store.resetForm();
  }

  // ── Private ───────────────────────────────────────────────────────────────

  private reloadLines(): void {
    const id = this.store.currentAjust()?.id;
    if (!id) return;
    this.store.setLoadingLines(true);
    this.api.getLines(id).subscribe({
      next: res => { this.store.setLines(res.body ?? []); this.store.emitEvent('LINES_LOADED'); },
      error: err => this.handleError(err),
    });
  }

  private loadMotifs(): void {
    this.api.listMotifs().subscribe({
      next: motifs => this.store.setMotifs(motifs),
      error: () => {},
    });
  }

  private loadStorages(): void {
    this.storageService.fetchUserStorages().subscribe({
      next: res => {
        const all = res.body ?? [];
        this.store.setStorages(all);
        const principal = all.find(s => s.storageType === 'PRINCIPAL') ?? all[0] ?? null;
        if (principal) this.store.setStorage(principal);
      },
      error: () => {},
    });
  }

  private loadLotsForProduit(produitId: number): void {
    this.store.setLoadingLots(true);
    this.api.getLotsForProduit(produitId).subscribe({
      next: lots => this.store.setAvailableLots(lots),
      error: () => this.store.setLoadingLots(false),
    });
  }

  private checkGestionLot(): void {
    this.config.find(Params.APP_GESTION_LOT).subscribe({
      next: res => this.store.setGestionLot(res.body?.value === '1'),
      error: () => {},
    });
  }

  private handleError(err: unknown): void {
    this.store.setIsSaving(false);
    const msg = (err as { error?: { detail?: string }; message?: string })?.error?.detail
      ?? (err as { message?: string })?.message
      ?? 'Erreur inattendue';
    this.store.setError(msg);
    this.store.emitEvent('ERROR', msg);
  }
}
