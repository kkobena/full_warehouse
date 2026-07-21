import { Component, inject, OnInit, signal, ChangeDetectionStrategy } from "@angular/core";
import { CommonModule } from "@angular/common";
import { FormsModule, ReactiveFormsModule } from "@angular/forms";
import { NgbActiveModal } from "@ng-bootstrap/ng-bootstrap";
import { ButtonComponent } from "../../shared/ui";
import { IMotifRetourProduit, MotifRetourProduit } from "app/shared/model/motif-retour-produit.model";
import { ModifRetourProduitService } from "./motif-retour-produit.service";

@Component({
  selector: "app-motif-retour-produit-form-modal",
  imports: [CommonModule, FormsModule, ButtonComponent, ReactiveFormsModule],
  styleUrl: "./form-motif-retour-fourn.scss",
  changeDetection: ChangeDetectionStrategy.Eager,
  template: `
    <div class="modal-header">
      <h4 class="modal-title">
        <i class="pi pi-tag me-2"></i>
        {{ isEditing() ? 'Modifier le motif de retour' : 'Nouveau motif de retour' }}
      </h4>
      <button type="button" class="btn-close" aria-label="Close" (click)="dismiss()" [disabled]="isSaving()"></button>
    </div>

    <div class="modal-body">
      <div class="card data-card">
        <div class="card-body">
          <div class="form-grid-compact">
            <div class="form-field form-field-full">
              <label for="libelle">Libellé<span class="required">*</span></label>
              <input
                required
                autocomplete="off"
                class="form-control"
                id="libelle"
                [(ngModel)]="motif().libelle"
                [disabled]="isSaving()"
                maxlength="100"
              />
              <small class="text-muted">Maximum 100 caractères</small>
            </div>
          </div>
        </div>
      </div>

      @if (errorMessage()) {
        <div class="alert alert-danger mt-3" role="alert">
          <i class="pi pi-exclamation-circle me-2"></i>
          {{ errorMessage() }}
        </div>
      }
    </div>

    <div class="modal-footer">
      <app-button (clicked)="dismiss()" [outlined]="true" [disabled]="isSaving()" icon="pi pi-times" label="Annuler"
                  severity="secondary" />
      <app-button
        (clicked)="save()"
        type="submit"
        [loading]="isSaving()"
        [disabled]="!motif().libelle || motif().libelle.trim() === ''"
        icon="pi pi-check"
        label="Enregistrer"
        severity="primary"
      />
    </div>
  `
})
export class MotifRetourProduitFormModalComponent implements OnInit {
  private readonly activeModal = inject(NgbActiveModal);
  private readonly motifRetourService = inject(ModifRetourProduitService);

  motifToEdit?: IMotifRetourProduit;
  protected motif = signal<IMotifRetourProduit>(new MotifRetourProduit());
  protected isEditing = signal<boolean>(false);
  protected isSaving = signal<boolean>(false);
  protected errorMessage = signal<string>("");

  ngOnInit(): void {
    if (this.motifToEdit) {
      this.motif.set({ ...this.motifToEdit });
      this.isEditing.set(true);
    }
  }

  protected save(): void {
    const motifData = this.motif();

    if (!motifData.libelle || motifData.libelle.trim() === "") {
      this.errorMessage.set("Le libellé est obligatoire");
      return;
    }

    this.isSaving.set(true);
    this.errorMessage.set("");

    const request = this.isEditing() ? this.motifRetourService.update(motifData) : this.motifRetourService.create(motifData);

    request.subscribe({
      next: response => {
        this.activeModal.close(response.body);
      },
      error: () => {
        this.errorMessage.set("Erreur lors de l'enregistrement du motif de retour");
        this.isSaving.set(false);
      }
    });
  }

  protected dismiss(): void {
    this.activeModal.dismiss();
  }
}
