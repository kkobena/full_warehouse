import { Component, computed, DestroyRef, inject, OnInit, signal, ChangeDetectionStrategy } from "@angular/core";
import { takeUntilDestroyed, toObservable } from "@angular/core/rxjs-interop";
import { combineLatest, Subject } from "rxjs";
import { debounceTime, distinctUntilChanged, startWith } from "rxjs/operators";
import { FormsModule } from "@angular/forms";
import { NgbActiveModal } from "@ng-bootstrap/ng-bootstrap";
import { SuggestionService } from "../../../../entities/commande/suggestion/suggestion.service";
import { Suggestion } from "../../../../entities/commande/suggestion/model/suggestion.model";
import { SuggestionLine } from "../../../../entities/commande/suggestion/model/suggestion-line.model";
import { EtaProduitComponent } from "../../../../shared/eta-produit/eta-produit.component";
import { CommandeService } from "../../../../entities/commande/commande.service";
import { CommandeId } from "../../../../shared/model/abstract-commande.model";
import { NotificationService } from "../../../../shared/services/notification.service";
import { ErrorService } from "../../../../shared/error.service";
import { IFournisseur } from "../../../../shared/model/fournisseur.model";
import { ButtonModule } from "primeng/button";
import { TableModule } from "primeng/table";
import { TagModule } from "primeng/tag";
import { TooltipModule } from "primeng/tooltip";
import { CommonModule } from "@angular/common";
import { IconField } from "primeng/iconfield";
import { InputIcon } from "primeng/inputicon";
import { InputTextModule } from "primeng/inputtext";
import { FournisseurSelectComponent } from "../../../partners/ui/fournisseur-select/fournisseur-select.component";

@Component({
  selector: "app-import-suggestion-modal",
  templateUrl: "./import-suggestion-modal.component.html",
  styleUrls: ["./import-suggestion.scss"],
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [
    CommonModule,
    FormsModule,
    ButtonModule,
    TableModule,
    TagModule,
    TooltipModule,
    EtaProduitComponent,
    IconField,
    InputIcon,
    InputTextModule,
    FournisseurSelectComponent
  ]
})
export class ImportSuggestionModalComponent implements OnInit {
  commandeId!: CommandeId;
  fournisseurId: number | null | undefined = null;
  commandeFournisseurId: number | null | undefined = null;

  protected readonly searchTerm = signal("");
  protected readonly selectedFournisseurIds = signal<number[]>([]);
  protected readonly detailSearchTerms = signal<Record<number, string>>({});

  protected suggestions = signal<Suggestion[]>([]);
  protected loading = signal(false);
  protected importingId = signal<number | null>(null);
  protected selectedLines = signal<Record<number, SuggestionLine[]>>({});
  protected linesCache: Record<number, SuggestionLine[]> = {};
  protected loadingLines: Record<number, boolean> = {};

  // rebuilt only when selection changes
  private readonly _selectedLineIdSets = computed<Record<number, Set<number>>>(() => {
    const map: Record<number, Set<number>> = {};
    for (const [sId, lines] of Object.entries(this.selectedLines())) {
      map[+sId] = new Set(lines.map(l => l.id!));
    }
    return map;
  });

  private readonly searchSubject = new Subject<string>();
  private readonly destroyRef = inject(DestroyRef);
  private readonly activeModal = inject(NgbActiveModal);
  private readonly suggestionService = inject(SuggestionService);
  private readonly commandeService = inject(CommandeService);
  private readonly notificationService = inject(NotificationService);
  private readonly errorService = inject(ErrorService);
  private readonly fournisseurIds$ = toObservable(this.selectedFournisseurIds);

  ngOnInit(): void {
    combineLatest([
      this.searchSubject.pipe(debounceTime(300), distinctUntilChanged(), startWith("")),
      this.fournisseurIds$
    ]).pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(([search, ids]) => this.loadSuggestions(search, ids));
  }

  protected dismiss(): void {
    this.activeModal.dismiss();
  }

  protected onSearchChange(value: string): void {
    this.searchTerm.set(value);
    this.searchSubject.next(value);
  }

  protected onImport(suggestion: Suggestion): void {
    this.importingId.set(suggestion.id);
    this.commandeService.importSuggestionIntoCommande(this.commandeId, suggestion.id).subscribe({
      next: () => {
        this.notificationService.success("Suggestion importée avec succès", "Import");
        this.activeModal.close(true);
      },
      error: err => {
        this.notificationService.error(this.errorService.getErrorMessage(err), "Erreur");
        this.importingId.set(null);
      }
    });
  }

  protected onImportSelection(suggestion: Suggestion): void {
    const lineIds = (this.selectedLines()[suggestion.id] ?? []).map(l => l.id!).filter(Boolean);
    if (lineIds.length === 0) return;
    this.importingId.set(suggestion.id);
    this.commandeService.importSuggestionLinesIntoCommande(this.commandeId, suggestion.id, lineIds).subscribe({
      next: () => {
        this.notificationService.success(`${lineIds.length} ligne(s) importée(s) avec succès`, "Import");
        this.activeModal.close(true);
      },
      error: err => {
        this.notificationService.error(this.errorService.getErrorMessage(err), "Erreur");
        this.importingId.set(null);
      }
    });
  }

  protected onSelectionChange(suggestionId: number, lines: SuggestionLine[]): void {
    this.selectedLines.update(prev => ({ ...prev, [suggestionId]: lines }));
  }

  protected isLineSelected(suggestionId: number, lineId: number): boolean {
    return this._selectedLineIdSets()[suggestionId]?.has(lineId) ?? false;
  }

  protected selectedLineCount(suggestion: Suggestion): number {
    return this.selectedLines()[suggestion.id]?.length ?? 0;
  }

  protected onRowExpand(event: { data: Suggestion }): void {
    const id = event.data.id;
    if (this.linesCache[id]) return;
    this.loadingLines[id] = true;
    this.suggestionService.queryAllLines(id).subscribe({
      next: lines => {
        this.linesCache[id] = lines;
        this.loadingLines[id] = false;
      },
      error: () => {
        this.loadingLines[id] = false;
      }
    });
  }

  protected linesOf(suggestion: Suggestion): SuggestionLine[] {
    return this.linesCache[suggestion.id] ?? [];
  }

  protected filteredLinesOf(suggestion: Suggestion): SuggestionLine[] {
    const lines = this.linesCache[suggestion.id] ?? [];
    const term = (this.detailSearchTerms()[suggestion.id] ?? "").trim().toLowerCase();
    if (!term) return lines;
    return lines.filter(l =>
      l.fournisseurProduitCip?.toLowerCase().includes(term) ||
      l.fournisseurProduitLibelle?.toLowerCase().includes(term) ||
      l.fournisseurProduitCodeEan?.toLowerCase().includes(term)
    );
  }

  protected onFournisseursSelected(fournisseurs: IFournisseur[]): void {
    this.selectedFournisseurIds.set(fournisseurs.map(f => f.id!));
  }

  protected onDetailSearch(suggestionId: number, value: string): void {
    this.detailSearchTerms.update(prev => ({ ...prev, [suggestionId]: value }));
  }

  protected isLoadingLines(suggestion: Suggestion): boolean {
    return !!this.loadingLines[suggestion.id];
  }

  protected statutSeverity(statut: string): "success" | "info" | "warn" | "danger" | "secondary" {
    switch (statut) {
      case "ACTIVE":
        return "success";
      case "PENDING":
        return "info";
      case "CLOSED":
        return "secondary";
      default:
        return "info";
    }
  }

  protected isFournisseurMismatch(suggestion: Suggestion): boolean {
    return this.commandeFournisseurId != null && suggestion.fournisseurId !== this.commandeFournisseurId;
  }

  private loadSuggestions(search = "", fournisseurIds: number[] = []): void {
    this.loading.set(true);
    const params: any = { page: 0, size: 999, statut: ["GENEREE", "VALIDEE"] };
    if (search) params["search"] = search;
    if (fournisseurIds.length > 0) params["fournisseurIds"] = fournisseurIds;
    this.suggestionService.query(params).subscribe({
      next: res => {
        this.suggestions.set(res.body ?? []);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }
}
