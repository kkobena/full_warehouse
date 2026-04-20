import { Component, DestroyRef, ElementRef, inject, OnInit, Renderer2, signal } from "@angular/core";
import { CommonModule } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { NgbActiveModal } from "@ng-bootstrap/ng-bootstrap";
import { AutoCompleteModule } from "primeng/autocomplete";
import { SelectModule } from "primeng/select";
import { InputTextModule } from "primeng/inputtext";
import { ButtonModule } from "primeng/button";
import { Card } from "primeng/card";
import { IBed, MotifBed, MOTIFS_BED, MOTIFS_BED_CREATION } from "../../data-access/bed.model";
import { IFournisseur } from "app/shared/model/fournisseur.model";
import { FournisseurService } from "app/entities/fournisseur/fournisseur.service";
import { Textarea } from "primeng/textarea";

export interface BedValidateResult {
  motif: MotifBed;
  fournisseurId?: number;
  commentaire?: string;
}

@Component({
  selector: "app-bed-validate-modal",
  templateUrl: "./bed-validate-modal.component.html",
  styleUrls: ["./bed-validate-modal.component.scss"],
  imports: [
    CommonModule,
    FormsModule,
    SelectModule,
    AutoCompleteModule,
    InputTextModule,
    ButtonModule,
    Card,
    Textarea
  ]
})
export class BedValidateModalComponent implements OnInit {
  readonly activeModal = inject(NgbActiveModal);
  private readonly fournisseurService = inject(FournisseurService);
  private readonly destroyRef = inject(DestroyRef);

  bed?: IBed;

  readonly formMotif = signal<MotifBed | null>(null);
  readonly formFournisseur = signal<IFournisseur | null>(null);
  readonly fournisseurSuggestions = signal<IFournisseur[]>([]);
  protected commentaire = "";
  private readonly renderer = inject(Renderer2);
  private readonly elementRef = inject(ElementRef);
  readonly motifOptions = MOTIFS_BED_CREATION;

  ngOnInit(): void {
    this.onSearchFournisseur();
  }

  protected onSearchFournisseur(): void {
    this.fournisseurService
      .query({
        page: 0,
        size: 9999
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(res => this.fournisseurSuggestions.set(res.body ?? []));
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
