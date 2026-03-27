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
import { IReapproDashboard } from 'app/shared/model/semois/semois-dashboard.model';
import { SemoisExclureProduitComponent } from './ui/semois-exclure-produit/semois-exclure-produit.component';
import { SemoisExclusionPanelComponent } from './ui/semois-exclusion-panel/semois-exclusion-panel.component';

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
  readonly selectedClasse = signal<ClasseCriticite | null>(null);
  readonly searchText = signal<string>('');
  readonly selectedFournisseurId = signal<number | null>(null);
  readonly selectedNiveauUrgence = signal<'URGENT' | 'NORMAL' | 'OK' | null>(null);
  readonly selectedSuggestions = signal<ISemoisSuggestion[]>([]);

  // Pagination
  readonly page = signal<number>(0);
  readonly itemsPerPage = signal<number>(15);
  readonly totalItems = signal<number>(0);

  // KPI
  readonly dashboardStats = signal<IReapproDashboard | null>(null);

  // Fournisseurs distincts pour le filtre
  readonly fournisseurOptions = signal<Array<{ label: string; value: number }>>([]);

  /** Nombre d'exclusions actives — badge dans la barre d'actions. */
  readonly exclusionCount = signal<number>(0);

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
  private readonly modalService = inject(NgbModal);

  // ── Compteurs KPI (depuis dashboard, pas page courante) ──────────────────
  get urgentCount(): number { return this.dashboardStats()?.nbRupture ?? 0; }
  get normalCount(): number { return this.dashboardStats()?.nbSousSeuil ?? 0; }
  get okCount(): number { return this.dashboardStats()?.nbOk ?? 0; }


  ngOnInit(): void {
    this.loadDashboardStats();
    this.loadFournisseurs();
    this.loadSuggestions();
    this.loadExclusionCount();
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

  private loadExclusionCount(): void {
    this.semoisService.countExclusionsActives().subscribe({
      next: res => this.exclusionCount.set(res.body?.count ?? 0),
      error: () => this.exclusionCount.set(0),
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

  /**Navigation vers l'onglet Réapprovisionnement (panier auto-généré par batch). */
  naviguerVersReappro(): void {
    this.commandCommonService.suggestionsActiveSource.set('REAPPRO');
  }

  /** Ouvre le formulaire d'exclusion pour un produit. */
  ouvrirExclureProduit(suggestion: ISemoisSuggestion): void {
    const ref = this.modalService.open(SemoisExclureProduitComponent, { size: 'md', centered: true });
    ref.componentInstance.produitId = suggestion.produitId;
    ref.componentInstance.produitLibelle = suggestion.libelle;

    ref.closed.subscribe((result: { dureeJours: number; motif?: string }) => {
      this.semoisService.exclureProduit(suggestion.produitId, result).subscribe({
        next: () => {
          this.exclusionCount.update(n => n + 1);
          this.suggestions.update(list => list.filter(s => s.produitId !== suggestion.produitId));
          this.totalItems.update(n => Math.max(0, n - 1));
        },
      });
    });
  }

  /** Ouvre le panneau de gestion des exclusions actives (liste + réintégration). */
  ouvrirGestionExclusions(): void {
    const ref = this.modalService.open(SemoisExclusionPanelComponent, {
      size: 'lg',
      centered: true,
      scrollable: true,
    });

    ref.closed.subscribe((reintegratedCount: number) => {
      if (reintegratedCount > 0) {
        this.loadExclusionCount();
        this.loadSuggestions();
      }
    });
  }


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
