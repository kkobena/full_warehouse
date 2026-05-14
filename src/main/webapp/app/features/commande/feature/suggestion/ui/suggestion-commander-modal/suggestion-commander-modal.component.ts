import { Component, ElementRef, inject, Renderer2, signal } from "@angular/core";
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
import { IFournisseur } from "app/shared/model/fournisseur.model";
import { FournisseurSelectComponent } from "../../../../../partners/ui/fournisseur-select/fournisseur-select.component";

export { CommanderModalResult } from "../../data-access/suggestion-commander.model";

@Component({
  selector: "app-suggestion-commander-modal",
  templateUrl: "./suggestion-commander-modal.component.html",
  styleUrls: ["./suggestion-commander-modal.scss"],
  providers: [DatePipe],
  imports: [CommonModule, FormsModule, ButtonModule, TagModule, SelectModule, TextareaModule, DatePicker, DecimalPipe, FournisseurSelectComponent]
})
export class SuggestionCommanderModalComponent {
  private readonly activeModal = inject(NgbActiveModal);
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
  readonly pharmamlDateLivraison = signal<Date | null>(null);
  readonly selectedFournisseur = signal<IFournisseur | null>(null);

  readonly minDate = new Date();

  readonly typeCommandeOptions = [
    { label: "Normale", value: "NORMALE" as TypeCommande },
    { label: "Exceptionnelle", value: "EXCEPTIONNELLE" as TypeCommande }
  ];

  get montantTotal(): number {
    return this.lignes.reduce((s, l) => s + l.quantite * l.prixAchat, 0);
  }

  get lignesModifiees(): SuggestionLigneEnrichie[] {
    return this.lignes.filter(l => l.quantiteModifiee);
  }

  get selectedFournisseurLibelle(): string {
    return this.selectedFournisseur()?.libelle ?? this.fournisseurLibelle;
  }

  protected onFournisseurSelected(f: IFournisseur | null): void {
    this.selectedFournisseur.set(f);
  }

  confirm(): void {
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
}
