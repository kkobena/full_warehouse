import {ChangeDetectionStrategy, Component, inject, signal} from "@angular/core";
import {CommonModule} from "@angular/common";
import {FormsModule} from "@angular/forms";
import {NgbActiveModal} from "@ng-bootstrap/ng-bootstrap";
import {ButtonComponent, CardComponent, SelectComponent} from "../../../../../../shared/ui";
import {IBed, MotifBed, MOTIFS_BED_CREATION} from "../../data-access/bed.model";
import {IFournisseur} from "app/shared/model/fournisseur.model";
import {
  FournisseurSelectComponent
} from "../../../../../partners/ui/fournisseur-select/fournisseur-select.component";

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
    ButtonComponent,
    CardComponent,
    SelectComponent,
    FournisseurSelectComponent
  ]
})
export class BedValidateModalComponent {
  readonly activeModal = inject(NgbActiveModal);
  bed?: IBed;

  readonly formMotif = signal<MotifBed | null>(null);
  readonly formFournisseur = signal<IFournisseur | null>(null);
  readonly motifOptions = MOTIFS_BED_CREATION;
  protected commentaire = "";

  protected onFournisseurSelected(f: IFournisseur | null): void {
    this.formFournisseur.set(f);
  }


  protected onConfirm(): void {
    if (!this.formMotif()) {
      return;
    }
    const result: BedValidateResult = {
      motif: this.formMotif()!,
      fournisseurId: this.formFournisseur()?.id,
      commentaire: this.commentaire || undefined
    };
    this.activeModal.close(result);
  }
}
