import { Component, inject, ChangeDetectionStrategy } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';

@Component({
  selector: 'app-retour-commentaire-modal',
  imports: [FormsModule, ButtonModule, InputTextModule],
  styleUrls: ['./retour-commentiare.component.scss'],
  changeDetection: ChangeDetectionStrategy.Eager,
  template: `
    <div class="modal-header">
      <h5 class="modal-title"><i class="pi pi-check-circle me-2"></i>Confirmer la création du retour</h5>
    </div>
    <div class="modal-body">
      <label class="form-label" for="commentaire">Commentaire (optionnel)</label>
      <input
        pInputText
        id="commentaire"
        [(ngModel)]="commentaire"
        placeholder="Commentaire…"
        class="w-100"
        (keydown.enter)="confirm()"
      />
    </div>
    <div class="modal-footer">
      <p-button (onClick)="dismiss()" icon="pi pi-times" label="Annuler" severity="secondary" [outlined]="true" />
      <p-button (onClick)="confirm()" icon="pi pi-check" label="Valider le retour" severity="primary" />
    </div>
  `,
})
export class RetourCommentaireModalComponent {
  protected commentaire = '';
  private readonly activeModal = inject(NgbActiveModal);

  protected confirm(): void {
    this.activeModal.close(this.commentaire);
  }

  protected dismiss(): void {
    this.activeModal.dismiss();
  }
}
