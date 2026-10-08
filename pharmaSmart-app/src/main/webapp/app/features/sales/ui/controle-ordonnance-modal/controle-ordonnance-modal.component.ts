import {ChangeDetectionStrategy, Component, computed, inject, signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {NgbActiveModal} from '@ng-bootstrap/ng-bootstrap';
import {AbilityService} from 'app/core/auth/ability.service';
import {CustomerService} from 'app/entities/customer/customer.service';
import {IAlerteControle, NiveauControle} from 'app/entities/customer/customer-fiche.model';
import {ButtonComponent, PasswordComponent} from '../../../../shared/ui';
import {DROIT_FORCER_ALERTE_SANTE} from '../alerte-sante-modal/alerte-sante-modal.component';

const LIBELLES_NIVEAU: Record<NiveauControle, string> = {
  CI: 'Contre-indication',
  AD: 'Association déconseillée',
  PE: 'Précaution d\'emploi',
  APEC: 'À prendre en compte'
};

/**
 * Alertes du contrôle d'ordonnance (interactions, redondances, contre-indications) levées par
 * l'ajout d'un produit. Aucune ne bloque seule : une contre-indication exige un motif et le droit de
 * passer outre (ou la clé d'un collègue), les autres un simple acquittement. La prise en compte est
 * tracée côté serveur. La modale ne s'ouvre que si un niveau paramétré bloquant est en cause.
 */
@Component({
  selector: 'app-controle-ordonnance-modal',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, ButtonComponent, PasswordComponent],
  template: `
    <div class="modal-header text-white" [class.bg-danger]="exigeMotif()" [class.bg-warning]="!exigeMotif()">
      <h5 class="modal-title"><i class="pi pi-exclamation-triangle me-2"></i>Contrôle de l'ordonnance</h5>
    </div>
    <div class="modal-body">
      <p class="mb-2"><strong>{{ produitLibelle }}</strong></p>
      @for (alerte of alertes; track alerte.message) {
        <div class="alert py-2" [class.alert-danger]="alerte.bloquant" [class.alert-warning]="!alerte.bloquant">
          <div><span class="badge bg-dark me-2">{{ libelle(alerte.niveau) }}</span>{{ alerte.message }}</div>
          @if (alerte.conduite) {
            <div class="small mt-1"><strong>Conduite à tenir :</strong> {{ alerte.conduite }}</div>
          }
          @if (alerte.source) {
            <div class="small text-muted mt-1">Source : {{ alerte.source }}</div>
          }
        </div>
      }
      <div class="small text-muted mb-3">{{ limite }} L'outil aide, il ne décide pas.</div>
      @if (exigeMotif()) {
        <label class="form-label" for="motifControle">Motif de la délivrance <span class="text-danger">*</span></label>
        <textarea [(ngModel)]="motif" class="form-control mb-3" id="motifControle" rows="2"
                  placeholder="Prescription maintenue par le médecin, associations surveillées…"></textarea>
        @if (!peutForcer) {
          <label class="form-label" for="cleControle">Clé de sécurité d'un pharmacien <span class="text-danger">*</span></label>
          <app-password [(ngModel)]="cle" autocomplete="off" inputId="cleControle" />
        }
      }
      @if (erreur()) {
        <div class="text-danger mt-2">{{ erreur() }}</div>
      }
    </div>
    <div class="modal-footer">
      <app-button (clicked)="annuler()" icon="pi pi-times" label="Ne pas ajouter" severity="secondary" />
      <app-button (clicked)="confirmer()" [disabled]="enCours()" icon="pi pi-check"
                  [label]="exigeMotif() ? 'Ajouter malgré l\\'alerte' : 'J\\'ai pris connaissance'"
                  [severity]="exigeMotif() ? 'danger' : 'primary'" />
    </div>
  `
})
export class ControleOrdonnanceModalComponent {
  customerId!: number;
  produitLibelle = '';
  /** Produits du panier, produit ajouté compris : le serveur recalcule les alertes sur cet ensemble. */
  produitIds: number[] = [];
  alertes: IAlerteControle[] = [];
  limite = '';

  protected motif = '';
  protected cle = '';
  protected readonly enCours = signal(false);
  protected readonly erreur = signal('');
  protected readonly peutForcer = inject(AbilityService).can('execute', DROIT_FORCER_ALERTE_SANTE);
  protected readonly exigeMotif = computed(() => this.alertes.some(a => a.bloquant));

  private readonly activeModal = inject(NgbActiveModal);
  private readonly customerService = inject(CustomerService);

  protected libelle(niveau: NiveauControle): string {
    return LIBELLES_NIVEAU[niveau];
  }

  protected annuler(): void {
    this.activeModal.close(false);
  }

  protected confirmer(): void {
    if (this.exigeMotif()) {
      if (!this.motif.trim()) {
        this.erreur.set('Le motif est obligatoire.');
        return;
      }
      if (!this.peutForcer && !this.cle) {
        this.erreur.set("La clé de sécurité d'un pharmacien est requise.");
        return;
      }
    }
    this.enCours.set(true);
    this.erreur.set('');
    this.customerService
      .prendreEnCompteControle(this.customerId, {
        produitIds: this.produitIds,
        motif: this.motif.trim() || undefined,
        actionAuthorityKey: this.exigeMotif() && !this.peutForcer ? this.cle : undefined
      })
      .subscribe({
        next: () => this.activeModal.close(true),
        error: err => {
          this.enCours.set(false);
          this.erreur.set(err?.error?.message ?? "La prise en compte n'a pas pu être enregistrée.");
        }
      });
  }
}
