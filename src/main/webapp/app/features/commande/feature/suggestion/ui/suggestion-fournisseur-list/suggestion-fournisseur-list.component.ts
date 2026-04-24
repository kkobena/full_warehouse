import { Component, computed, input, output, signal } from "@angular/core";
import { CommonModule, DecimalPipe } from "@angular/common";
import { FournisseurSuggestionSummary } from "../../data-access/suggestion-enrichie.model";
import { TableModule } from "primeng/table";
import { SuggestionFournisseurAction, SuggestionFournisseurActionsComponent } from "./suggestion-fournisseur-actions.component";

@Component({
  selector: "app-suggestion-fournisseur-list",
  templateUrl: "./suggestion-fournisseur-list.component.html",
  styleUrls: ["./suggestion-fournisseur-list.component.scss"],
  imports: [CommonModule, DecimalPipe, TableModule, SuggestionFournisseurActionsComponent],
})
export class SuggestionFournisseurListComponent {
  // ── Inputs / Outputs ───────────────────────────────────────────────────────
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
  selectionCountChange = output<number>();
  canFusionnerChange = output<boolean>();

  // ── Sélection multiple (native PrimeNG) ───────────────────────────────────
  readonly selectionMultiple = signal<FournisseurSuggestionSummary[]>([]);
  readonly selectionCount = computed(() => this.selectionMultiple().length);
  readonly canFusionner = computed(() => {
    const sel = this.selectionMultiple();
    if (sel.length < 2) return false;
    const fId = sel[0].fournisseurId;
    return sel.every(f => f.fournisseurId === fId);
  });

  // ── Totaux ─────────────────────────────────────────────────────────────────
  readonly totalMontant = computed(() =>
    this.fournisseurs().reduce((s, f) => s + f.montantEstime, 0),
  );
  readonly totalUrgents = computed(() =>
    this.fournisseurs().reduce((s, f) => s + f.nbUrgents, 0),
  );

  // ── Row styling ────────────────────────────────────────────────────────────
  private static readonly STATUT_STYLES: Record<string, { [k: string]: string }> = {
    VALIDEE: { background: "#d1fae5", color: "#065f46" },
    EN_ATTENTE_VALIDATION: { background: "#fef3c7", color: "#92400e" },
    COMMANDEE: { background: "#dbeafe", color: "#1e40af" },
  };

  private static readonly STATUT_LABELS: Record<string, string> = {
    VALIDEE: "Validée",
    EN_ATTENTE_VALIDATION: "En attente",
    COMMANDEE: "Commandée",
  };

  getRowClass(f: FournisseurSuggestionSummary): Record<string, boolean> {
    const isActive = this.selected()?.suggestionId === f.suggestionId;
    return {
      "row-active": isActive,
      "row-urgent": f.nbUrgents > 0 && !isActive,
    };
  }

  getStatutStyle(statut: string | null | undefined): { [key: string]: string } {
    if (!statut) return {};
    return (
      SuggestionFournisseurListComponent.STATUT_STYLES[statut] ?? {
        background: "#f3f4f6",
        color: "#4b5563",
      }
    );
  }

  getStatutLabel(statut: string | null | undefined): string {
    return statut ? (SuggestionFournisseurListComponent.STATUT_LABELS[statut] ?? statut) : "";
  }

  onRowClick(f: FournisseurSuggestionSummary): void {
    this.fournisseurSelected.emit(f);
  }

  onSelectionChange(items: FournisseurSuggestionSummary[]): void {
    this.selectionMultiple.set(items);
    this.selectionCountChange.emit(items.length);
    this.canFusionnerChange.emit(this.canFusionner());
  }

  // ── Méthodes appelées par SuggestionFournisseurActionsComponent ────────────

  onSuggestionMenuAction(action: SuggestionFournisseurAction, f: FournisseurSuggestionSummary): void {
    switch (action) {
      case 'valider':    this.onValider(f); break;
      case 'commander':  this.onCommander(f); break;
      case 'exportPdf':  this.onExportPdf(f); break;
      case 'exportCsv':  this.onExportCsv(f); break;
      case 'supprimer':  this.onSupprimer(f); break;
    }
  }

  onValider(f: FournisseurSuggestionSummary): void {
    this.validerRequest.emit(f);
  }

  onCommander(f: FournisseurSuggestionSummary): void {
    this.commanderRequest.emit(f);
  }

  onExportPdf(f: FournisseurSuggestionSummary): void {
    this.exportPdfRequest.emit(f);
  }

  onExportCsv(f: FournisseurSuggestionSummary): void {
    this.exportCsvRequest.emit(f);
  }

  onSupprimer(f: FournisseurSuggestionSummary): void {
    if (f.suggestionId) this.supprimerSuggestionRequest.emit(f.suggestionId);
  }

  // ── Bulk actions ───────────────────────────────────────────────────────────

  onFusionner(): void {
    const ids = this.selectionMultiple()
      .map(f => f.suggestionId!)
      .filter(id => id != null);
    this.fusionnerRequest.emit(ids);
    this.selectionMultiple.set([]);
    this.selectionCountChange.emit(0);
    this.canFusionnerChange.emit(false);
  }

  onSupprimerSelection(): void {
    const ids = this.selectionMultiple()
      .map(f => f.suggestionId!)
      .filter(id => id != null);
    this.supprimerSelectionRequest.emit(ids);
    this.selectionMultiple.set([]);
    this.selectionCountChange.emit(0);
    this.canFusionnerChange.emit(false);
  }
}
