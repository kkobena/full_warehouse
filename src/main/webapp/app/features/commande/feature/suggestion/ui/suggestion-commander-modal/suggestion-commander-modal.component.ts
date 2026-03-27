import { Component, ElementRef, inject, OnInit, Renderer2, signal } from "@angular/core";
import { CommonModule, DatePipe, DecimalPipe } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { NgbActiveModal } from "@ng-bootstrap/ng-bootstrap";
import { ButtonModule } from "primeng/button";
import { TagModule } from "primeng/tag";
import { SelectModule } from "primeng/select";
import { TextareaModule } from "primeng/textarea";
import { DatePicker } from "primeng/datepicker";
import { SuggestionLigneEnrichie } from "../../data-access/suggestion-enrichie.model";
import { TypeCommande } from "app/shared/model/pharmaml.model";
import { CommanderModalResult } from "../../data-access/suggestion-commander.model";
import { FournisseurService } from "app/entities/fournisseur/fournisseur.service";
import { IFournisseur } from "app/shared/model/fournisseur.model";

export { CommanderModalResult } from "../../data-access/suggestion-commander.model";

@Component({
  selector: "app-suggestion-commander-modal",
  templateUrl: "./suggestion-commander-modal.component.html",
  styleUrls: ["./suggestion-commander-modal.scss"],
  providers: [DatePipe],
  imports: [CommonModule, FormsModule, ButtonModule, TagModule, SelectModule, TextareaModule, DatePicker, DecimalPipe]
})
export class SuggestionCommanderModalComponent implements OnInit {
  private readonly activeModal = inject(NgbActiveModal);
  private readonly fournisseurService = inject(FournisseurService);
  private readonly datePipe = inject(DatePipe);
  private readonly renderer = inject(Renderer2);
  private readonly elementRef = inject(ElementRef);
  // ─── Propriétés injectées via componentInstance ──────────────────────────
  fournisseurLibelle: string = "";
  /** ID du fournisseur par défaut de la suggestion. */
  fournisseurId?: number;
  lignes: SuggestionLigneEnrichie[] = [];
  budgetRestant?: number;

  // ─── Signals ─────────────────────────────────────────────────────────────
  readonly modeCommande = signal<"INTERNE" | "PHARMAML">("INTERNE");
  readonly pharmamlTypeCommande = signal<TypeCommande>("NORMALE");
  readonly pharmamlCommentaire = signal("");
  /** Date stockée sous forme objet Date pour p-datePicker. */
  readonly pharmamlDateLivraison = signal<Date | null>(null);

  readonly fournisseurs = signal<IFournisseur[]>([]);
  readonly loadingFournisseurs = signal(false);
  /** Fournisseur sélectionné pour la commande (par défaut = fournisseur de la suggestion). */
  readonly selectedFournisseurId = signal<number | undefined>(undefined);

  readonly minDate = new Date();

  readonly typeCommandeOptions = [
    { label: "Normale", value: "NORMALE" as TypeCommande },
    { label: "Exceptionnelle", value: "EXCEPTIONNELLE" as TypeCommande }
  ];

  ngOnInit(): void {
    // Initialise le fournisseur sélectionné avec celui de la suggestion
    this.selectedFournisseurId.set(this.fournisseurId);
    this.loadFournisseurs();
  }

  get montantTotal(): number {
    return this.lignes.reduce((s, l) => s + l.quantite * l.prixAchat, 0);
  }

  get lignesModifiees(): SuggestionLigneEnrichie[] {
    return this.lignes.filter(l => l.quantiteModifiee);
  }

  get selectedFournisseurLibelle(): string {
    const id = this.selectedFournisseurId();
    if (!id) return this.fournisseurLibelle;
    return this.fournisseurs().find(f => f.id === id)?.libelle ?? this.fournisseurLibelle;
  }

  confirm(): void {
    const result: CommanderModalResult = {
      type: this.modeCommande(),
      fournisseurId: this.selectedFournisseurId(),
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

  protected onDropdownShow(event: any): void {
    const modalBody = this.elementRef.nativeElement.querySelector(".modal-body");
    if (modalBody) {
      this.renderer.addClass(modalBody, "overflow-visible");
    }
  }

  protected onDropdownHide(event: any): void {
    const modalBody = this.elementRef.nativeElement.querySelector(".modal-body");
    if (modalBody) {
      this.renderer.removeClass(modalBody, "overflow-visible");
    }
  }

  private loadFournisseurs(): void {
    this.loadingFournisseurs.set(true);
    this.fournisseurService.query({ size: 200, sort: ["libelle,asc"] }).subscribe({
      next: res => {
        this.fournisseurs.set(res.body ?? []);
        this.loadingFournisseurs.set(false);
      },
      error: () => this.loadingFournisseurs.set(false)
    });
  }
}
