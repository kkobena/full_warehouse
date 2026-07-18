import {Component, inject, signal, ChangeDetectionStrategy} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {NgbActiveModal} from '@ng-bootstrap/ng-bootstrap';
import {Button} from 'primeng/button';
import {CommandeId} from '../../../../../../shared/model/abstract-commande.model';
import {PharmamlApiService} from '../../../../data-access/pharmaml-api.service';
import {NotificationService} from "../../../../../../shared/services/notification.service";
import {ErrorService} from "../../../../../../shared/error.service";

@Component({
  selector: 'app-annulation-pharmaml',
  imports: [FormsModule, Button],
  styleUrls: ['./annulation-pharmaml.scss'],
  changeDetection: ChangeDetectionStrategy.Eager,
  template: `
    <div class="modal-header">
      <h5 class="modal-title">Annuler la commande PharmaML</h5>
      <button type="button" class="btn-close" (click)="cancel()"></button>
    </div>
    <div class="modal-body">
      <p class="text-muted mb-3">
        Cette action envoie un message <strong>REQ_ANNULATION</strong> au grossiste. La commande sera archivée côté
        officine.
      </p>
      <div class="mb-3">
        <label class="form-label" for="motif">Motif (optionnel)</label>
        <textarea
          id="motif"
          class="form-control"
          rows="2"
          [(ngModel)]="motif"
          placeholder="Ex : Produits reçus d'un autre fournisseur"
        ></textarea>
      </div>
      @if (error()) {
        <div class="alert alert-danger py-2">{{ error() }}</div>
      }
    </div>
    <div class="modal-footer gap-2">
      <p-button label="Fermer" severity="secondary" [outlined]="true" (onClick)="cancel()"/>
      <p-button
        label="Confirmer l'annulation"
        severity="danger"
        icon="pi pi-ban"
        [loading]="loading()"
        (onClick)="confirmer()"
      />
    </div>
  `,
})
export class AnnulationPharmamlComponent {
  commandeId!: CommandeId;

  motif = '';
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  private readonly notificationService = inject(NotificationService);
  private readonly errorService = inject(ErrorService);
  private readonly activeModal = inject(NgbActiveModal);
  private readonly api = inject(PharmamlApiService);

  confirmer(): void {
    if (this.loading()) return;
    this.loading.set(true);
    this.error.set(null);
    this.api.annulation(this.commandeId.id, this.commandeId.orderDate, this.motif || undefined).subscribe({
      next: () => {
        this.loading.set(false);
        this.activeModal.close('annulee');
      },
      error: (err) => {
        this.loading.set(false);
        this.notificationService.error(this.errorService.getErrorMessage(err), 'Erreur');
      },
    });
  }

  cancel(): void {
    this.activeModal.dismiss();
  }
}
