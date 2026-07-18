import { Component, ElementRef, inject, Renderer2, signal, ChangeDetectionStrategy } from "@angular/core";
import { CommonModule } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { NgbActiveModal } from "@ng-bootstrap/ng-bootstrap";
import { SelectModule } from "primeng/select";
import { InputTextModule } from "primeng/inputtext";
import { ButtonModule } from "primeng/button";
import { Card } from "primeng/card";
import { IBed, MotifBed, MOTIFS_BED_CREATION } from "../../data-access/bed.model";
import { IFournisseur } from "app/shared/model/fournisseur.model";
import { Textarea } from "primeng/textarea";
import { FournisseurSelectComponent } from "../../../../../partners/ui/fournisseur-select/fournisseur-select.component";

export interface BedValidateResult {
  motif: MotifBed;
  fournisseurId?: number;
  commentaire?: string;
}

@Component({
  selector: "app-bed-validate-modal",
  templateUrl: "./bed-validate-modal.component.html",
  styleUrls: ["./bed-validate-modal.component.scss"],
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [
    CommonModule,
    FormsModule,
    SelectModule,
    InputTextModule,
    ButtonModule,
    Card,
    Textarea,
    FournisseurSelectComponent
  ]
})
export class BedValidateModalComponent {
  readonly activeModal = inject(NgbActiveModal);
  private readonly renderer = inject(Renderer2);
  private readonly elementRef = inject(ElementRef);

  bed?: IBed;

  readonly formMotif = signal<MotifBed | null>(null);
  readonly formFournisseur = signal<IFournisseur | null>(null);
  protected commentaire = "";
  readonly motifOptions = MOTIFS_BED_CREATION;

  protected onFournisseurSelected(f: IFournisseur | null): void {
    this.formFournisseur.set(f);
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

  protected onConfirm(): void {
    if (!this.formMotif()) return;
    const result: BedValidateResult = {
      motif: this.formMotif()!,
      fournisseurId: this.formFournisseur()?.id,
      commentaire: this.commentaire || undefined
    };
    this.activeModal.close(result);
  }
}
