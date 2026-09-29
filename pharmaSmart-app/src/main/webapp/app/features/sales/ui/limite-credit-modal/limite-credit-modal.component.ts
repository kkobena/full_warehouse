import {ChangeDetectionStrategy, Component, inject, signal} from '@angular/core';
import {DecimalPipe} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {NgbActiveModal} from '@ng-bootstrap/ng-bootstrap';
import {AbilityService} from 'app/core/auth/ability.service';
import {CustomerService} from 'app/entities/customer/customer.service';
import {ButtonComponent, PasswordComponent} from '../../../../shared/ui';
import {currencySymbol} from '../../../../shared/utils/format-utils';

/** Droit de vendre à crédit au-delà de la limite ; sans lui, la clé d'un collègue qui le détient. */
export const DROIT_DEPASSER_LIMITE_CREDIT = 'pr-depasser-limite-credit';

/** Ce que le serveur renvoie avec le refus `limiteCreditDepassee`. */
export interface DepassementLimiteCredit {
  customerId: number;
  saleId: number;
  saleDate: string;
  montant: number;
  encours: number;
  limite: number;
}

/**
 * Vente à crédit refusée pour dépassement de la limite (fiche client, lot 3). La vente n'est
 * finalisée que si la dérogation est enregistrée côté serveur, avec son motif.
 */
@Component({
  selector: 'app-limite-credit-modal',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, DecimalPipe, ButtonComponent, PasswordComponent],
  template: `
    <div class="modal-header bg-warning">
      <h5 class="modal-title"><i class="pi pi-credit-card me-2"></i>Limite de crédit dépassée</h5>
    </div>
    <div class="modal-body">
      <div class="alert alert-warning">
        <div>Encours actuel : <strong>{{ depassement.encours | number }} {{ devise }}</strong></div>
        <div>Cette vente à crédit : <strong>{{ depassement.montant | number }} {{ devise }}</strong></div>
        <div>Limite de l'officine : <strong>{{ depassement.limite | number }} {{ devise }}</strong></div>
      </div>
      <label class="form-label" for="motifLimiteCredit">Motif <span class="text-danger">*</span></label>
      <textarea [(ngModel)]="motif" class="form-control mb-3" id="motifLimiteCredit" rows="2"
                placeholder="Client régulier, règlement promis…"></textarea>
      @if (!peutForcer) {
        <label class="form-label" for="cleLimiteCredit">Clé de sécurité d'un pharmacien <span class="text-danger">*</span></label>
        <app-password [(ngModel)]="cle" autocomplete="off" inputId="cleLimiteCredit" />
      }
      @if (erreur()) {
        <div class="text-danger mt-2">{{ erreur() }}</div>
      }
    </div>
    <div class="modal-footer">
      <app-button (clicked)="annuler()" icon="pi pi-times" label="Revenir à la vente" severity="secondary" />
      <app-button (clicked)="deroger()" [disabled]="enCours()" icon="pi pi-check" label="Vendre à crédit quand même" severity="warn" />
    </div>
  `
})
export class LimiteCreditModalComponent {
  depassement!: DepassementLimiteCredit;

  protected motif = '';
  protected cle = '';
  protected readonly devise = currencySymbol();
  protected readonly enCours = signal(false);
  protected readonly erreur = signal('');
  protected readonly peutForcer = inject(AbilityService).can('execute', DROIT_DEPASSER_LIMITE_CREDIT);

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
      .derogerLimiteCredit(this.depassement.customerId, {
        saleId: this.depassement.saleId,
        saleDate: this.depassement.saleDate,
        montant: this.depassement.montant,
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
