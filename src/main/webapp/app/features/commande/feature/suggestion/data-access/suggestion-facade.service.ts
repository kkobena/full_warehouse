import { computed, inject, Injectable, signal } from '@angular/core';
import { finalize, map } from 'rxjs/operators';
import { BudgetCommande, SemoisFraicheur, SuggestionService } from 'app/entities/commande/suggestion/suggestion.service';
import { NotificationService } from 'app/shared/services/notification.service';
import { ErrorService } from 'app/shared/error.service';
import { SuggestionLine } from 'app/entities/commande/suggestion/model/suggestion-line.model';
import { FournisseurSuggestionSummary, NiveauUrgence, SourceCalcul, SuggestionLigneEnrichie } from './suggestion-enrichie.model';
import { TauriPrinterService } from 'app/shared/services/tauri-printer.service';
import { handleBlobForTauri } from 'app/shared/util/tauri-util';
import { saveAs } from 'file-saver';
import { IFournisseurProduit } from 'app/shared/model/fournisseur-produit.model';
import { Keys } from 'app/shared/model/keys.model';

@Injectable({ providedIn: 'root' })
export class SuggestionFacadeService {
  private readonly suggestionService = inject(SuggestionService);
  private readonly notificationService = inject(NotificationService);
  private readonly errorService = inject(ErrorService);
  private readonly tauriPrinterService = inject(TauriPrinterService);

  // ─── Signals ────────────────────────────────────────────────────────────────
  readonly fournisseurs = signal<FournisseurSuggestionSummary[]>([]);
  readonly selectedFournisseur = signal<FournisseurSuggestionSummary | null>(null);
  readonly budget = signal<BudgetCommande | null>(null);
  readonly lignesEnrichies = signal<SuggestionLigneEnrichie[]>([]);
  readonly loading = signal(false);
  readonly loadingLignes = signal(false);
  readonly semoisFraicheur = signal<SemoisFraicheur | null>(null);
  readonly recalculEnCours = signal(false);
  readonly fournisseursProduit = signal<IFournisseurProduit[]>([]);
  readonly loadingComparaison = signal(false);

  // ─── Pagination & filtre (backend-driven) ───────────────────────────────────
  readonly page = signal(0);
  readonly rows = signal(20);
  readonly totalLignes = signal(0);
  readonly searchText = signal('');
  readonly urgenceFilter = signal('TOUS');

  // ─── Computed ────────────────────────────────────────────────────────────────
  readonly totalMontantEstime = computed(() =>
    this.fournisseurs().reduce((s, f) => s + f.montantEstime, 0),
  );

  readonly nbUrgentsTotal = computed(() =>
    this.fournisseurs().reduce((s, f) => s + f.nbUrgents, 0),
  );

  readonly lignesSelectionnees = computed(() =>
    this.lignesEnrichies().filter(l => l.selected),
  );

  readonly montantSelection = computed(() =>
    this.lignesSelectionnees().reduce((s, l) => s + l.quantite * l.prixAchat, 0),
  );

  // Contexte du fournisseur courant pour les rechargements
  private currentSuggestionId: number | null = null;
  private currentFournisseurId: number | null = null;

  // ─── Public methods ──────────────────────────────────────────────────────────

  /** Charge toutes les suggestions et le budget en parallèle. */
  loadAll(): void {
    this.loading.set(true);
    this.fournisseurs.set([]);
    this.selectedFournisseur.set(null);
    this.lignesEnrichies.set([]);
    this.currentSuggestionId = null;
    this.currentFournisseurId = null;

    this.suggestionService
      .queryParFournisseur()
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: summaries => {
          this.fournisseurs.set(summaries);
        },
        error: err => {
          this.notificationService.error(this.errorService.getErrorMessage(err), 'Chargement');
        },
      });

    this.suggestionService.getBudget().subscribe({
      next: b => this.budget.set(b),
      error: () => this.budget.set(null),
    });

    this.suggestionService.getSemoisFraicheur().subscribe({
      next: f => this.semoisFraicheur.set(f),
      error: () => this.semoisFraicheur.set(null),
    });
  }

  /** Déclenche un recalcul SEMOIS manuel puis recharge la fraîcheur. */
  recalculerSemois(): void {
    this.recalculEnCours.set(true);
    this.suggestionService.recalculerSemois().subscribe({
      next: () => {
        this.notificationService.success('Recalcul SEMOIS déclenché. Les données seront mises à jour dans quelques instants.', 'SEMOIS');
        this.recalculEnCours.set(false);
        this.loadAll();
      },
      error: err => {
        this.notificationService.error(this.errorService.getErrorMessage(err), 'Recalcul SEMOIS');
        this.recalculEnCours.set(false);
      },
    });
  }

  /** Sélectionne un fournisseur et charge la première page de ses lignes. */
  selectFournisseur(fournisseur: FournisseurSuggestionSummary): void {
    this.selectedFournisseur.set(fournisseur);
    this.page.set(0);
    this.searchText.set('');
    this.urgenceFilter.set('TOUS');

    if (!fournisseur.suggestionId) {
      this.lignesEnrichies.set([]);
      this.totalLignes.set(0);
      return;
    }
    this.currentSuggestionId = fournisseur.suggestionId;
    this.currentFournisseurId = fournisseur.fournisseurId;
    this.loadLignes();
  }

  /** Applique un filtre texte + urgence et recharge depuis la page 0. */
  applyFilter(search: string, urgence: string): void {
    this.searchText.set(search);
    this.urgenceFilter.set(urgence);
    this.page.set(0);
    this.loadLignes();
  }

  /** Change de page et recharge. */
  changePage(page: number, rows: number): void {
    this.page.set(page);
    this.rows.set(rows);
    this.loadLignes();
  }

  /** Recharge les lignes avec les paramètres courants (page, filtre). */
  loadLignes(): void {
    if (this.currentSuggestionId == null) return;
    this.loadLignesForFournisseur(this.currentSuggestionId, this.currentFournisseurId!);
  }

  /**
   * Lance la commande pour les lignes fournies (sélection ou toutes les lignes).
   * Utilise l'endpoint commander-selection pour respecter les quantités modifiées.
   */
  commander(lignes?: SuggestionLigneEnrichie[]): void {
    const fournisseur = this.selectedFournisseur();
    if (!fournisseur?.suggestionId) {
      this.notificationService.warning('Aucun fournisseur sélectionné.');
      return;
    }

    const linesToCommander = lignes ?? this.lignesEnrichies();
    const selection = linesToCommander
      .filter(l => l.id != null)
      .map(l => ({ suggestionLineId: l.id!, quantite: l.quantite }));

    if (selection.length === 0) {
      this.notificationService.warning('Aucune ligne à commander.');
      return;
    }

    this.suggestionService
      .commanderSelection({ suggestionId: fournisseur.suggestionId, lignes: selection })
      .subscribe({
        next: () => {
          this.notificationService.success(
            `Commande passée avec succès pour ${fournisseur.libelle}.`,
            'Commander',
          );
          this.loadAll();
        },
        error: err => {
          this.notificationService.error(this.errorService.getErrorMessage(err), 'Commander');
        },
      });
  }

  /** Met à jour la quantité d'une ligne via l'API puis met à jour le signal. */
  updateQuantite(ligne: SuggestionLigneEnrichie, qte: number): void {
    if (!ligne.id) return;
    const updatedLine: SuggestionLine = {
      id: ligne.id,
      quantity: qte,
      produitId: ligne.produitId,
      fournisseurProduitId: ligne.fournisseurProduitId,
    };
    this.suggestionService.updateQuantity(updatedLine).subscribe({
      next: () => {
        this.setQuantite(ligne, qte);
      },
      error: err => {
        this.notificationService.error(this.errorService.getErrorMessage(err), 'Mise à jour quantité');
      },
    });
  }

  /** Exporte la suggestion en PDF (Tauri : boîte de dialogue de sauvegarde ; browser : ouvre dans un onglet). */
  exporterPdf(): void {
    const fournisseur = this.selectedFournisseur();
    if (!fournisseur?.suggestionId) return;
    const filename = `suggestion_${fournisseur.suggestionId}`;
    this.suggestionService.exportToPdf(fournisseur.suggestionId).subscribe({
      next: blob => {
        if (this.tauriPrinterService.isRunningInTauri()) {
          handleBlobForTauri(blob, filename);
        } else {
          window.open(URL.createObjectURL(blob));
        }
      },
      error: err => {
        this.notificationService.error(this.errorService.getErrorMessage(err), 'Export PDF');
      },
    });
  }

  /** Exporte la suggestion en CSV. */
  exporterCsv(): void {
    const fournisseur = this.selectedFournisseur();
    if (!fournisseur?.suggestionId) return;
    const filename = `suggestion_${fournisseur.suggestionId}.csv`;
    this.suggestionService.exportToCsv(fournisseur.suggestionId).subscribe({
      next: blob => {
        if (this.tauriPrinterService.isRunningInTauri()) {
          handleBlobForTauri(blob, `suggestion_${fournisseur.suggestionId}`,'csv');
        } else {
          saveAs(blob, filename);
        }
      },
      error: err => {
        this.notificationService.error(this.errorService.getErrorMessage(err), 'Export CSV');
      },
    });
  }

  /** Valide une suggestion (GENEREE → VALIDEE). */
  valider(id: number): void {
    this.suggestionService.valider(id).subscribe({
      next: () => {
        this.notificationService.success('Suggestion validée.', 'Valider');
        this.loadAll();
      },
      error: err => {
        this.notificationService.error(this.errorService.getErrorMessage(err), 'Valider');
      },
    });
  }

  /** Rejette (supprime) une suggestion. */
  rejeter(id: number): void {
    this.suggestionService.rejeter(id).subscribe({
      next: () => {
        this.notificationService.success('Suggestion rejetée.', 'Rejeter');
        this.loadAll();
      },
      error: err => {
        this.notificationService.error(this.errorService.getErrorMessage(err), 'Rejeter');
      },
    });
  }

  /** Nettoie (sanitize) une suggestion puis recharge la liste. */
  sanitize(id: number): void {
    this.suggestionService.sanitize(id).subscribe({
      next: () => {
        this.notificationService.success('Suggestion nettoyée.', 'Sanitize');
        this.loadAll();
      },
      error: err => {
        this.notificationService.error(this.errorService.getErrorMessage(err), 'Sanitize');
      },
    });
  }

  /** Inverse la sélection d'une ligne. */
  toggleSelection(ligne: SuggestionLigneEnrichie): void {
    this.lignesEnrichies.update(lignes =>
      lignes.map(l => (l === ligne ? { ...l, selected: !l.selected } : l)),
    );
  }

  /** Sélectionne ou désélectionne toutes les lignes. */
  toggleTout(selected: boolean): void {
    this.lignesEnrichies.update(lignes => lignes.map(l => ({ ...l, selected })));
  }

  /** Remet la quantité d'une ligne à sa valeur calculée. */
  resetQuantite(ligne: SuggestionLigneEnrichie): void {
    this.lignesEnrichies.update(lignes =>
      lignes.map(l =>
        l === ligne ? { ...l, quantite: l.quantiteCalculee, quantiteModifiee: false } : l,
      ),
    );
  }

  /** Met à jour la quantité d'une ligne dans le signal local. */
  setQuantite(ligne: SuggestionLigneEnrichie, qte: number): void {
    this.lignesEnrichies.update(lignes =>
      lignes.map(l =>
        l === ligne
          ? { ...l, quantite: qte, quantiteModifiee: qte !== l.quantiteCalculee }
          : l,
      ),
    );
  }

  /** Met à jour la sélection de plusieurs lignes en une opération. */
  setSelections(lignes: SuggestionLigneEnrichie[]): void {
    const selectedSet = new Set(lignes.map(l => l.id ?? `${l.produitId}-${l.libelle}`));
    this.lignesEnrichies.update(current =>
      current.map(l => {
        const key = l.id ?? `${l.produitId}-${l.libelle}`;
        return { ...l, selected: selectedSet.has(key) };
      }),
    );
  }

  /** Supprime une ligne de suggestion. */
  supprimerLigne(id: number): void {
    const keys: Keys = { ids: [id] };
    this.suggestionService.deleteItem(keys).subscribe({
      next: () => {
        this.lignesEnrichies.update(lignes => lignes.filter(l => l.id !== id));
        this.totalLignes.update(n => Math.max(0, n - 1));
        const fournisseur = this.selectedFournisseur();
        if (fournisseur) {
          this.fournisseurs.update(list =>
            list.map(f =>
              f.fournisseurId === fournisseur.fournisseurId
                ? { ...f, nbProduits: Math.max(0, f.nbProduits - 1) }
                : f,
            ),
          );
        }
        this.notificationService.success('Produit retiré de la suggestion.', 'Suppression');
      },
      error: err => {
        this.notificationService.error(this.errorService.getErrorMessage(err), 'Suppression ligne');
      },
    });
  }

  /** Supprime plusieurs lignes sélectionnées. */
  supprimerLignes(ids: number[]): void {
    if (ids.length === 0) return;
    const keys: Keys = { ids };
    this.suggestionService.deleteItem(keys).subscribe({
      next: () => {
        const idsSet = new Set(ids);
        this.lignesEnrichies.update(lignes => lignes.filter(l => l.id == null || !idsSet.has(l.id)));
        this.totalLignes.update(n => Math.max(0, n - ids.length));
        const fournisseur = this.selectedFournisseur();
        if (fournisseur) {
          this.fournisseurs.update(list =>
            list.map(f =>
              f.fournisseurId === fournisseur.fournisseurId
                ? { ...f, nbProduits: Math.max(0, f.nbProduits - ids.length) }
                : f,
            ),
          );
        }
        this.notificationService.success(`${ids.length} produit(s) retiré(s) de la suggestion.`, 'Suppression');
      },
      error: err => {
        this.notificationService.error(this.errorService.getErrorMessage(err), 'Suppression lignes');
      },
    });
  }

  /** Supprime une ou plusieurs suggestions. Désélectionne si la suggestion courante est supprimée. */
  supprimerSuggestions(ids: number[]): void {
    if (ids.length === 0) return;
    const keys: Keys = { ids };
    this.suggestionService.delete(keys).subscribe({
      next: () => {
        this.notificationService.success(
          ids.length === 1 ? 'Suggestion supprimée.' : `${ids.length} suggestions supprimées.`,
          'Suppression',
        );
        const selected = this.selectedFournisseur();
        if (selected?.suggestionId && ids.includes(selected.suggestionId)) {
          this.selectedFournisseur.set(null);
          this.lignesEnrichies.set([]);
          this.currentSuggestionId = null;
          this.currentFournisseurId = null;
        }
        this.loadAll();
      },
      error: err => {
        this.notificationService.error(this.errorService.getErrorMessage(err), 'Suppression suggestion');
      },
    });
  }

  /** Fusionne plusieurs suggestions du même fournisseur. */
  fusionnerSuggestions(ids: number[]): void {
    if (ids.length < 2) return;
    const keys: Keys = { ids };
    this.suggestionService.fusionner(keys).subscribe({
      next: () => {
        this.notificationService.success('Suggestions fusionnées avec succès.', 'Fusion');
        this.selectedFournisseur.set(null);
        this.lignesEnrichies.set([]);
        this.currentSuggestionId = null;
        this.currentFournisseurId = null;
        this.loadAll();
      },
      error: err => {
        this.notificationService.error(this.errorService.getErrorMessage(err), 'Fusion suggestions');
      },
    });
  }

  /** Ajoute un produit à la suggestion courante puis recharge les lignes. */
  ajouterProduit(produitId: number, fournisseurProduitId: number, quantite = 1): void {
    const fournisseur = this.selectedFournisseur();
    if (!fournisseur?.suggestionId) {
      this.notificationService.warning('Aucun fournisseur sélectionné.');
      return;
    }
    const item: SuggestionLine = { produitId, fournisseurProduitId, quantity: quantite };
    this.suggestionService.createOrUpdateItem(item, fournisseur.suggestionId).subscribe({
      next: () => {
        this.notificationService.success('Produit ajouté à la suggestion.', 'Ajout');
        this.loadLignes();
      },
      error: err => {
        this.notificationService.error(this.errorService.getErrorMessage(err), 'Ajout produit');
      },
    });
  }

  /** Charge les fournisseurs d'un produit pour la vue comparaison, triés : principal d'abord puis prix croissant. */
  loadFournisseursProduit(produitId: number): void {
    this.loadingComparaison.set(true);
    this.fournisseursProduit.set([]);
    this.suggestionService
      .getFournisseursProduit(produitId)
      .pipe(finalize(() => this.loadingComparaison.set(false)))
      .subscribe({
        next: data => {
          const sorted = [...data].sort((a, b) => {
            if (a.principal && !b.principal) return -1;
            if (!a.principal && b.principal) return 1;
            return (a.prixAchat ?? 0) - (b.prixAchat ?? 0);
          });
          this.fournisseursProduit.set(sorted);
        },
        error: err => {
          this.notificationService.error(this.errorService.getErrorMessage(err), 'Comparaison fournisseurs');
        },
      });
  }

  // ─── Private helpers ─────────────────────────────────────────────────────────

  private loadLignesForFournisseur(suggestionId: number, fournisseurId: number): void {
    this.loadingLignes.set(true);
    this.lignesEnrichies.set([]);

    const req: Record<string, any> = { suggestionId, page: this.page(), size: this.rows() };
    const search = this.searchText();
    const urgence = this.urgenceFilter();
    if (search) req['search'] = search;
    if (urgence && urgence !== 'TOUS') req['niveauUrgence'] = urgence;

    this.suggestionService
      .queryItems(req)
      .pipe(
        map(res => {
          const total = Number(res.headers.get('X-Total-Count') ?? res.body?.length ?? 0);
          this.totalLignes.set(total);
          return res.body ?? [];
        }),
        finalize(() => this.loadingLignes.set(false)),
      )
      .subscribe({
        next: lines => {
          const enrichies = lines.map(line => this.toEnrichie(line));
          this.lignesEnrichies.set(enrichies);
          const nbUrgents = enrichies.filter(l => l.niveauUrgence === 'URGENT').length;
          this.fournisseurs.update(list =>
            list.map(f => f.fournisseurId === fournisseurId ? { ...f, nbUrgents } : f),
          );
        },
        error: err => {
          this.notificationService.error(this.errorService.getErrorMessage(err), 'Chargement lignes');
        },
      });
  }

  private toEnrichie(line: SuggestionLine): SuggestionLigneEnrichie {
    const quantiteCalculee = line.quantity ?? 0;
    return {
      id: line.id,
      produitId: line.produitId,
      fournisseurProduitId: line.fournisseurProduitId,
      libelle: line.fournisseurProduitLibelle ?? '',
      codeCip: line.fournisseurProduitCip ?? '',
      currentStock: line.currentStock ?? 0,
      quantite: quantiteCalculee,
      quantiteCalculee,
      prixAchat: line.prixAchat ?? 0,
      prixVente: line.prixVente,
      etatProduit: line.etatProduit,
      consommationMensuelle: line.consommationMensuelle,
      niveauUrgence: (line.niveauUrgence ?? 'OK') as NiveauUrgence,
      joursRestants: line.joursRestants ?? null,
      tauxCouvertureMois: null,
      quantiteSemois: undefined,
      classeCriticite: undefined,
      sourceCalcul: (line.sourceCalcul ?? 'CLASSIQUE') as SourceCalcul,
      selected: false,
      quantiteModifiee: false,
      commandee: false,
    };
  }

}

