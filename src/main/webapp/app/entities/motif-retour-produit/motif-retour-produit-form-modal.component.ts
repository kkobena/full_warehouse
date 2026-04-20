import { Component, inject, OnInit, signal } from "@angular/core";
import { CommonModule } from "@angular/common";
import { FormsModule, ReactiveFormsModule } from "@angular/forms";
import { NgbActiveModal } from "@ng-bootstrap/ng-bootstrap";
import { ButtonModule } from "primeng/button";
import { InputText } from "primeng/inputtext";
import { IMotifRetourProduit, MotifRetourProduit } from "app/shared/model/motif-retour-produit.model";
import { ModifRetourProduitService } from "./motif-retour-produit.service";
import { Card } from "primeng/card";

@Component({
  selector: "jhi-motif-retour-produit-form-modal",
  imports: [CommonModule, FormsModule, ButtonModule, InputText, Card, ReactiveFormsModule],
  styleUrl: "./form-motif-retour-fourn.scss",
  template: `
    <div class="modal-header">
      <h4 class="modal-title">
        <i class="pi pi-tag me-2"></i>
        {{ isEditing() ? 'Modifier le motif de retour' : 'Nouveau motif de retour' }}
      </h4>
      <button type="button" class="btn-close" aria-label="Close" (click)="dismiss()" [disabled]="isSaving()"></button>
    </div>

    <div class="modal-body">
      <p-card>
        <div class="form-grid-compact">
          <div class="form-field form-field-full">
            <label for="libelle">Libellé<span class="required">*</span></label>
            <input
              required="true"
              autocomplete="off"
              class="w-full"
              pInputText
              id="libelle"
              [(ngModel)]="motif().libelle"
              [disabled]="isSaving()"
              maxlength="100"
            />
            <small class="text-muted">Maximum 100 caractères</small>
          </div>
        </div>
      </p-card>

      @if (errorMessage()) {
        <div class="alert alert-danger mt-3" role="alert">
          <i class="pi pi-exclamation-circle me-2"></i>
          {{ errorMessage() }}
        </div>
      }
    </div>

    <div class="modal-footer">
      <p-button (onClick)="dismiss()" [outlined]="true" [disabled]="isSaving()" icon="pi pi-times" label="Annuler"
                severity="secondary">
      </p-button>
      <p-button
        (onClick)="save()"
        type="submit"
        [loading]="isSaving()"
        [disabled]="!motif().libelle || motif().libelle.trim() === ''"
        icon="pi pi-check"
        label="Enregistrer"
        severity="primary"
      >
      </p-button>
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
