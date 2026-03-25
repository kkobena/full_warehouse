import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { SelectModule } from 'primeng/select';
import { ToolbarModule } from 'primeng/toolbar';
import { Tag } from 'primeng/tag';
import { Drawer } from 'primeng/drawer';
import { InputTextModule } from 'primeng/inputtext';
import { TooltipModule } from 'primeng/tooltip';

import { ISemoisSuggestion, SemoisSuggestion } from 'app/shared/model/semois/semois-suggestion.model';
import { ClasseCriticite, getClasseCriticiteInfo, CLASSE_CRITICITE_INFO } from 'app/shared/model/semois/classe-criticite.model';
import { SemoisService } from 'app/entities/semois/semois.service';
import { WarehouseCommonModule } from 'app/shared/warehouse-common/warehouse-common.module';
import { CommandCommonService } from 'app/entities/commande/command-common.service';

@Component({
  standalone: true,
  selector: 'app-semois-suggestions',
  templateUrl: './semois-suggestions.component.html',
  styleUrls: ['./semois-suggestions.component.scss'],
  imports: [
    CommonModule,
    FormsModule,
    TableModule,
    ButtonModule,
    SelectModule,
    ToolbarModule,
    WarehouseCommonModule,
    Tag,
    Drawer,
    InputTextModule,
    TooltipModule,
  ],
})
export class SemoisSuggestionsComponent implements OnInit {
  readonly suggestions = signal<ISemoisSuggestion[]>([]);
  readonly isLoading = signal<boolean>(false);
  readonly selectedClasse = signal<ClasseCriticite | null>(null);
  readonly urgenceFilter = signal<string | null>(null);
  readonly searchText = signal<string>('');
  readonly helpDrawerVisible = signal<boolean>(false);

  // Pagination
  readonly page = signal<number>(0);
  readonly itemsPerPage = signal<number>(20);
  readonly totalItems = signal<number>(0);

  /** Options pour le sélecteur de classe */
  readonly classeOptions = Object.entries(CLASSE_CRITICITE_INFO).map(([key, info]) => ({
    label: info.label,
    value: key as ClasseCriticite,
  }));

  private readonly semoisService = inject(SemoisService);
  private readonly router = inject(Router);
  private readonly commandCommonService = inject(CommandCommonService);

  // Compteurs calculés
  readonly urgentCount = computed(
    () =>
      this.suggestions().filter(s =>
        new SemoisSuggestion(
          s.produitId, s.libelle, s.codeCip, s.fournisseurId, s.fournisseurLibelle,
          s.classeCriticite, s.vmm, s.margeSecurite, s.stockObjectif, s.stockActuel,
          s.quantiteACommander, s.delaiLivraisonJours, s.coefficientSecurite,
          s.facteurSaisonnier, s.dateDernierCalcul,
        ).estEnRupture(),
      ).length,
  );

  readonly normalCount = computed(
    () =>
      this.suggestions().filter(s => {
        const suggestion = new SemoisSuggestion(
          s.produitId, s.libelle, s.codeCip, s.fournisseurId, s.fournisseurLibelle,
          s.classeCriticite, s.vmm, s.margeSecurite, s.stockObjectif, s.stockActuel,
          s.quantiteACommander, s.delaiLivraisonJours, s.coefficientSecurite,
          s.facteurSaisonnier, s.dateDernierCalcul,
        );
        return !suggestion.estEnRupture() && suggestion.necessiteReappro();
      }).length,
  );

  readonly okCount = computed(
    () =>
      this.suggestions().filter(s => {
        const suggestion = new SemoisSuggestion(
          s.produitId, s.libelle, s.codeCip, s.fournisseurId, s.fournisseurLibelle,
          s.classeCriticite, s.vmm, s.margeSecurite, s.stockObjectif, s.stockActuel,
          s.quantiteACommander, s.delaiLivraisonJours, s.coefficientSecurite,
          s.facteurSaisonnier, s.dateDernierCalcul,
        );
        return !suggestion.necessiteReappro();
      }).length,
  );

  ngOnInit(): void {
    this.loadSuggestions();
  }

  loadSuggestions(): void {
    this.isLoading.set(true);
    const classe = this.selectedClasse();
    const search = this.searchText();

    const req = {
      page: this.page(),
      size: this.itemsPerPage(),
    };

    this.semoisService.getSuggestions(req, search || undefined, classe ?? undefined).subscribe({
      next: (res: HttpResponse<ISemoisSuggestion[]>) => {
        this.suggestions.set(res.body ?? []);
        this.totalItems.set(Number(res.headers.get('X-Total-Count') ?? 0));
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
      },
    });
  }

  onPageChange(event: any): void {
    this.page.set(event.page);
    this.itemsPerPage.set(event.rows);
    this.loadSuggestions();
  }

  onFilterChange(): void {
    this.page.set(0);
    this.loadSuggestions();
  }

  onSearchChange(): void {
    this.page.set(0);
    this.loadSuggestions();
  }

  onClearFilters(): void {
    this.selectedClasse.set(null);
    this.urgenceFilter.set(null);
    this.searchText.set('');
    this.page.set(0);
    this.loadSuggestions();
  }

  toggleHelpDrawer(): void {
    this.helpDrawerVisible.set(!this.helpDrawerVisible());
  }

  openMassConfig(): void {
    this.router.navigate(['/semois/config-masse']);
  }

  navigateToDashboard(): void {
    this.commandCommonService.updateCommandPreviousActiveNav('SEMOIS_DASHBOARD');
  }

  // Méthodes utilitaires pour l'affichage
  getUrgenceLabel(suggestion: ISemoisSuggestion): string {
    const s = new SemoisSuggestion(
      suggestion.produitId, suggestion.libelle, suggestion.codeCip, suggestion.fournisseurId,
      suggestion.fournisseurLibelle, suggestion.classeCriticite, suggestion.vmm,
      suggestion.margeSecurite, suggestion.stockObjectif, suggestion.stockActuel,
      suggestion.quantiteACommander, suggestion.delaiLivraisonJours, suggestion.coefficientSecurite,
      suggestion.facteurSaisonnier, suggestion.dateDernierCalcul,
    );
    return s.getNiveauUrgence();
  }

  getUrgenceSeverity(suggestion: ISemoisSuggestion): 'danger' | 'warn' | 'success' {
    const urgence = this.getUrgenceLabel(suggestion);
    if (urgence === 'URGENT') return 'danger';
    if (urgence === 'NORMAL') return 'warn';
    return 'success';
  }

  getClasseLabel(classe?: ClasseCriticite): string {
    return getClasseCriticiteInfo(classe)?.label ?? '-';
  }

  getClasseSeverity(classe?: ClasseCriticite): 'danger' | 'success' | 'info' | 'warn' | 'secondary' {
    return getClasseCriticiteInfo(classe)?.severity ?? 'secondary';
  }

  getStockActuelClass(suggestion: ISemoisSuggestion): string {
    const s = new SemoisSuggestion(
      suggestion.produitId, suggestion.libelle, suggestion.codeCip, suggestion.fournisseurId,
      suggestion.fournisseurLibelle, suggestion.classeCriticite, suggestion.vmm,
      suggestion.margeSecurite, suggestion.stockObjectif, suggestion.stockActuel,
      suggestion.quantiteACommander, suggestion.delaiLivraisonJours, suggestion.coefficientSecurite,
      suggestion.facteurSaisonnier, suggestion.dateDernierCalcul,
    );
    if (s.estEnRupture()) return 'text-danger fw-bold';
    if (s.estEnSurstock()) return 'text-warning';
    return '';
  }

  getCouvertureMois(suggestion: ISemoisSuggestion): number {
    const s = new SemoisSuggestion(
      suggestion.produitId, suggestion.libelle, suggestion.codeCip, suggestion.fournisseurId,
      suggestion.fournisseurLibelle, suggestion.classeCriticite, suggestion.vmm,
      suggestion.margeSecurite, suggestion.stockObjectif, suggestion.stockActuel,
      suggestion.quantiteACommander, suggestion.delaiLivraisonJours, suggestion.coefficientSecurite,
      suggestion.facteurSaisonnier, suggestion.dateDernierCalcul,
    );
    return s.getTauxCouvertureMois();
  }

  getCouvertureClass(suggestion: ISemoisSuggestion): string {
    const couverture = this.getCouvertureMois(suggestion);
    if (couverture < 0.5) return 'bg-danger-subtle text-danger';
    if (couverture < 1.0) return 'bg-warning-subtle text-warning';
    if (couverture <= 2.0) return 'bg-success-subtle text-success';
    return 'bg-info-subtle text-info';
  }

  /** Couverture cible en mois = Stock Objectif / VMM */
  getCouvertureCibleMois(suggestion: ISemoisSuggestion): number {
    if (!suggestion.vmm || suggestion.vmm === 0) return 0;
    return (suggestion.stockObjectif ?? 0) / suggestion.vmm;
  }

  /** Classe CSS pour la couverture cible */
  getCouvertureCibleClass(suggestion: ISemoisSuggestion): string {
    const cible = this.getCouvertureCibleMois(suggestion);
    if (cible < 0.5) return 'text-danger';
    if (cible < 1.0) return 'text-warning';
    if (cible <= 3.0) return 'text-success';
    return 'text-info';
  }

  getJoursRestants(suggestion: ISemoisSuggestion): number {
    const s = new SemoisSuggestion(
      suggestion.produitId, suggestion.libelle, suggestion.codeCip, suggestion.fournisseurId,
      suggestion.fournisseurLibelle, suggestion.classeCriticite, suggestion.vmm,
      suggestion.margeSecurite, suggestion.stockObjectif, suggestion.stockActuel,
      suggestion.quantiteACommander, suggestion.delaiLivraisonJours, suggestion.coefficientSecurite,
      suggestion.facteurSaisonnier, suggestion.dateDernierCalcul,
    );
    return s.getJoursStockRestant();
  }

  getJoursRestantsClass(suggestion: ISemoisSuggestion): string {
    const jours = this.getJoursRestants(suggestion);
    if (jours <= 7) return 'bg-danger-subtle text-danger';
    if (jours <= 14) return 'bg-warning-subtle text-warning';
    if (jours <= 30) return 'bg-success-subtle text-success';
    return 'bg-info-subtle text-info';
  }

  getRowClass(suggestion: ISemoisSuggestion): string {
    const s = new SemoisSuggestion(
      suggestion.produitId, suggestion.libelle, suggestion.codeCip, suggestion.fournisseurId,
      suggestion.fournisseurLibelle, suggestion.classeCriticite, suggestion.vmm,
      suggestion.margeSecurite, suggestion.stockObjectif, suggestion.stockActuel,
      suggestion.quantiteACommander, suggestion.delaiLivraisonJours, suggestion.coefficientSecurite,
      suggestion.facteurSaisonnier, suggestion.dateDernierCalcul,
    );
    if (s.estEnRupture()) return 'table-danger';
    if (s.necessiteReappro()) return 'table-warning';
    return '';
  }
}

