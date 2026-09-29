import {ChangeDetectionStrategy, Component, inject, signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {NgbActiveModal} from '@ng-bootstrap/ng-bootstrap';
import {AbilityService} from 'app/core/auth/ability.service';
import {CustomerService} from 'app/entities/customer/customer.service';
import {IAlerteSante} from 'app/entities/customer/customer-fiche.model';
import {ButtonComponent, PasswordComponent} from '../../../../shared/ui';

/** Droit de délivrer malgré une alerte santé ; sans lui, la clé d'un collègue qui le détient. */
export const DROIT_FORCER_ALERTE_SANTE = 'pr-forcer-alerte-sante';

/**
 * Alerte santé bloquante à l'ajout d'un produit (fiche client, lot 2). Le produit n'est ajouté que
 * si la dérogation est enregistrée côté serveur, avec son motif — elle y est tracée.
 */
@Component({
  selector: 'app-alerte-sante-modal',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, ButtonComponent, PasswordComponent],
  template: `
    <div class="modal-header bg-danger text-white">
      <h5 class="modal-title"><i class="pi pi-exclamation-triangle me-2"></i>Alerte santé</h5>
    </div>
    <div class="modal-body">
      <p class="mb-2"><strong>{{ produitLibelle }}</strong></p>
      <div class="alert alert-danger">
        @for (alerte of alertes; track alerte.message) {
          <div>{{ alerte.message }}</div>
        }
      </div>
      @if (rappels.length) {
        <div class="alert alert-warning py-2">
          @for (rappel of rappels; track rappel.message) {
            <div>{{ rappel.message }}</div>
          }
        </div>
      }
      <label class="form-label" for="motifDerogation">Motif de la délivrance <span class="text-danger">*</span></label>
      <textarea [(ngModel)]="motif" class="form-control mb-3" id="motifDerogation" rows="2"
                placeholder="Prescription maintenue par le médecin, allergie infirmée…"></textarea>
      @if (!peutForcer) {
        <label class="form-label" for="cleDerogation">Clé de sécurité d'un pharmacien <span class="text-danger">*</span></label>
        <app-password [(ngModel)]="cle" autocomplete="off" inputId="cleDerogation" />
      }
      @if (erreur()) {
        <div class="text-danger mt-2">{{ erreur() }}</div>
      }
    </div>
    <div class="modal-footer">
      <app-button (clicked)="annuler()" icon="pi pi-times" label="Ne pas délivrer" severity="secondary" />
      <app-button (clicked)="deroger()" [disabled]="enCours()" icon="pi pi-check" label="Délivrer malgré l'alerte" severity="danger" />
    </div>
  `
})
export class AlerteSanteModalComponent {
  customerId!: number;
  produitId!: number;
  produitLibelle = '';
  alertes: IAlerteSante[] = [];
  /** Grossesse, allaitement : rappelés dans la modale plutôt qu'en toast par-dessus. */
  rappels: IAlerteSante[] = [];

  protected motif = '';
  protected cle = '';
  protected readonly enCours = signal(false);
  protected readonly erreur = signal('');
  protected readonly peutForcer = inject(AbilityService).can('execute', DROIT_FORCER_ALERTE_SANTE);

  private readonly activeModal = inject(NgbActiveModal);
  private readonly customerService = inject(CustomerService);

  protected annuler(): void {
    this.activeModal.close(false);
  }

  protected deroger(): void {
    if (!this.motif.trim()) {
      this.erreur.set('Le motif est obligatoire.');
      return;
    }
    if (!this.peutForcer && !this.cle) {
      this.erreur.set("La clé de sécurité d'un pharmacien est requise.");
      return;
    }
    this.enCours.set(true);
    this.erreur.set('');
    this.customerService
      .derogerAlerteSante(this.customerId, {
        produitId: this.produitId,
        motif: this.motif.trim(),
        actionAuthorityKey: this.peutForcer ? undefined : this.cle
      })
      .subscribe({
        next: () => this.activeModal.close(true),
        error: err => {
          this.enCours.set(false);
          this.erreur.set(err?.error?.message ?? "La dérogation n'a pas pu être enregistrée.");
        }
      });
  }
}
