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

import { ISemoisSuggestion, SemoisSuggestion } from 'app/shared/model/semois/semois-suggestion.model';
import { ClasseCriticite, getClasseCriticiteInfo } from 'app/shared/model/semois/classe-criticite.model';
import { SemoisService } from './semois.service';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';

@Component({
  selector: 'jhi-semois-suggestions',
  templateUrl: './semois-suggestions.component.html',
  styleUrl: './semois-suggestions.component.scss',
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
  ],
})
export default class SemoisSuggestionsComponent implements OnInit {
  suggestions = signal<ISemoisSuggestion[]>([]);
  isLoading = signal<boolean>(false);
  selectedClasse = signal<ClasseCriticite | null>(null);
  urgenceFilter = signal<string | null>(null);
  searchText = signal<string>('');
  helpDrawerVisible = signal<boolean>(false);

  // Pagination
  page = signal<number>(0);
  itemsPerPage = signal<number>(20);
  totalItems = signal<number>(0);

  private readonly semoisService = inject(SemoisService);
  private readonly router = inject(Router);

  // Compteurs calculés
  urgentCount = computed(() => this.suggestions().filter(s => new SemoisSuggestion(s.produitId, s.libelle, s.codeCip, s.classeCriticite, s.vmm, s.margeSecurite, s.stockObjectif, s.stockActuel, s.quantiteACommander, s.delaiLivraisonJours, s.coefficientSecurite, s.facteurSaisonnier, s.dateDernierCalcul).estEnRupture()).length);

  normalCount = computed(() =>
    this.suggestions().filter(s => {
      const suggestion = new SemoisSuggestion(s.produitId, s.libelle, s.codeCip, s.classeCriticite, s.vmm, s.margeSecurite, s.stockObjectif, s.stockActuel, s.quantiteACommander, s.delaiLivraisonJours, s.coefficientSecurite, s.facteurSaisonnier, s.dateDernierCalcul);
      return !suggestion.estEnRupture() && suggestion.necessiteReappro();
    }).length
  );

  okCount = computed(() =>
    this.suggestions().filter(s => {
      const suggestion = new SemoisSuggestion(s.produitId, s.libelle, s.codeCip, s.classeCriticite, s.vmm, s.margeSecurite, s.stockObjectif, s.stockActuel, s.quantiteACommander, s.delaiLivraisonJours, s.coefficientSecurite, s.facteurSaisonnier, s.dateDernierCalcul);
      return !suggestion.necessiteReappro();
    }).length
  );

  // Note: Le filtrage est maintenant fait côté serveur via la pagination

  ngOnInit(): void {
    this.loadSuggestions();
  }

  loadSuggestions(): void {
    this.isLoading.set(true);
    const classe = this.selectedClasse();
    const search = this.searchText();

    const req = {
      page: this.page(),
      size: this.itemsPerPage()
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
    this.page.set(0); // Réinitialiser à la première page
    this.loadSuggestions();
  }

  onSearchChange(): void {
    this.page.set(0); // Réinitialiser à la première page lors d'une recherche
    this.loadSuggestions();
  }

  onClearFilters(): void {
    this.selectedClasse.set(null);
    this.urgenceFilter.set(null);
    this.searchText.set('');
    this.page.set(0); // Réinitialiser la pagination
    this.loadSuggestions();
  }

  toggleHelpDrawer(): void {
    this.helpDrawerVisible.set(!this.helpDrawerVisible());
  }

  openMassConfig(): void {
    this.router.navigate(['/semois/config-masse']);
  }

  // Méthodes utilitaires pour l'affichage
  getUrgenceLabel(suggestion: ISemoisSuggestion): string {
    const s = new SemoisSuggestion(suggestion.produitId, suggestion.libelle, suggestion.codeCip, suggestion.classeCriticite, suggestion.vmm, suggestion.margeSecurite, suggestion.stockObjectif, suggestion.stockActuel, suggestion.quantiteACommander, suggestion.delaiLivraisonJours, suggestion.coefficientSecurite, suggestion.facteurSaisonnier, suggestion.dateDernierCalcul);
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
    const s = new SemoisSuggestion(suggestion.produitId, suggestion.libelle, suggestion.codeCip, suggestion.classeCriticite, suggestion.vmm, suggestion.margeSecurite, suggestion.stockObjectif, suggestion.stockActuel, suggestion.quantiteACommander, suggestion.delaiLivraisonJours, suggestion.coefficientSecurite, suggestion.facteurSaisonnier, suggestion.dateDernierCalcul);
    if (s.estEnRupture()) return 'text-danger fw-bold';
    if (s.estEnSurstock()) return 'text-warning';
    return '';
  }

  getCouvertureMois(suggestion: ISemoisSuggestion): number {
    const s = new SemoisSuggestion(suggestion.produitId, suggestion.libelle, suggestion.codeCip, suggestion.classeCriticite, suggestion.vmm, suggestion.margeSecurite, suggestion.stockObjectif, suggestion.stockActuel, suggestion.quantiteACommander, suggestion.delaiLivraisonJours, suggestion.coefficientSecurite, suggestion.facteurSaisonnier, suggestion.dateDernierCalcul);
    return s.getTauxCouvertureMois();
  }

  getCouvertureClass(suggestion: ISemoisSuggestion): string {
    const couverture = this.getCouvertureMois(suggestion);
    if (couverture < 0.5) return 'bg-danger-subtle text-danger';
    if (couverture < 1.0) return 'bg-warning-subtle text-warning';
    if (couverture <= 2.0) return 'bg-success-subtle text-success';
    return 'bg-info-subtle text-info';
  }

  getJoursRestants(suggestion: ISemoisSuggestion): number {
    const s = new SemoisSuggestion(suggestion.produitId, suggestion.libelle, suggestion.codeCip, suggestion.classeCriticite, suggestion.vmm, suggestion.margeSecurite, suggestion.stockObjectif, suggestion.stockActuel, suggestion.quantiteACommander, suggestion.delaiLivraisonJours, suggestion.coefficientSecurite, suggestion.facteurSaisonnier, suggestion.dateDernierCalcul);
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
    const s = new SemoisSuggestion(suggestion.produitId, suggestion.libelle, suggestion.codeCip, suggestion.classeCriticite, suggestion.vmm, suggestion.margeSecurite, suggestion.stockObjectif, suggestion.stockActuel, suggestion.quantiteACommander, suggestion.delaiLivraisonJours, suggestion.coefficientSecurite, suggestion.facteurSaisonnier, suggestion.dateDernierCalcul);
    if (s.estEnRupture()) return 'table-danger';
    if (s.necessiteReappro()) return 'table-warning';
    return '';
  }
}
