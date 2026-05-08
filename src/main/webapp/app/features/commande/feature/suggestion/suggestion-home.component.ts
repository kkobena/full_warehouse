import {
  Component,
  DestroyRef,
  effect,
  inject,
  Injector,
  signal,
  untracked,
  viewChild
} from "@angular/core";
import {CommonModule, DecimalPipe} from "@angular/common";
import {FormsModule} from "@angular/forms";
import {NgbModal} from "@ng-bootstrap/ng-bootstrap";
import {ButtonModule} from "primeng/button";
import {TooltipModule} from "primeng/tooltip";
import {IconField} from "primeng/iconfield";
import {InputIcon} from "primeng/inputicon";
import {InputTextModule} from "primeng/inputtext";
import {MultiSelectModule} from "primeng/multiselect";
import {SuggestionFacadeService} from "./data-access/suggestion-facade.service";
import {
  SuggestionFournisseurListComponent
} from "./ui/suggestion-fournisseur-list/suggestion-fournisseur-list.component";
import {
  SuggestionProduitPanelComponent
} from "./ui/suggestion-produit-panel/suggestion-produit-panel.component";
import {
  SuggestionCommanderModalComponent
} from "./ui/suggestion-commander-modal/suggestion-commander-modal.component";
import {CommanderModalResult} from "./data-access/suggestion-commander.model";
import {
  SuggestionComparaisonComponent
} from "./ui/suggestion-comparaison/suggestion-comparaison.component";
import {
  DispoComparaisonComponent
} from "../pharmaml/ui/dispo-comparaison/dispo-comparaison.component";
import {
  FournisseurSuggestionSummary,
  SuggestionLigneEnrichie
} from "./data-access/suggestion-enrichie.model";
import {NotificationService} from "app/shared/services/notification.service";
import {
  NgbConfirmDialogService
} from "../../../../shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive";
import {Toast} from "primeng/toast";
import {FournisseurService} from "../../../../entities/fournisseur/fournisseur.service";
import {IFournisseur} from "../../../../shared/model/fournisseur.model";
import {takeUntilDestroyed, toObservable} from "@angular/core/rxjs-interop";
import {combineLatest, Subject} from "rxjs";
import {debounceTime, distinctUntilChanged, startWith} from "rxjs/operators";

@Component({
  selector: "app-suggestion-home",
  templateUrl: "./suggestion-home.component.html",
  styleUrls: ["./suggestion-home.component.scss"],
  imports: [
    CommonModule,
    FormsModule,
    SuggestionFournisseurListComponent,
    SuggestionProduitPanelComponent,
    ButtonModule,
    TooltipModule,
    DecimalPipe,
    Toast,
    IconField,
    InputIcon,
    InputTextModule,
    MultiSelectModule
  ]
})
export class SuggestionHomeComponent {
  readonly selectedLignes = signal<SuggestionLigneEnrichie[]>([]);
  readonly editingFournisseur = signal<FournisseurSuggestionSummary | null>(null);
  // ── List-level filters ─────────────────────────────────────────────────────
  readonly listSearch = signal("");
  readonly listFournisseurIds = signal<number[]>([]);
  readonly fournisseurOptions = signal<IFournisseur[]>([]);
  readonly childSelectionCount = signal<number>(0);
  readonly childCanFusionner = signal<boolean>(false);
  protected readonly facade = inject(SuggestionFacadeService);
  private readonly modalService = inject(NgbModal);
  private readonly injector = inject(Injector);
  private readonly notificationService = inject(NotificationService);
  private readonly confirmDialog = inject(NgbConfirmDialogService);
  private readonly fournisseurService = inject(FournisseurService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly listSearchSubject = new Subject<string>();
  private readonly listFournisseurIds$ = toObservable(this.listFournisseurIds);
  // ── Bulk actions (propagées depuis le composant enfant) ────────────────────
  private readonly sfList = viewChild(SuggestionFournisseurListComponent);

  constructor() {
    // Charge les options du multiselect une seule fois
    this.fournisseurService.query({page: 0, size: 999}).subscribe({
      next: res => this.fournisseurOptions.set(res.body ?? [])
    });

    // Recharge la liste à chaque changement de search (debounced) ou fournisseurs (init inclus via startWith)
    combineLatest([
      this.listSearchSubject.pipe(debounceTime(300), distinctUntilChanged(), startWith("")),
      this.listFournisseurIds$
    ]).pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(([search, ids]) => this.facade.loadAll(["GENEREE", "VALIDEE"], search, ids));

    // After loadAll(), selectedFournisseur is cleared. Re-select if still editing.
    effect(() => {
      const fournisseurs = this.facade.fournisseurs();
      const editing = this.editingFournisseur();
      if (editing && !this.facade.selectedFournisseur() && fournisseurs.length > 0) {
        const match = fournisseurs.find(f => f.suggestionId === editing.suggestionId);
        if (match) {
          untracked(() => this.facade.selectFournisseur(match));
          this.editingFournisseur.set(match);
        }
      }
    });
  }

  onListSearchChange(value: string): void {
    this.listSearch.set(value);
    this.listSearchSubject.next(value);
  }

  onListFournisseurChange(ids: number[]): void {
    this.listFournisseurIds.set(ids ?? []);
  }

  onFournisseurSelected(f: FournisseurSuggestionSummary): void {
    this.facade.selectFournisseur(f);
    this.editingFournisseur.set(f);
  }

  onRetour(): void {
    this.editingFournisseur.set(null);
  }

  /** Commander toute la suggestion, envoie uniquement l'ID au backend. */
  onCommander(): void {
    const fournisseur = this.facade.selectedFournisseur();
    if (!fournisseur) {
      this.notificationService.warning("Aucun fournisseur sélectionné.");
      return;
    }
    const lignes = this.facade.lignesEnrichies();
    this.openCommanderModal(fournisseur, lignes, "full");
  }

  /** Commander uniquement les lignes sélectionnées (la suggestion n'est pas supprimée si des lignes restent). */
  onCommanderSelection(): void {
    const fournisseur = this.facade.selectedFournisseur();
    if (!fournisseur) {
      this.notificationService.warning("Aucun fournisseur sélectionné.");
      return;
    }
    const lignes = this.selectedLignes();
    if (lignes.length === 0) {
      this.notificationService.warning("Aucune ligne sélectionnée.");
      return;
    }
    this.openCommanderModal(fournisseur, lignes, "selection");
  }

  /**
   * Commander les lignes visibles après filtrage urgence.
   * Déclenché par le bouton "Commander filtrés (N)" quand un filtre est actif.
   */
  onCommanderFiltre(lignes: SuggestionLigneEnrichie[]): void {
    const fournisseur = this.facade.selectedFournisseur();
    if (!fournisseur) {
      this.notificationService.warning("Aucun fournisseur sélectionné.");
      return;
    }
    if (lignes.length === 0) {
      this.notificationService.warning("Aucune ligne visible après filtrage.");
      return;
    }
    this.openCommanderModal(fournisseur, lignes, "selection");
  }

  onQuantiteChanged(data: { ligne: SuggestionLigneEnrichie; qte: number }): void {
    this.facade.updateQuantite(data.ligne, data.qte);
  }

  onResetQte(ligne: SuggestionLigneEnrichie): void {
    this.facade.resetQuantite(ligne);
  }


  onSelectionChanged(lignes: SuggestionLigneEnrichie[]): void {
    this.selectedLignes.set(lignes);
  }

  onFilterChange(event: { search: string; urgence: string }): void {
    this.facade.applyFilter(event.search, event.urgence);
  }

  onPageChange(event: { page: number; rows: number }): void {
    this.facade.changePage(event.page, event.rows);
  }

  onSanitize(): void {
    const fournisseur = this.facade.selectedFournisseur();
    if (!fournisseur?.suggestionId) {
      return;
    }
    this.facade.sanitize(fournisseur.suggestionId);
  }

  onExporterPdf(): void {
    this.facade.exporterPdf();
  }

  onExporterCsv(): void {
    this.facade.exporterCsv();
  }

  onComparer(ligne: SuggestionLigneEnrichie): void {
    if (!ligne.produitId) {
      return;
    }
    const modalRef = this.modalService.open(SuggestionComparaisonComponent, {
      size: "lg",
      scrollable: true,
      injector: this.injector
    });
    modalRef.componentInstance.produitId = ligne.produitId;
    modalRef.componentInstance.produitLibelle = ligne.libelle;
    modalRef.componentInstance.currentFournisseurProduitId = ligne.fournisseurProduitId;
  }

  onValider(): void {
    const fournisseur = this.facade.selectedFournisseur();
    if (!fournisseur?.suggestionId) {
      return;
    }

    this.confirmDialog.onConfirm(
      () => this.facade.valider(fournisseur.suggestionId),
      "Validation de la suggestion",
      "Valider cette suggestion ? Après validation, elle passera dans l'onglet \"Commandes à passer\" et ne pourra plus être modifiée par la suggestion automatique."
    );


  }

  onRejeter(): void {
    const fournisseur = this.facade.selectedFournisseur();
    if (!fournisseur?.suggestionId) {
      return;
    }
    this.confirmDialog.onConfirm(
      () => this.facade.rejeter(fournisseur.suggestionId!),
      "Rejeter la suggestion",
      "Cette action supprimera définitivement la suggestion. Confirmer ?"
    );
  }

  onLigneSupprimee(ligne: SuggestionLigneEnrichie): void {
    if (!ligne.id) {
      return;
    }
    this.confirmDialog.onConfirm(
      () => this.facade.supprimerLigne(ligne.id!),
      "Supprimer le produit",
      `Retirer "${ligne.libelle}" de cette suggestion ?`
    );
  }

  onLignesSupprimeees(lignes: SuggestionLigneEnrichie[]): void {
    const ids = lignes.map(l => l.id!).filter(id => id != null);
    if (ids.length === 0) {
      return;
    }
    this.confirmDialog.onConfirm(
      () => this.facade.supprimerLignes(ids),
      "Supprimer la sélection",
      `Retirer ${ids.length} produit(s) de cette suggestion ?`
    );
  }

  onSuggestionSupprimee(id: number): void {
    this.confirmDialog.onConfirm(
      () => {
        if (this.editingFournisseur()?.suggestionId === id) {
          this.editingFournisseur.set(null);
        }
        this.facade.supprimerSuggestions([id]);
      },
      "Supprimer la suggestion",
      "Cette action supprimera définitivement la suggestion et toutes ses lignes. Confirmer ?"
    );
  }

  onSuggestionsSupprimeees(ids: number[]): void {
    if (ids.length === 0) {
      return;
    }
    this.confirmDialog.onConfirm(
      () => {
        if (ids.includes(this.editingFournisseur()?.suggestionId ?? -1)) {
          this.editingFournisseur.set(null);
        }
        this.facade.supprimerSuggestions(ids);
      },
      "Supprimer les suggestions",
      `Supprimer définitivement ${ids.length} suggestion(s) ? Cette action est irréversible.`
    );
  }

  onAjouterProduit(data: {
    produitId: number;
    fournisseurProduitId: number;
    quantite: number
  }): void {
    this.facade.ajouterProduit(data.produitId, data.fournisseurProduitId, data.quantite);
  }

  onVerifierDispo(): void {
    const fournisseur = this.facade.selectedFournisseur();
    if (!fournisseur?.suggestionId) {
      this.notificationService.warning("Aucune suggestion sélectionnée.");
      return;
    }
    const modalRef = this.modalService.open(DispoComparaisonComponent, {
      size: "xl",
      scrollable: true,
      backdrop: 'static',
      centered: true,
      injector: this.injector
    });
    modalRef.componentInstance.suggestionId = fournisseur.suggestionId;
    modalRef.componentInstance.header = `Disponibilité PharmaML — ${fournisseur.libelle}`;
  }

  onChildSelectionCountChange(n: number): void {
    this.childSelectionCount.set(n);
  }

  onChildCanFusionnerChange(b: boolean): void {
    this.childCanFusionner.set(b);
  }

  protected onFusionnerBulk(): void {
    this.sfList()?.onFusionner();
  }

  protected onSupprimerBulk(): void {
    this.sfList()?.onSupprimerSelection();
  }

  protected onFusionnerSuggestions(ids: number[]): void {
    if (ids.length < 2) {
      return;
    }
    this.confirmDialog.onConfirm(
      () => this.facade.fusionnerSuggestions(ids),
      "Fusionner les suggestions",
      `Fusionner ${ids.length} suggestions en une seule ? Les lignes seront regroupées.`
    );
  }

  protected onRecalculer(): void {
    this.facade.recalculerSemois();
  }

  protected onValiderDirect(f: FournisseurSuggestionSummary): void {
    if (!f.suggestionId) {
      return;
    }
    this.confirmDialog.onConfirm(
      () => {
        this.editingFournisseur.set(f);
        this.facade.valider(f.suggestionId!);
      },
      "Valider la suggestion",
      `Valider la suggestion pour ${f.libelle} ? Elle passera en statut VALIDÉE.`
    );
  }

  protected onExportPdfDirect(f: FournisseurSuggestionSummary): void {
    if (!f.suggestionId) {
      return;
    }
    this.facade.selectFournisseur(f);
    this.facade.exporterPdf();
  }

  protected onExportCsvDirect(f: FournisseurSuggestionSummary): void {
    if (!f.suggestionId) {
      return;
    }
    this.facade.selectFournisseur(f);
    this.facade.exporterCsv();
  }

  protected onCommanderDirect(f: FournisseurSuggestionSummary): void {

    this.facade.selectFournisseur(f);
    this.facade.fetchLignes(f.suggestionId).subscribe(
      {
        next: lignes => {
          this.openCommanderModal(f, lignes, "full");
        },
        error: () => {
          this.notificationService.error("Erreur lors du chargement des lignes de la suggestion.");
        }
      }
    );

  }

  private openCommanderModal(
    fournisseur: FournisseurSuggestionSummary,
    lignes: SuggestionLigneEnrichie[],
    mode: "full" | "selection" = "selection"
  ): void {
    const modalRef = this.modalService.open(SuggestionCommanderModalComponent, {
      size: "xl",
      backdrop: "static"
    });
    modalRef.componentInstance.fournisseurLibelle = fournisseur.libelle;
    modalRef.componentInstance.fournisseurId = fournisseur.fournisseurId;
    modalRef.componentInstance.lignes = lignes;
    if (fournisseur.suggestionId) {
      const budget = this.facade.budget();
      if (budget && !budget.budgetIllimite) {
        modalRef.componentInstance.budgetRestant = budget.budgetRestant;
      }
    }

    modalRef.result.then(
      (result: CommanderModalResult) => {
        if (!result?.type) {
          return;
        }
        if (mode === "full") {
          this.facade.commanderFull(result);
        } else {
          this.facade.commanderSelection(lignes, result);
        }
      },
      () => {
        // dismissed — do nothing
      }
    );
  }
}
