import { Component, computed, input, output, signal } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { TagModule } from 'primeng/tag';
import { ButtonModule } from 'primeng/button';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TooltipModule } from 'primeng/tooltip';
import { CheckboxModule } from 'primeng/checkbox';
import { FormsModule } from '@angular/forms';
import { FournisseurSuggestionSummary } from '../../data-access/suggestion-enrichie.model';

@Component({
  selector: 'app-suggestion-fournisseur-list',
  templateUrl: './suggestion-fournisseur-list.component.html',
  styleUrls: ['./suggestion-fournisseur-list.component.scss'],
  imports: [CommonModule, TagModule, ButtonModule, ProgressSpinnerModule, TooltipModule, CheckboxModule, FormsModule, DecimalPipe],
})
export class SuggestionFournisseurListComponent {
  fournisseurs = input<FournisseurSuggestionSummary[]>([]);
  selected = input<FournisseurSuggestionSummary | null>(null);
  loading = input(false);

  fournisseurSelected = output<FournisseurSuggestionSummary>();
  supprimerSuggestionRequest = output<number>();
  supprimerSelectionRequest = output<number[]>();
  fusionnerRequest = output<number[]>();
  validerRequest = output<FournisseurSuggestionSummary>();
  exportPdfRequest = output<FournisseurSuggestionSummary>();
  exportCsvRequest = output<FournisseurSuggestionSummary>();
  commanderRequest = output<FournisseurSuggestionSummary>();

  // Sélection multiple locale (pour bulk actions)
  readonly selectionMultiple = signal<FournisseurSuggestionSummary[]>([]);

  readonly selectionCount = computed(() => this.selectionMultiple().length);

  /** Fusion possible uniquement si ≥2 sélectionnées ET toutes du même fournisseur. */
  readonly canFusionner = computed(() => {
    const sel = this.selectionMultiple();
    if (sel.length < 2) return false;
    const fournisseurId = sel[0].fournisseurId;
    return sel.every(f => f.fournisseurId === fournisseurId);
  });

  select(f: FournisseurSuggestionSummary): void {
    this.fournisseurSelected.emit(f);
  }

  isSelected(f: FournisseurSuggestionSummary): boolean {
    return this.selectionMultiple().some(s => s.suggestionId === f.suggestionId && s.suggestionId != null);
  }

  toggleSelect(f: FournisseurSuggestionSummary, event: MouseEvent): void {
    event.stopPropagation();
    if (!f.suggestionId) return;
    this.selectionMultiple.update(sel => {
      const exists = sel.some(s => s.suggestionId === f.suggestionId);
      return exists ? sel.filter(s => s.suggestionId !== f.suggestionId) : [...sel, f];
    });
  }

  onSupprimerSuggestion(f: FournisseurSuggestionSummary, event: MouseEvent): void {
    event.stopPropagation();
    if (f.suggestionId) {
      this.supprimerSuggestionRequest.emit(f.suggestionId);
    }
  }

  onFusionner(): void {
    const ids = this.selectionMultiple()
      .map(f => f.suggestionId!)
      .filter(id => id != null);
    this.fusionnerRequest.emit(ids);
    this.selectionMultiple.set([]);
  }

  onSupprimerSelection(): void {
    const ids = this.selectionMultiple()
      .map(f => f.suggestionId!)
      .filter(id => id != null);
    this.supprimerSelectionRequest.emit(ids);
    this.selectionMultiple.set([]);
  }

  onValider(f: FournisseurSuggestionSummary, event: MouseEvent): void {
    event.stopPropagation();
    this.validerRequest.emit(f);
  }

  onExportPdf(f: FournisseurSuggestionSummary, event: MouseEvent): void {
    event.stopPropagation();
    this.exportPdfRequest.emit(f);
  }

  onExportCsv(f: FournisseurSuggestionSummary, event: MouseEvent): void {
    event.stopPropagation();
    this.exportCsvRequest.emit(f);
  }

  onCommander(f: FournisseurSuggestionSummary, event: MouseEvent): void {
    event.stopPropagation();
    this.commanderRequest.emit(f);
  }

  statutSeverity(statut: string | undefined): 'success' | 'warn' | 'danger' | 'secondary' | 'info' {
    switch (statut) {
      case 'VALIDEE': return 'success';
      case 'EN_ATTENTE_VALIDATION': return 'warn';
      case 'COMMANDEE': return 'info';
      default: return 'secondary';
    }
  }
}
