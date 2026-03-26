import { Component, OnInit, inject, signal } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { SelectModule } from 'primeng/select';
import { Tag } from 'primeng/tag';
import { InputTextModule } from 'primeng/inputtext';
import { TooltipModule } from 'primeng/tooltip';

import { NgbModal } from '@ng-bootstrap/ng-bootstrap';

import { ISemoisSuggestion, SemoisSuggestion } from 'app/shared/model/semois/semois-suggestion.model';
import { ClasseCriticite, getClasseCriticiteInfo, CLASSE_CRITICITE_INFO } from 'app/shared/model/semois/classe-criticite.model';
import { SemoisService } from 'app/entities/semois/semois.service';
import { WarehouseCommonModule } from 'app/shared/warehouse-common/warehouse-common.module';
import { CommandCommonService } from 'app/entities/commande/command-common.service';
import { NotificationService } from 'app/shared/services/notification.service';
import { ErrorService } from 'app/shared/error.service';
import { SemoisCommanderModalComponent, SemoisCommandeLine } from './ui/semois-commander-modal/semois-commander-modal.component';
import { IReapproDashboard } from 'app/shared/model/semois/semois-dashboard.model';

@Component({
  selector: 'app-semois-suggestions',
  templateUrl: './semois-suggestions.component.html',
  styleUrls: ['./semois-suggestions.component.scss'],
  imports: [
    CommonModule,
    FormsModule,
    TableModule,
    ButtonModule,
    SelectModule,
    WarehouseCommonModule,
    Tag,
    InputTextModule,
    TooltipModule,
  ],
})
export class SemoisSuggestionsComponent implements OnInit {
  readonly suggestions = signal<ISemoisSuggestion[]>([]);
  readonly isLoading = signal<boolean>(false);
  readonly isCommanding = signal<boolean>(false);
  readonly selectedClasse = signal<ClasseCriticite | null>(null);
  readonly searchText = signal<string>('');
  readonly selectedFournisseurId = signal<number | null>(null);
  readonly selectedNiveauUrgence = signal<'URGENT' | 'NORMAL' | 'OK' | null>(null);
  readonly selectedSuggestions = signal<ISemoisSuggestion[]>([]);

  // Pagination
  readonly page = signal<number>(0);
  readonly itemsPerPage = signal<number>(15);
  readonly totalItems = signal<number>(0);

  // KPI — chargés depuis le dashboard (total, pas page courante)
  readonly dashboardStats = signal<IReapproDashboard | null>(null);

  // Fournisseurs distincts pour le filtre
  readonly fournisseurOptions = signal<Array<{ label: string; value: number }>>([]);

  /** Options pour le sélecteur de classe */
  readonly classeOptions = Object.entries(CLASSE_CRITICITE_INFO).map(([key, info]) => ({
    label: info.label,
    value: key as ClasseCriticite,
  }));

  /** Options pour le filtre urgence — lexique métier */
  readonly urgenceOptions: Array<{ label: string; value: 'URGENT' | 'NORMAL' | 'OK' }> = [
    { label: 'Rupture', value: 'URGENT' },
    { label: 'Sous seuil', value: 'NORMAL' },
    { label: 'Suffisant', value: 'OK' },
  ];

  private readonly semoisService = inject(SemoisService);
  private readonly commandCommonService = inject(CommandCommonService);
  private readonly notificationService = inject(NotificationService);
  private readonly errorService = inject(ErrorService);
  private readonly modalService = inject(NgbModal);

  // ── Compteurs KPI (depuis dashboard, pas page courante) ──────────────────
  get urgentCount(): number { return this.dashboardStats()?.nbRupture ?? 0; }
  get normalCount(): number { return this.dashboardStats()?.nbSousSeuil ?? 0; }
  get okCount(): number { return this.dashboardStats()?.nbOk ?? 0; }

  /** Nombre de lignes sélectionnées avec quantité > 0 */
  get nbSelectionCommandable(): number {
    return this.selectedSuggestions().filter(s => (s.quantiteACommander ?? 0) > 0).length;
  }

  /** Nombre de lignes commandables sur la page courante (selection ou page entière) */
  get nbCommandable(): number {
    const sel = this.nbSelectionCommandable;
    if (sel > 0) return sel;
    return this.suggestions().filter(s => (s.quantiteACommander ?? 0) > 0).length;
  }

  get commanderLabel(): string {
    const n = this.nbCommandable;
    return n > 0 ? `Commander (${n})` : 'Commander';
  }

  get commanderTooltip(): string {
    const sel = this.nbSelectionCommandable;
    if (sel > 0) return `Commander les ${sel} ligne(s) sélectionnée(s)`;
    const page = this.suggestions().filter(s => (s.quantiteACommander ?? 0) > 0).length;
    return page > 0 ? `Commander les ${page} article(s) de la page courante` : 'Aucune ligne à commander';
  }

  ngOnInit(): void {
    this.loadDashboardStats();
    this.loadFournisseurs();
    this.loadSuggestions();
  }

  // ── Chargement ────────────────────────────────────────────────────────────

  private loadDashboardStats(): void {
    this.semoisService.getDashboard().subscribe({
      next: (res: HttpResponse<IReapproDashboard>) => this.dashboardStats.set(res.body),
      error: () => this.dashboardStats.set(null),
    });
  }

  private loadFournisseurs(): void {
    this.semoisService.getSemoisFournisseurs().subscribe({
      next: res => {
        const opts = (res.body ?? []).map(f => ({ label: f.fournisseurLibelle, value: f.fournisseurId }));
        this.fournisseurOptions.set(opts);
      },
      error: () => this.fournisseurOptions.set([]),
    });
  }

  loadSuggestions(): void {
    this.isLoading.set(true);
    this.selectedSuggestions.set([]);

    const req = { page: this.page(), size: this.itemsPerPage() };

    this.semoisService.getSuggestions(
      req,
      this.searchText() || undefined,
      this.selectedClasse() ?? undefined,
      this.selectedFournisseurId() ?? undefined,
      this.selectedNiveauUrgence() ?? undefined,
    ).subscribe({
      next: (res: HttpResponse<ISemoisSuggestion[]>) => {
        this.suggestions.set(res.body ?? []);
        this.totalItems.set(Number(res.headers.get('X-Total-Count') ?? 0));
        this.isLoading.set(false);
      },
      error: () => this.isLoading.set(false),
    });
  }

  // ── Pagination ────────────────────────────────────────────────────────────

  /** PrimeNG 20 TablePageEvent n'a que first/rows (pas page). */
  onPageChange(event: { first: number; rows: number }): void {
    this.page.set(Math.floor(event.first / event.rows));
    this.itemsPerPage.set(event.rows);
    this.loadSuggestions();
  }

  // ── Filtres ───────────────────────────────────────────────────────────────

  onFilterChange(): void { this.page.set(0); this.loadSuggestions(); }
  onSearchChange(): void { this.page.set(0); this.loadSuggestions(); }

  onClearFilters(): void {
    this.selectedClasse.set(null);
    this.searchText.set('');
    this.selectedFournisseurId.set(null);
    this.selectedNiveauUrgence.set(null);
    this.page.set(0);
    this.loadSuggestions();
  }

  navigateToDashboard(): void {
    this.commandCommonService.updateCommandPreviousActiveNav('DASHBOARD');
  }

  // ── Commander ─────────────────────────────────────────────────────────────

  /**
   * Point d'entrée unique du bouton "Commander".
   * - Si des lignes sont cochées → commander la sélection
   * - Sinon → commander toutes les lignes commandables de la page courante
   */
  commander(): void {
    if (this.nbSelectionCommandable > 0) {
      this.commanderSelection();
    } else {
      this.commanderPageCourante();
    }
  }

  /** Commander les lignes commandables de la page courante (sans sélection explicite). */
  private commanderPageCourante(): void {
    const lignes = this.suggestions().filter(s => (s.quantiteACommander ?? 0) > 0);
    if (lignes.length === 0) {
      this.notificationService.info('Aucune ligne à commander sur cette page.');
      return;
    }
    this.ouvrirModalCommande(lignes, `Commander ${lignes.length} article(s)`);
  }

  /** Commander la sélection courante (checkboxes). */
  commanderSelection(): void {
    const selection = this.selectedSuggestions().filter(s => (s.quantiteACommander ?? 0) > 0);
    if (selection.length === 0) {
      this.notificationService.warning('Sélectionnez au moins une ligne avec une quantité à commander.');
      return;
    }
    this.ouvrirModalCommande(selection, `Commander ${selection.length} article(s) sélectionné(s)`);
  }

  /** Commander TOUS les urgents (stockActuel < margeSecurite) — appel API dédié. */
  commanderUrgents(): void {
    this.isCommanding.set(true);
    this.semoisService.getAllUrgentSuggestions().subscribe({
      next: res => {
        const urgents = (res.body ?? []).filter(s => (s.quantiteACommander ?? 0) > 0);
        this.isCommanding.set(false);
        if (urgents.length === 0) {
          this.notificationService.info('Aucun produit urgent avec quantité à commander.');
          return;
        }
        this.ouvrirModalCommande(urgents, `Commander ${urgents.length} article(s) urgents`);
      },
      error: err => {
        this.notificationService.error(this.errorService.getErrorMessage(err), 'Urgents');
        this.isCommanding.set(false);
      },
    });
  }

  /** Commander tous les produits du fournisseur sélectionné. */
  commanderParFournisseur(): void {
    const fId = this.selectedFournisseurId();
    if (!fId) {
      this.notificationService.warning('Sélectionnez un fournisseur dans le filtre.');
      return;
    }
    const lignes = this.suggestions().filter(s => (s.quantiteACommander ?? 0) > 0 && s.fournisseurId === fId);
    if (lignes.length === 0) {
      this.notificationService.info('Aucune ligne à commander pour ce fournisseur sur cette page.');
      return;
    }
    const lib = this.fournisseurOptions().find(f => f.value === fId)?.label ?? '';
    this.ouvrirModalCommande(lignes, `Commander — ${lib}`);
  }

  /** Commander tous les produits de la classe sélectionnée. */
  commanderParClasse(): void {
    const cl = this.selectedClasse();
    if (!cl) {
      this.notificationService.warning('Sélectionnez une classe dans le filtre.');
      return;
    }
    const lignes = this.suggestions().filter(s => (s.quantiteACommander ?? 0) > 0 && s.classeCriticite === cl);
    if (lignes.length === 0) {
      this.notificationService.info('Aucune ligne à commander pour cette classe sur cette page.');
      return;
    }
    this.ouvrirModalCommande(lignes, `Commander — Classe ${cl}`);
  }

  private ouvrirModalCommande(lignes: ISemoisSuggestion[], titre: string): void {
    const commandeLines: SemoisCommandeLine[] = lignes.map(s => ({
      produitId: s.produitId!,
      fournisseurId: s.fournisseurId ?? 0,
      libelle: s.libelle!,
      fournisseurLibelle: s.fournisseurLibelle ?? 'Inconnu',
      quantite: s.quantiteACommander ?? 0,
      urgence: this.getUrgenceLabel(s),
    }));

    const modalRef = this.modalService.open(SemoisCommanderModalComponent, {
      size: 'lg', scrollable: true, centered: true,
    });
    modalRef.componentInstance.lignes = commandeLines;
    modalRef.componentInstance.titre = titre;

    modalRef.result.then(result => {
      if (result === 'confirmed') this.executerCommande(commandeLines);
    }).catch(() => {});
  }

  private executerCommande(lignes: SemoisCommandeLine[]): void {
    this.isCommanding.set(true);
    const payload = lignes
      .filter(l => l.fournisseurId > 0 && l.quantite > 0)
      .map(l => ({ produitId: l.produitId, fournisseurId: l.fournisseurId, quantite: l.quantite }));

    this.semoisService.commanderSemois(payload).subscribe({
      next: () => {
        const nbFournisseurs = new Set(payload.map(p => p.fournisseurId)).size;
        this.notificationService.success(
          `${payload.length} article(s) commandé(s) → ${nbFournisseurs} commande(s) créée(s).`, 'Commander SEMOIS');
        this.isCommanding.set(false);
        this.selectedSuggestions.set([]);
        this.loadDashboardStats();
        this.loadSuggestions();
      },
      error: err => {
        this.notificationService.error(this.errorService.getErrorMessage(err), 'Commander SEMOIS');
        this.isCommanding.set(false);
      },
    });
  }

  // ── Utilitaires d'affichage ───────────────────────────────────────────────

  private toModel(s: ISemoisSuggestion): SemoisSuggestion {
    return new SemoisSuggestion(
      s.produitId, s.libelle, s.codeCip, s.fournisseurId, s.fournisseurLibelle,
      s.classeCriticite, s.vmm, s.margeSecurite, s.stockObjectif, s.stockActuel,
      s.quantiteACommander, s.delaiLivraisonJours, s.coefficientSecurite, s.facteurSaisonnier, s.dateDernierCalcul,
    );
  }

  getUrgenceLabel(s: ISemoisSuggestion): string { return this.toModel(s).getNiveauUrgence(); }

  /** Libellé métier affiché dans la colonne Urgence. */
  getUrgenceDisplayLabel(s: ISemoisSuggestion): string {
    switch (this.getUrgenceLabel(s)) {
      case 'URGENT': return 'Rupture';
      case 'NORMAL': return 'Sous seuil';
      default:       return 'Suffisant';
    }
  }

  getUrgenceSeverity(s: ISemoisSuggestion): 'danger' | 'warn' | 'success' {
    switch (this.getUrgenceLabel(s)) {
      case 'URGENT': return 'danger';
      case 'NORMAL': return 'warn';
      default:       return 'success';
    }
  }

  getClasseLabel(c?: ClasseCriticite): string { return getClasseCriticiteInfo(c)?.label ?? '-'; }
  getClasseSeverity(c?: ClasseCriticite): 'danger' | 'success' | 'info' | 'warn' | 'secondary' {
    return getClasseCriticiteInfo(c)?.severity ?? 'secondary';
  }

  getStockActuelClass(s: ISemoisSuggestion): string {
    if ((s.stockActuel ?? 0) < 0) return 'text-danger fw-bold';   // stock négatif
    const m = this.toModel(s);
    if (m.estEnRupture())  return 'text-danger fw-bold';
    if (m.estEnSurstock()) return 'text-warning';
    return '';
  }

  getCouvertureMois(s: ISemoisSuggestion): number {
    // couverture basée sur le stock effectif (≥ 0)
    const stockEffectif = Math.max(0, s.stockActuel ?? 0);
    return (!s.vmm || s.vmm === 0) ? 0 : stockEffectif / s.vmm;
  }

  getCouvertureClass(s: ISemoisSuggestion): string {
    if ((s.stockActuel ?? 0) <= 0) return 'bg-danger-subtle text-danger';
    const c = this.getCouvertureMois(s);
    if (c < 0.5) return 'bg-danger-subtle text-danger';
    if (c < 1.0) return 'bg-warning-subtle text-warning';
    if (c <= 2.0) return 'bg-success-subtle text-success';
    return 'bg-info-subtle text-info';
  }

  getCouvertureCibleMois(s: ISemoisSuggestion): number {
    return (!s.vmm || s.vmm === 0) ? 0 : (s.stockObjectif ?? 0) / s.vmm;
  }

  getCouvertureCibleClass(s: ISemoisSuggestion): string {
    const c = this.getCouvertureCibleMois(s);
    if (c < 0.5) return 'text-danger';
    if (c < 1.0) return 'text-warning';
    if (c <= 3.0) return 'text-success';
    return 'text-info';
  }

  getJoursRestants(s: ISemoisSuggestion): number { return this.toModel(s).getJoursStockRestant(); }

  getJoursRestantsClass(s: ISemoisSuggestion): string {
    if ((s.stockActuel ?? 0) <= 0) return 'bg-danger-subtle text-danger';
    const j = this.getJoursRestants(s);
    if (j <= 7)  return 'bg-danger-subtle text-danger';
    if (j <= 14) return 'bg-warning-subtle text-warning';
    if (j <= 30) return 'bg-success-subtle text-success';
    return 'bg-info-subtle text-info';
  }

  /** Affiche "Rupture" si stock ≤ 0, sinon le nombre de jours. */
  getJoursRestantsLabel(s: ISemoisSuggestion): string {
    return (s.stockActuel ?? 0) <= 0 ? 'Rupture' : `${this.getJoursRestants(s)} jours`;
  }

  getRowClass(s: ISemoisSuggestion): string {
    switch (this.getUrgenceLabel(s)) {
      case 'URGENT': return 'table-danger';
      case 'NORMAL': return 'table-warning';
      default:       return '';
    }
  }
}
