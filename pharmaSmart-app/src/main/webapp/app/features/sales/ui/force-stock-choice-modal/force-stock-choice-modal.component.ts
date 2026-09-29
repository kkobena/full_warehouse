import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { MotifForcageStock } from '../../../../shared/model';
import { ButtonComponent } from '../../../../shared/ui';

/** Stock insuffisant : le caissier dit pourquoi il force. Ferme avec le motif, rejette sur « Annuler ». */
@Component({
  selector: 'app-force-stock-choice-modal',
  template: `
    <div class="modal-header">
      <h5 class="modal-title"><i class="pi pi-exclamation-triangle me-2"></i>Stock insuffisant</h5>
      <!-- Premier élément focalisable, donc focus initial : un Entrée résiduel annule au lieu de forcer. -->
      <button type="button" class="btn-close" aria-label="Fermer" (click)="annuler()"></button>
    </div>
    <div class="modal-body">
      <p class="mb-3">
        @if (produitLibelle) {
          <strong>{{ produitLibelle }}</strong> :
        }
        la quantité saisie
        @if (quantiteDemandee != null) {
          (<strong>{{ quantiteDemandee }}</strong>)
        }
        dépasse le stock affiché. Que se passe-t-il au comptoir ?
      </p>
      <div class="d-grid gap-3">
        @if (canRupture) {
          <div data-motif="RUPTURE_AVOIR">
            <app-button
              class="d-block"
              buttonClass="w-100 text-start"
              icon="pi pi-clock"
              label="Le client sera livré plus tard"
              severity="primary"
              [outlined]="true"
              (clicked)="choisir('RUPTURE_AVOIR')"
            />
            <small class="choice-hint">Le rayon est vide. Le manquant part en avoir : un client est obligatoire.</small>
          </div>
        }
        @if (canEcart) {
          <div data-motif="ECART_INVENTAIRE">
            <app-button
              class="d-block"
              buttonClass="w-100 text-start"
              icon="pi pi-box"
              label="Le produit est en rayon, la machine se trompe"
              severity="success"
              [outlined]="true"
              (clicked)="choisir('ECART_INVENTAIRE')"
            />
            <small class="choice-hint">Le client repart servi, sans avoir. Le stock est régularisé par un ajustement à l'encaissement.</small>
          </div>
        }
      </div>
    </div>
    <div class="modal-footer">
      <app-button data-action="annuler" icon="pi pi-times" label="Annuler" severity="secondary" [outlined]="true" (clicked)="annuler()" />
    </div>
  `,
  styleUrl: './force-stock-choice-modal.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ButtonComponent],
})
export class ForceStockChoiceModalComponent {
  // Renseignés via componentInstance avant le premier rendu.
  produitLibelle?: string;
  quantiteDemandee?: number;
  canRupture = false;
  canEcart = false;

  private readonly activeModal = inject(NgbActiveModal);

  choisir(motif: MotifForcageStock): void {
    this.activeModal.close(motif);
  }

  annuler(): void {
    this.activeModal.dismiss('annuler');
  }
}
