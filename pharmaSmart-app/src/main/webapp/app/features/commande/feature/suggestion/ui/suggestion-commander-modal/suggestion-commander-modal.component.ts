import { Component, computed, inject, OnInit, signal } from "@angular/core";
import { CommonModule, DatePipe, DecimalPipe } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { NgbActiveModal } from "@ng-bootstrap/ng-bootstrap";
import { ButtonModule } from "primeng/button";
import { TagModule } from "primeng/tag";
import { SelectModule } from "primeng/select";
import { TextareaModule } from "primeng/textarea";
import { DatePicker } from "primeng/datepicker";
import { TableModule } from "primeng/table";
import { SuggestionLigneEnrichie } from "../../data-access/suggestion-enrichie.model";
import { TypeCommande } from "app/shared/model/pharmaml.model";
import { CommanderModalResult } from "../../data-access/suggestion-commander.model";
import { IFournisseur } from "app/shared/model/fournisseur.model";
import { FournisseurSelectComponent } from "../../../../../partners/ui/fournisseur-select/fournisseur-select.component";
import { FournisseurApiService } from "../../../../../partners/data-access/services/fournisseur-api.service";

export { CommanderModalResult } from "../../data-access/suggestion-commander.model";

@Component({
  selector: "app-suggestion-commander-modal",
  templateUrl: "./suggestion-commander-modal.component.html",
  styleUrls: ["./suggestion-commander-modal.scss"],
  providers: [DatePipe],
  imports: [CommonModule, FormsModule, ButtonModule, TagModule, SelectModule, TextareaModule, DatePicker, DecimalPipe, FournisseurSelectComponent, TableModule]
})
export class SuggestionCommanderModalComponent implements OnInit {
  private readonly activeModal = inject(NgbActiveModal);
  private readonly datePipe = inject(DatePipe);
  private readonly fournisseurApi = inject(FournisseurApiService);

  // ─── Propriétés injectées via componentInstance ──────────────────────────
  fournisseurLibelle: string = "";
  /** ID du fournisseur principal de la suggestion. */
  fournisseurId?: number;
  lignes: SuggestionLigneEnrichie[] = [];
  budgetRestant?: number;

  // ─── Signals ─────────────────────────────────────────────────────────────
  readonly modeCommande = signal<"INTERNE" | "PHARMAML">("INTERNE");
  readonly pharmamlTypeCommande = signal<TypeCommande>("NORMALE");
  readonly pharmamlCommentaire = signal("");
  readonly pharmamlDateLivraison = signal<Date | null>(null);
  readonly selectedFournisseur = signal<IFournisseur | null>(null);
  /** Agences du fournisseur principal (vide si fournisseur sans agences). */
  readonly agences = signal<IFournisseur[]>([]);

  /** true si le principal possède des agences → sélection obligatoire. */
  readonly hasAgences = computed(() => this.agences().length > 0);
  /** Commande possible uniquement si pas d'agences OU un fournisseur/agence est sélectionné. */
  readonly canConfirm = computed(() => !this.hasAgences() || this.selectedFournisseur() != null);

  readonly minDate = new Date();

  readonly typeCommandeOptions = [
    { label: "Normale", value: "NORMALE" as TypeCommande },
    { label: "Exceptionnelle", value: "EXCEPTIONNELLE" as TypeCommande }
  ];

  ngOnInit(): void {
    if (this.fournisseurId != null) {
      this.fournisseurApi.findAgences(this.fournisseurId).subscribe({
        next: list => this.agences.set(list),
        error: () => {}
      });
    }
  }

  get montantTotal(): number {
    return this.lignes.reduce((s, l) => s + l.quantite * l.prixAchat, 0);
  }

  get lignesModifiees(): SuggestionLigneEnrichie[] {
    return this.lignes.filter(l => l.quantiteModifiee);
  }

  get selectedFournisseurLibelle(): string {
    return this.selectedFournisseur()?.libelle ?? this.fournisseurLibelle;
  }

  /** Vrai si le fournisseur sélectionné (mode sans-agence) est différent du principal de la suggestion. */
  get isFournisseurDifferent(): boolean {
    const sf = this.selectedFournisseur();
    if (!sf) return false;
    return sf.id !== this.fournisseurId && sf.parentId !== this.fournisseurId;
  }

  protected onFournisseurSelected(f: IFournisseur | null): void {
    this.selectedFournisseur.set(f);
  }

  confirm(): void {
    if (!this.canConfirm()) return;
    const result: CommanderModalResult = {
      type: this.modeCommande(),
      fournisseurId: this.selectedFournisseur()?.id ?? this.fournisseurId,
      fournisseurLibelle: this.selectedFournisseurLibelle
    };
    if (this.modeCommande() === "PHARMAML") {
      const date = this.pharmamlDateLivraison();
      result.pharmamlParams = {
        typeCommande: this.pharmamlTypeCommande(),
        commentaire: this.pharmamlCommentaire() || undefined,
        dateLivraisonSouhaitee: date ? (this.datePipe.transform(date, "yyyy-MM-dd") ?? undefined) : undefined
      };
    }
    this.activeModal.close(result);
  }

  cancel(): void {
    this.activeModal.dismiss();
  }

}
